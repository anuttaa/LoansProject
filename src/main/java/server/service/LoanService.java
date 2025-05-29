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
        // 1. Загружаем сущности с полными связями
        User client = userRepository.findById(loanDTO.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));

        LoanType loanType = loanTypeRepository.findByIdWithAllRelations(loanDTO.getLoanTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Тип кредита не найден"));

        // 2. Создаем и сохраняем кредит
        Loan loan = new Loan();
        loan.setClient(client);
        loan.setLoanType(loanType);
        loan.setLoanAmount(loanDTO.getLoanAmount());
        loan.setTermMonths(loanDTO.getTermMonths());
        loan.setStartDate(LocalDate.now());
        loan.setEndDate(calculateEndDate(loan.getStartDate(), loan.getTermMonths()));
        loan.setStatus("PENDING");

        Loan savedLoan = loanRepository.save(loan);

        // 3. Перезагружаем с полными связями
        Loan fullLoan = loanRepository.findByIdWithAllRelations(savedLoan.getLoanId())
                .orElseThrow(() -> new EntityNotFoundException("Кредит не найден после сохранения"));

        // 4. Конвертируем в DTO
        return convertToDTO(fullLoan);
    }

    @Transactional
    public LoanDTO getLoanDTOById(Long loanId) {
        Loan loan = loanRepository.findByIdWithAllRelations(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит не найден"));
        return convertToDTO(loan);
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

        // Информация о клиенте
        if (createdLoan.getClientId() != null) {
            loanJson.addProperty("clientId", createdLoan.getClientId());
        }

        // Информация о типе кредита
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

    public LoanDTO convertToDTO(Loan loan) {
        LoanDTO dto = new LoanDTO();
        dto.setLoanId(loan.getLoanId());
        dto.setLoanAmount(loan.getLoanAmount());
        dto.setTermMonths(loan.getTermMonths());
        dto.setStatus(loan.getStatus());
        dto.setStartDate(loan.getStartDate());
        dto.setEndDate(loan.getEndDate());

        // Для связанных сущностей используем только ID
        if (loan.getClient() != null) {
            dto.setClientId(loan.getClient().getUserId());
        }

        if (loan.getLoanType() != null) {
            dto.setLoanTypeId(loan.getLoanType().getLoanTypeId());
            dto.setLoanTypeName(loan.getLoanType().getLoanTypeName());
            dto.setInterestRate(loan.getLoanType().getInterestRate());

            if (loan.getLoanType().getBank() != null) {
                dto.setBankId(loan.getLoanType().getBank().getBankId());
            }
        }

        return dto;
    }

    public Loan convertToEntity(LoanDTO loanDTO) {
        User client = userRepository.findById(loanDTO.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        LoanType loanType = loanTypeRepository.findById(loanDTO.getLoanTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Loan type not found"));

        Loan loan = new Loan();
        loan.setClient(client);
        loan.setLoanType(loanType);
        loan.setLoanAmount(loanDTO.getLoanAmount());
        loan.setTermMonths(loanDTO.getTermMonths());
        loan.setStartDate(loanDTO.getStartDate());
        loan.setEndDate(calculateEndDate(loanDTO.getStartDate(), loanDTO.getTermMonths()));
        loan.setStatus(loanDTO.getStatus() != null ? loanDTO.getStatus() : "ACTIVE");

        return loan;
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

    @Transactional
    public LoanDTO updateLoan(LoanDTO loanDTO, Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит не найден"));

        LoanType loanType = loanTypeRepository.findById(loanDTO.getLoanTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Тип кредита не найден"));

        // Обновляем поля
        loan.setLoanType(loanType);
        loan.setLoanAmount(loanDTO.getLoanAmount());
        loan.setTermMonths(loanDTO.getTermMonths());
        loan.setStartDate(loanDTO.getStartDate());
        loan.setEndDate(calculateEndDate(loanDTO.getStartDate(), loanDTO.getTermMonths()));
        loan.setStatus(loanDTO.getStatus() != null ? loanDTO.getStatus() : loan.getStatus());

        Loan updatedLoan = loanRepository.update(loan);
        return convertToDTO(updatedLoan);
    }

    public void deleteLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит с id " + loanId + " не найден"));
        loanRepository.delete(loan);
    }

    public List<Loan> getLoansByUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с id " + userId + " не найден"));
        return loanRepository.findByClientId(userId);
    }

    public List<Payment> getPaymentsByLoan(Long loanId) {
        Loan loan = getLoanById(loanId);
        return new ArrayList<>(loan.getPayments());
    }

    public List<PaymentScheduleDTO> generatePaymentSchedule(Long loanId) {
        Loan loan = getLoanById(loanId);
        BigDecimal principal = loan.getLoanAmount();
        BigDecimal monthlyRate = loan.getLoanType().getInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        int months = loan.getTermMonths();

        List<PaymentScheduleDTO> schedule = new ArrayList<>();
        LocalDate paymentDate = loan.getStartDate(); // Теперь LocalDate

        // Формула аннуитетного платежа
        BigDecimal monthlyPayment = principal.multiply(monthlyRate)
                .divide(BigDecimal.ONE.subtract(
                                BigDecimal.ONE.add(monthlyRate).pow(-months, MathContext.DECIMAL64)),
                        10, RoundingMode.HALF_UP);

        for (int i = 0; i < months; i++) {
            PaymentScheduleDTO dto = new PaymentScheduleDTO();
            dto.setPaymentNumber(i + 1);
            dto.setAmount(monthlyPayment.setScale(2, RoundingMode.HALF_UP));
            dto.setDueDate(paymentDate);
            schedule.add(dto);

            paymentDate = paymentDate.plusMonths(1); // Корректно для LocalDate
        }

        return schedule;
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

    private LocalDate calculateEndDate(LocalDate startDate, int termMonths) {
        return startDate.plusMonths(termMonths);
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public List<Loan> getLoansByBank(Long bankId) {
        bankRepository.findById(bankId)
                .orElseThrow(() -> new NoSuchElementException("Банк с id " + bankId + " не найден"));

        return loanRepository.findByLoanTypeBankId(bankId);
    }

    public List<Loan> getLoansByBankName(String bankName) {
        return loanRepository.findByLoanTypeBankName(bankName);
    }

    public List<PaymentScheduleDTO> getPaymentSchedule(Long loanId) {
        return generatePaymentSchedule(loanId);
    }

    public List<Loan> getLoansByLoanTypeBankName(String bankName) {
        return loanRepository.findByLoanTypeBankName(bankName);
    }

    public List<Loan> getLoansByClientId(Long clientId) {
        return loanRepository.findByClientId(clientId);
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

            // Проверка прав доступа
            if (!hasPermissionToDelete(loan, currentUserId)) {
                return "Недостаточно прав для удаления кредита";
            }

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

    private boolean hasPermissionToDelete(Loan loan, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        return "1".equals(currentUser.getRole()) ||
                loan.getClient().getUserId().equals(currentUserId);
    }
}


