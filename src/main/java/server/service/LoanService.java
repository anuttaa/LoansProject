package server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.DAO.BankDAO;
import server.DAO.LoanDAO;
import server.DAO.UserDAO;
import server.DTO.LoanDTO;
import server.DTO.PaymentScheduleDTO;
import server.Entities.Bank;
import server.Entities.Loan;
import server.Entities.Payment;
import server.Entities.User;

import java.util.*;

public class LoanService {

    private final LoanDAO loanRepository = new LoanDAO();
    private final BankDAO bankRepository = new BankDAO();
    private final UserDAO userRepository = new UserDAO();
    private static final Logger LOG = LoggerFactory.getLogger(LoanService.class);

    public Loan createLoan(LoanDTO loanDTO, Long bankId) {
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new RuntimeException("Bank not found"));

        Loan loan = new Loan();
        loan.setLoanAmount(loanDTO.getLoanAmount());
        loan.setInterestRate(loanDTO.getInterestRate());
        loan.setStartDate(loanDTO.getStartDate());
        loan.setEndDate(loanDTO.getEndDate());
        loan.setBank(bank);

        bank.getLoans().add(loan);

        return loanRepository.save(loan);
    }

    public void addClientToLoan(Long loanId, Long clientId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        loan.getClients().add(client);
        loanRepository.save(loan);
    }


    public Loan getLoanById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит с id " + loanId + " не найден"));
    }

    public List<Loan> getLoansByBank(Long bankId) {
        bankRepository.findById(bankId)
                .orElseThrow(() -> new NoSuchElementException("Банк с id " + bankId + " не найден"));

        return loanRepository.findByBankId(bankId); // этот метод ты добавишь в LoanDAO
    }

    public Loan updateLoan(LoanDTO loanDTO, Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит с id " + loanId + " не найден"));

        loan.setLoanTypeName(loanDTO.getLoanTypeName());
        loan.setInterestRate(loanDTO.getInterestRate());
        loan.setLoanAmount(loanDTO.getLoanAmount());
        loan.setTermMonths(loanDTO.getTermMonths());
        loan.setStartDate(currentSqlDate());
        loan.setEndDate(calculateEndDate(loan.getStartDate(), loan.getTermMonths()));
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
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит с id " + loanId + " не найден"));

        return new ArrayList<>(loan.getPayments());
    }

    public List<PaymentScheduleDTO> generatePaymentSchedule(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Кредит с id " + loanId + " не найден"));

        double principal = loan.getLoanAmount();
        double monthlyRate = loan.getInterestRate() / 100 / 12;
        int months = loan.getTermMonths();

        List<PaymentScheduleDTO> schedule = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(loan.getStartDate());

        double monthlyPayment = (principal * monthlyRate) / (1 - Math.pow(1 + monthlyRate, -months));

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

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public double calculateEffectiveInterestRate(Loan loan) {
        List<Double> cashFlows = new ArrayList<>();

        // Первый денежный поток — получение кредита (отрицательное значение)
        cashFlows.add(-loan.getLoanAmount());

        // Добавляем все платежи (входящий поток)
        List<Payment> payments = loan.getPayments();
        payments.sort(Comparator.comparing(Payment::getPaymentDate));
        for (Payment payment : payments) {
            cashFlows.add(payment.getAmount());
        }

        double guessRate = 0.1; // начальное приближение
        double tolerance = 1e-6;
        int maxIterations = 100;

        for (int i = 0; i < maxIterations; i++) {
            double npv = 0.0;
            double derivative = 0.0;

            for (int t = 0; t < cashFlows.size(); t++) {
                double cf = cashFlows.get(t);
                npv += cf / Math.pow(1 + guessRate, t);
                derivative -= t * cf / Math.pow(1 + guessRate, t + 1);
            }

            double newRate = guessRate - npv / derivative;

            if (Math.abs(newRate - guessRate) < tolerance) {
                return newRate * 100; // результат в процентах
            }

            guessRate = newRate;
        }

        throw new ArithmeticException("Не удалось рассчитать эффективную процентную ставку");
    }



    public LoanDTO convertToDTO(Loan loan) {
        LoanDTO dto = new LoanDTO();
        dto.setLoanTypeName(loan.getLoanTypeName());
        dto.setInterestRate(loan.getInterestRate());
        dto.setLoanAmount(loan.getLoanAmount());
        dto.setTermMonths(loan.getTermMonths());
        dto.setStartDate(loan.getStartDate());
        dto.setEndDate(loan.getEndDate());
        dto.setStatus(loan.getStatus());
        return dto;
    }

    public Loan convertToEntity(LoanDTO loanDTO, Long bankId, Long clientId) {
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new NoSuchElementException("Банк с id " + bankId + " не найден"));

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new NoSuchElementException("Клиент с id " + clientId + " не найден"));

        Loan loan = new Loan();
        loan.setLoanTypeName(loanDTO.getLoanTypeName());
        loan.setInterestRate(loanDTO.getInterestRate());
        loan.setLoanAmount(loanDTO.getLoanAmount());
        loan.setTermMonths(loanDTO.getTermMonths());
        loan.setStartDate(currentSqlDate());
        loan.setEndDate(calculateEndDate(loan.getStartDate(), loan.getTermMonths()));
        loan.setStatus("ACTIVE");

        loan.setBank(bank);

        return loan;
    }

    private java.sql.Date currentSqlDate() {
        return new java.sql.Date(System.currentTimeMillis());
    }

    private java.sql.Date calculateEndDate(java.sql.Date startDate, int termMonths) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.MONTH, termMonths);
        return new java.sql.Date(calendar.getTimeInMillis());
    }
}


