package server.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import config.LocalDateAdapter;
import enums.PaymentType;
import exeption.PaymentConversionException;
import exeption.PaymentException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.DAO.LoanDAO;
import server.DAO.PaymentDAO;
import server.DTO.PaymentDTO;
import server.DTO.PaymentScheduleDTO;
import server.Entities.Loan;
import server.Entities.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class PaymentService {
    private final PaymentDAO paymentRepository = new PaymentDAO();
    private final LoanDAO loanRepository = new LoanDAO();
    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
    private final LoanService loanService = new LoanService();
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();

    @Transactional
    public Payment createPayment(Long loanId, BigDecimal amount, PaymentType paymentType) {
        Loan loan = loanRepository.findByIdWithLoanType(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Кредит не найден"));

        Hibernate.initialize(loan.getLoanType());

        validatePayment(loan, amount, paymentType);

        Payment payment = buildPayment(loan, amount, paymentType);
        return paymentRepository.save(payment);
    }

    private void validatePayment(Loan loan, BigDecimal amount, PaymentType type) {
        switch(type) {
            case FULL_PREPAYMENT:
                validateFullPrepayment(loan, amount);
                break;
            case PARTIAL_PREPAYMENT:
                validatePartialPrepayment(loan, amount);
                break;
            case REGULAR:
                validateRegularPayment(loan, amount);
                break;
        }
    }

    private Payment buildPayment(Loan loan, BigDecimal amount, PaymentType type) {
        Payment payment = new Payment();
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(amount);
        payment.setPaymentType(type);
        payment.setLoan(loan);
        return payment;
    }

    private void validateFullPrepayment(Loan loan, BigDecimal amount) {
        BigDecimal remainingDebt = calculateRemainingDebt(loan);
        if (amount.compareTo(remainingDebt) < 0) {
            throw new PaymentException("Для полного погашения необходимо внести " + remainingDebt);
        }
    }

    public void validatePartialPrepayment(Loan loan, BigDecimal amount) throws PaymentException {
        PaymentScheduleDTO nextPayment = getNextPaymentSchedule(loan);

        BigDecimal remainingDebt = calculateRemainingDebt(loan);

        if (amount.compareTo(remainingDebt) >= 0) {
            throw new PaymentException("Для полного погашения используйте соответствующую функцию");
        }

        BigDecimal minPrepayment = nextPayment.getAmount().multiply(new BigDecimal("1.5")); // 1.5x of regular payment
        BigDecimal minPercent = remainingDebt.multiply(new BigDecimal("0.15")); // 15% of remaining debt

        BigDecimal requiredMin = minPrepayment.max(minPercent);

        if (amount.compareTo(requiredMin) < 0) {
            throw new PaymentException(String.format(
                    "Минимальная сумма частичного погашения: %,.2f ₽ (%.0f%% от остатка или в 1.5 раза больше платежа)",
                    requiredMin,
                    requiredMin.divide(remainingDebt, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            ));
        }
    }

    private void validateRegularPayment(Loan loan, BigDecimal amount) {
        PaymentScheduleDTO nextPayment = getNextPaymentSchedule(loan);
        if (nextPayment != null && amount.compareTo(nextPayment.getAmount()) < 0) {
            throw new PaymentException("Минимальный платеж: " + nextPayment.getAmount());
        }
    }

    @Transactional
    public PaymentScheduleDTO getNextPaymentSchedule(Loan loan) {
        Hibernate.initialize(loan.getLoanType());

        List<Payment> madePayments = paymentRepository.findByLoanIdOrderByPaymentDateAsc(loan.getLoanId());
        List<PaymentScheduleDTO> schedule = generateSchedule(loan.getLoanId());

        return schedule.stream()
                .filter(p -> madePayments.stream().noneMatch(mp ->
                        isSameMonthAndYear(mp.getPaymentDate(), p.getDueDate())))
                .min(Comparator.comparing(PaymentScheduleDTO::getDueDate))
                .orElse(null);
    }

    private boolean isSameMonthAndYear(LocalDate date1, LocalDate date2) {
        return date1.getMonth() == date2.getMonth() &&
                date1.getYear() == date2.getYear();
    }

    public List<PaymentScheduleDTO> getFullPaymentSchedule(Loan loan) {
        return generateSchedule(loan.getLoanId());
    }

    private void updateLoanStatus(Loan loan) {
        BigDecimal totalPaid = paymentRepository.sumPaymentsByLoan(loan.getLoanId());
        if (totalPaid.compareTo(loan.getLoanAmount()) >= 0) {
            loan.setStatus("PAID");
            loanRepository.save(loan);
        }
    }

    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Платеж с id " + paymentId + " не найден"));
    }

    public List<Payment> getPaymentsByLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит с id " + loanId + " не найден"));

        return new ArrayList<>(loan.getPayments());
    }

    public void deletePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("Платеж с id " + paymentId + " не найден"));

        paymentRepository.delete(payment);
    }

    public PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setAmount(payment.getAmount());
        dto.setPaymentType(payment.getPaymentType().name());
        return dto;
    }

    public Payment convertToEntity(PaymentDTO dto, Loan loan) {
        Payment payment = new Payment();
        payment.setLoan(loan);
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setAmount(dto.getAmount());

        try {
            payment.setPaymentType(PaymentType.valueOf(dto.getPaymentType()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PaymentConversionException("Неверный тип платежа: " + dto.getPaymentType());
        }

        return payment;
    }

    public BigDecimal calculateRemainingDebt(Loan loan) {
        BigDecimal totalPaid = paymentRepository.sumPaymentsByLoan(loan.getLoanId());
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        return loan.getLoanAmount().subtract(totalPaid).max(BigDecimal.ZERO);
    }

    public JsonObject getLoanDetails(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));

        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("loan", gson.toJsonTree(loan));
        response.add("schedule", gson.toJsonTree(generateSchedule(loanId)));
        response.addProperty("remainingDebt", calculateRemainingDebt(loan).toString());

        PaymentScheduleDTO nextPayment = getNextPaymentSchedule(loan);
        if (nextPayment != null) {
            response.add("nextPayment", gson.toJsonTree(nextPayment));
        }

        return response;
    }

    // Метод генерации графика платежей
    @Transactional
    public List<PaymentScheduleDTO> generateSchedule(Long loanId) {
        Loan loan = loanRepository.findByIdWithLoanType(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Кредит не найден"));
        Hibernate.initialize(loan.getLoanType());

        if ("CLOSED".equals(loan.getStatus())) {
            return Collections.emptyList();
        }

        List<Payment> payments = paymentRepository.findByLoanIdOrderByPaymentDateAsc(loanId);

        BigDecimal remainingPrincipal = calculateRemainingDebt(loan);
        if (remainingPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }

        BigDecimal monthlyRate = loan.getLoanType().getInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        int remainingMonths = loan.getTermMonths() - payments.size();

        BigDecimal temp = BigDecimal.ONE.add(monthlyRate).pow(remainingMonths);
        BigDecimal monthlyPayment = remainingPrincipal.multiply(monthlyRate)
                .multiply(temp)
                .divide(temp.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

        List<PaymentScheduleDTO> schedule = new ArrayList<>();
        LocalDate nextDate = payments.isEmpty()
                ? loan.getStartDate()
                : payments.get(payments.size()-1).getPaymentDate().plusMonths(1);

        BigDecimal currentBalance = remainingPrincipal;

        for (int i = 0; i < remainingMonths; i++) {
            PaymentScheduleDTO dto = new PaymentScheduleDTO();
            dto.setPaymentNumber(payments.size() + i + 1);
            dto.setAmount(monthlyPayment);
            dto.setDueDate(nextDate);

            BigDecimal interest = currentBalance.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal principal = monthlyPayment.subtract(interest)
                    .setScale(2, RoundingMode.HALF_UP);

            if (i == remainingMonths - 1) {
                principal = currentBalance;
                dto.setAmount(principal.add(interest));
            }

            dto.setInterestPart(interest);
            dto.setPrincipalPart(principal);

            schedule.add(dto);
            currentBalance = currentBalance.subtract(principal);
            nextDate = nextDate.plusMonths(1);
        }

        return schedule;
    }

    public List<PaymentScheduleDTO> regenerateSchedule(Loan loan, List<Payment> payments) {
        BigDecimal totalLoanAmount = loan.getLoanAmount();
        BigDecimal annualInterestRate = loan.getLoanType().getInterestRate();
        int totalMonths = loan.getTermMonths();

        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingPrincipal = totalLoanAmount.subtract(totalPaid);

        if (remainingPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }

        int monthsPaid = payments.size();
        int remainingMonths = totalMonths - monthsPaid;

        BigDecimal monthlyRate = annualInterestRate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal one = BigDecimal.ONE;
        BigDecimal temp = one.add(monthlyRate).pow(remainingMonths);
        BigDecimal monthlyPayment = remainingPrincipal
                .multiply(monthlyRate)
                .multiply(temp)
                .divide(temp.subtract(one), 2, RoundingMode.HALF_UP);

        // Находим максимальную дату из существующих платежей или используем дату начала кредита
        LocalDate lastPaymentDate = payments.stream()
                .map(Payment::getPaymentDate)
                .max(LocalDate::compareTo)
                .orElse(loan.getStartDate());

        LocalDate nextPaymentDate = lastPaymentDate.plusMonths(1); // Следующий месяц после последнего платежа

        List<PaymentScheduleDTO> schedule = new ArrayList<>();

        for (int i = 0; i < remainingMonths; i++) {
            PaymentScheduleDTO dto = new PaymentScheduleDTO();
            dto.setPaymentNumber(monthsPaid + i + 1);
            dto.setAmount(monthlyPayment);
            dto.setDueDate(nextPaymentDate);
            schedule.add(dto);

            nextPaymentDate = nextPaymentDate.plusMonths(1);
        }

        return schedule;
    }

    public List<PaymentDTO> getPaymentHistory(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Кредит не найден"));

        return paymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}

