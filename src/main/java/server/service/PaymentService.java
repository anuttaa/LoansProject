package server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.DAO.LoanDAO;
import server.DAO.PaymentDAO;
import server.DTO.PaymentDTO;
import server.DTO.PaymentScheduleDTO;
import server.Entities.Loan;
import server.Entities.Payment;

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
        double principal = loan.getLoanAmount();
        double monthlyRate = loan.getInterestRate() / 100.0 / 12;
        int months = loan.getTermMonths();

        double monthlyPayment = (principal * monthlyRate) / (1 - Math.pow(1 + monthlyRate, -months));

        List<PaymentScheduleDTO> schedule = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(loan.getStartDate());

        for (int i = 0; i < months; i++) {
            PaymentScheduleDTO dto = new PaymentScheduleDTO();
            dto.setPaymentNumber(i + 1);
            dto.setAmount(round(monthlyPayment));
            dto.setDueDate(new java.sql.Date(calendar.getTimeInMillis()));
            schedule.add(dto);

            calendar.add(Calendar.MONTH, 1);
        }

        return schedule;
    }

    public List<PaymentScheduleDTO> regenerateSchedule(Loan loan, List<Payment> payments) {
        double totalLoanAmount = loan.getLoanAmount();
        double annualInterestRate = loan.getInterestRate();
        int totalMonths = loan.getTermMonths();

        double totalPaid = payments.stream().mapToDouble(Payment::getAmount).sum();
        double remainingPrincipal = totalLoanAmount - totalPaid;

        if (remainingPrincipal <= 0) {
            return Collections.emptyList(); // Всё погашено
        }

        int monthsPaid = payments.size();
        int remainingMonths = totalMonths - monthsPaid;

        double monthlyRate = annualInterestRate / 100.0 / 12;
        double monthlyPayment = (remainingPrincipal * monthlyRate) /
                (1 - Math.pow(1 + monthlyRate, -remainingMonths));

        List<PaymentScheduleDTO> schedule = new ArrayList<>();

        Date startDate = payments.stream()
                .map(Payment::getPaymentDate)
                .max(Date::compareTo)
                .orElse(new java.sql.Date(System.currentTimeMillis()));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.MONTH, 1); // следующий месяц

        for (int i = 0; i < remainingMonths; i++) {
            PaymentScheduleDTO dto = new PaymentScheduleDTO();
            dto.setPaymentNumber(monthsPaid + i + 1);
            dto.setAmount(round(monthlyPayment));
            dto.setDueDate(new java.sql.Date(calendar.getTimeInMillis()));
            schedule.add(dto);

            calendar.add(Calendar.MONTH, 1);
        }

        return schedule;
    }

}

