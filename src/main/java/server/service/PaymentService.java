package server.service;

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
import java.util.Date;
import java.util.*;

import static java.lang.Math.round;

public class PaymentService {
    private final PaymentDAO paymentRepository = new PaymentDAO();
    private final LoanDAO loanRepository = new LoanDAO();
    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    public Payment createPayment(PaymentDTO paymentDTO, Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит с id " + loanId + " не найден"));

        Payment payment = new Payment();
        payment.setLoan(loan);
        payment.setPaymentDate(paymentDTO.getPaymentDate());
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentType(paymentDTO.getPaymentType());

        return paymentRepository.save(payment);
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
        dto.setPaymentType(payment.getPaymentType());
        return dto;
    }

    public Payment convertToEntity(PaymentDTO dto, Loan loan) {
        Payment payment = new Payment();
        payment.setLoan(loan);
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setAmount(dto.getAmount());
        payment.setPaymentType(dto.getPaymentType());
        return payment;
    }

    public List<PaymentScheduleDTO> generateInitialSchedule(Loan loan) {
        BigDecimal principal = loan.getLoanAmount();
        BigDecimal annualRate = loan.getLoanType().getInterestRate();
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        int months = loan.getTermMonths();

        BigDecimal one = BigDecimal.ONE;
        BigDecimal temp = one.add(monthlyRate).pow(months);
        BigDecimal monthlyPayment = principal.multiply(monthlyRate)
                .multiply(temp)
                .divide(temp.subtract(one), 2, RoundingMode.HALF_UP);

        List<PaymentScheduleDTO> schedule = new ArrayList<>();
        LocalDate paymentDate = loan.getStartDate(); // Используем LocalDate

        for (int i = 0; i < months; i++) {
            PaymentScheduleDTO dto = new PaymentScheduleDTO();
            dto.setPaymentNumber(i + 1);
            dto.setAmount(monthlyPayment);
            dto.setDueDate(paymentDate);
            schedule.add(dto);

            paymentDate = paymentDate.plusMonths(1); // Просто добавляем месяц
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
}

