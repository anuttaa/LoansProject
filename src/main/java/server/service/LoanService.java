package server.service;

import com.google.gson.JsonObject;
import com.mysql.cj.exceptions.DataConversionException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.DAO.*;
import server.DTO.LoanDTO;
import server.DTO.PaymentScheduleDTO;
import server.Entities.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
public class LoanService {
    private final LoanDAO loanRepository = new LoanDAO();
    private final LoanTypeDAO loanTypeRepository = new LoanTypeDAO();
    private final UserDAO userRepository = new UserDAO();
    private final BankDAO bankRepository = new BankDAO();
    private final PaymentDAO paymentRepository = new PaymentDAO();
    private static final Logger LOG = LoggerFactory.getLogger(LoanService.class);

    @Transactional
    public LoanDTO createLoan(LoanDTO loanDTO) {
        User client = userRepository.findById(loanDTO.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));

        LoanType loanType = loanTypeRepository.findByIdWithAllRelations(loanDTO.getLoanTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Тип кредита не найден"));

        Loan loan = new Loan();
        loan.setClient(client);
        loan.setLoanType(loanType);
        loan.setLoanAmount(loanDTO.getLoanAmount());
        loan.setTermMonths(loanDTO.getTermMonths());
        loan.setStartDate(LocalDate.now());
        loan.setEndDate(calculateEndDate(loan.getStartDate(), loan.getTermMonths()));
        loan.setStatus("PENDING");

        Loan savedLoan = loanRepository.save(loan);

        Loan fullLoan = loanRepository.findByIdWithAllRelations(savedLoan.getLoanId())
                .orElseThrow(() -> new EntityNotFoundException("Кредит не найден после сохранения"));

        return convertToDTO(fullLoan);
    }

    @Transactional
    public JsonObject createLoanAndReturnJson(LoanDTO loanDTO) {
        LoanDTO createdLoan = createLoan(loanDTO);

        JsonObject loanJson = new JsonObject();
        loanJson.addProperty("id", createdLoan.getLoanId());
        loanJson.addProperty("amount", createdLoan.getLoanAmount().toString());
        loanJson.addProperty("termMonths", createdLoan.getTermMonths());
        loanJson.addProperty("status", createdLoan.getStatus());
        loanJson.addProperty("startDate", createdLoan.getStartDate().toString());

        if (createdLoan.getEndDate() != null) {
            loanJson.addProperty("endDate", createdLoan.getEndDate().toString());
        }

        if (createdLoan.getClientId() != null) {
            loanJson.addProperty("clientId", createdLoan.getClientId());
        }

        JsonObject typeJson = new JsonObject();
        typeJson.addProperty("id", createdLoan.getLoanTypeId());
        typeJson.addProperty("name", createdLoan.getLoanTypeName());
        typeJson.addProperty("rate", createdLoan.getInterestRate().toString());

        if (createdLoan.getBankId() != null) {
            typeJson.addProperty("bankId", createdLoan.getBankId());
        }

        loanJson.add("loanType", typeJson);

        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("loan", loanJson);

        return response;
    }

    public Loan assignLoanToClient(Long loanId, Long clientId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        loan.setClient(client);
        return loanRepository.save(loan);
    }

    public Loan getLoanById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит с id " + loanId + " не найден"));
    }

    public LoanDTO updateLoan(Long loanId, LoanDTO loanDTO) {
        if (loanId == null) {
            throw new IllegalArgumentException("ID кредита не может быть null");
        }
        if (loanDTO == null) {
            throw new IllegalArgumentException("DTO кредита не может быть null");
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит с ID " + loanId + " не найден"));

        if (loanDTO.getLoanTypeId() == null) {
            throw new IllegalArgumentException("ID типа кредита обязательно");
        }
        LoanType loanType = loanTypeRepository.findById(loanDTO.getLoanTypeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Тип кредита с ID " + loanDTO.getLoanTypeId() + " не найден"));

        if (loanDTO.getLoanAmount() == null || loanDTO.getLoanAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма кредита должна быть положительной");
        }
        if (loanDTO.getTermMonths() == null || loanDTO.getTermMonths() <= 0) {
            throw new IllegalArgumentException("Срок кредита должен быть положительным");
        }
        if (loanDTO.getStartDate() == null) {
            throw new IllegalArgumentException("Дата начала обязательна");
        }

        loan.setLoanType(loanType);
        loan.setLoanAmount(loanDTO.getLoanAmount());
        loan.setTermMonths(loanDTO.getTermMonths());
        loan.setStartDate(loanDTO.getStartDate());

        loan.setEndDate(calculateEndDate(loanDTO.getStartDate(), loanDTO.getTermMonths()));

        loan.setStatus(loanDTO.getStatus() != null ?
                validateLoanStatus(loanDTO.getStatus()) :
                loan.getStatus());

        Loan updatedLoan = loanRepository.save(loan);

        LOG.info("Кредит с ID {} успешно обновлен", loanId);

        return convertToDTO(updatedLoan);
    }

    private String validateLoanStatus(String status) {
        List<String> validStatuses = Arrays.asList("PENDING", "ACTIVE", "CLOSED");
        if (!validStatuses.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("Недопустимый статус кредита: " + status);
        }
        return status.toUpperCase();
    }

    private LocalDate calculateEndDate(LocalDate startDate, Integer termMonths) {
        return startDate.plusMonths(termMonths);
    }

    private LoanDTO convertToDTO(Loan loan) {
        LoanDTO dto = new LoanDTO();
        dto.setLoanId(loan.getLoanId());

        if (loan.getLoanType() != null) {
            dto.setLoanTypeId(loan.getLoanType().getLoanTypeId());
            dto.setLoanTypeName(loan.getLoanType().getLoanTypeName());
            dto.setInterestRate(loan.getLoanType().getInterestRate());

            if (loan.getLoanType().getBank() != null) {
                dto.setBankId(loan.getLoanType().getBank().getBankId());
                dto.setBankName(loan.getLoanType().getBank().getBankName());
            }
        }

        dto.setLoanAmount(loan.getLoanAmount());
        dto.setTermMonths(loan.getTermMonths());
        dto.setStartDate(loan.getStartDate());
        dto.setEndDate(loan.getEndDate());
        dto.setStatus(loan.getStatus());

        if (loan.getClient() != null) {
            dto.setClientId(loan.getClient().getUserId());
            dto.setClientName(loan.getClient().getUsername());
        }

        return dto;
    }

    public BigDecimal calculateEffectiveInterestRate(Loan loan) {
        List<BigDecimal> cashFlows = new ArrayList<>();
        cashFlows.add(loan.getLoanAmount().negate());

        loan.getPayments().stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate))
                .forEach(p -> cashFlows.add(p.getAmount()));

        BigDecimal guessRate = BigDecimal.valueOf(0.1);
        BigDecimal tolerance = BigDecimal.valueOf(1e-6);
        int maxIterations = 100;

        for (int i = 0; i < maxIterations; i++) {
            BigDecimal npv = BigDecimal.ZERO;
            BigDecimal derivative = BigDecimal.ZERO;

            for (int t = 0; t < cashFlows.size(); t++) {
                BigDecimal cf = cashFlows.get(t);
                BigDecimal denominator = BigDecimal.ONE.add(guessRate).pow(t);
                npv = npv.add(cf.divide(denominator, 10, RoundingMode.HALF_UP));

                if (t > 0) {
                    derivative = derivative.subtract(
                            BigDecimal.valueOf(t).multiply(cf)
                                    .divide(denominator.multiply(BigDecimal.ONE.add(guessRate)),
                                            10, RoundingMode.HALF_UP));
                }
            }

            BigDecimal newRate = guessRate.subtract(npv.divide(derivative, 10, RoundingMode.HALF_UP));

            if (newRate.subtract(guessRate).abs().compareTo(tolerance) < 0) {
                return newRate.multiply(BigDecimal.valueOf(100));
            }

            guessRate = newRate;
        }

        throw new ArithmeticException("Не удалось рассчитать эффективную процентную ставку");
    }

    public List<LoanDTO> getLoanDTOsByBankName(String bankName) {
        List<Loan> loans = loanRepository.findByLoanTypeBankName(bankName);
        return loans.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LoanDTO> getAllLoansWithBankInfo() {
        List<Loan> loans = loanRepository.getAllLoansWithBankInfo();
        return loans.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LoanDTO> getLoanDTOsByClientId(Long clientId) {
        List<Loan> loans = loanRepository.findByClientId(clientId);
        return loans.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public String deleteLoan(Long loanId, Long currentUserId) {
        try {
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new EntityNotFoundException("Кредит не найден"));

            loanRepository.delete(loan);
            LOG.info("Кредит {} удален пользователем {}", loanId, currentUserId);
            return null;

        } catch (EntityNotFoundException e) {
            return e.getMessage();
        } catch (Exception e) {
            LOG.error("Ошибка при удалении кредита", e);
            return "Внутренняя ошибка сервера";
        }
    }
}


