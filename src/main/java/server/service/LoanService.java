package server.service;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.DAO.BankDAO;
import server.DAO.LoanDAO;
import server.DAO.LoanTypeDAO;
import server.DAO.UserDAO;
import server.DTO.LoanDTO;
import server.DTO.PaymentScheduleDTO;
import server.Entities.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class LoanService {
    private final LoanDAO loanRepository = new LoanDAO();
    private final LoanTypeDAO loanTypeRepository = new LoanTypeDAO();
    private final UserDAO userRepository = new UserDAO();
    private final BankDAO bankRepository = new BankDAO();
    private static final Logger LOG = LoggerFactory.getLogger(LoanService.class);

    public Loan createLoan(LoanDTO loanDTO) {
        // 1. Проверяем существование клиента
        User client = userRepository.findById(loanDTO.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));

        // 2. Получаем тип кредита
        LoanType loanType = loanTypeRepository.findById(loanDTO.getLoanTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Тип кредита не найден"));

        // 3. Проверяем валидность данных
        if (loanDTO.getLoanAmount() == null || loanDTO.getLoanAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Некорректная сумма кредита");
        }
        if (loanDTO.getTermMonths() == null || loanDTO.getTermMonths() <= 0) {
            throw new IllegalArgumentException("Некорректный срок кредита");
        }

        // 4. Создаем и сохраняем кредит
        Loan loan = new Loan();
        loan.setClient(client);
        loan.setLoanType(loanType);
        loan.setLoanAmount(loanDTO.getLoanAmount());
        loan.setTermMonths(loanDTO.getTermMonths());
        loan.setStartDate(LocalDate.now()); // Устанавливаем текущую дату
        loan.setEndDate(calculateEndDate(loan.getStartDate(), loan.getTermMonths()));
        loan.setStatus("ACTIVE"); // Статус по умолчанию

        return loanRepository.save(loan);
    }

    public LoanDTO convertToDTO(Loan loan) {
        LoanDTO dto = new LoanDTO();
        dto.setLoanId(loan.getLoanId());
        dto.setClientId(loan.getClient().getUserId());
        dto.setLoanTypeId(loan.getLoanType().getLoanTypeId());
        dto.setBankId(loan.getLoanType().getBank().getBankId());
        dto.setLoanTypeName(loan.getLoanType().getLoanTypeName());
        dto.setInterestRate(loan.getLoanType().getInterestRate());
        dto.setLoanAmount(loan.getLoanAmount());
        dto.setTermMonths(loan.getTermMonths());
        dto.setStartDate(loan.getStartDate());
        dto.setEndDate(loan.getEndDate());
        dto.setStatus(loan.getStatus());
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

    public Loan updateLoan(LoanDTO loanDTO, Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит с id " + loanId + " не найден"));

        LoanType loanType = loanTypeRepository.findById(loanDTO.getLoanTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Loan type not found"));

        loan.setLoanType(loanType);
        loan.setLoanAmount(loanDTO.getLoanAmount());
        loan.setTermMonths(loanDTO.getTermMonths());
        loan.setStartDate(loanDTO.getStartDate());
        loan.setEndDate(calculateEndDate(loanDTO.getStartDate(), loanDTO.getTermMonths()));
        loan.setStatus(loanDTO.getStatus() != null ? loanDTO.getStatus() : loan.getStatus());

        return loanRepository.update(loan);
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

    public List<LoanDTO> getAllLoansWithBankInfo() {
        return loanRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}


