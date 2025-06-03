package server.DAO;

import org.hibernate.Session;
import server.DTO.BankLoanCountDTO;
import server.DTO.MonthlyRateDTO;
import server.Entities.Loan;
import server.Entities.LoanType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LoanDAO extends AbstractDAO<Loan, Long> {
    public LoanDAO() {
        super(Loan.class);
    }

    public List<Loan> findByClientId(Long clientId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT DISTINCT l FROM Loan l " +
                                    "JOIN FETCH l.loanType lt " +
                                    "JOIN FETCH lt.bank b " +
                                    "JOIN FETCH l.client c " +
                                    "WHERE c.userId = :clientId", Loan.class)
                    .setParameter("clientId", clientId)
                    .list();
        }
    }

    public List<Loan> findByStatus(String status) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM Loan WHERE status = :status", Loan.class)
                    .setParameter("status", status)
                    .list();
        }
    }

    public List<Loan> findByLoanTypeBankName(String bankName) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT l FROM Loan l JOIN l.loanType lt JOIN lt.bank b WHERE b.bankName = :bankName",
                            Loan.class)
                    .setParameter("bankName", bankName)
                    .list();
        }
    }

    public List<Loan> findByLoanTypeBankId(Long bankId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT l FROM Loan l JOIN l.loanType lt WHERE lt.bank.bankId = :bankId",
                            Loan.class)
                    .setParameter("bankId", bankId)
                    .list();
        }
    }

    public List<Loan> findClientLoansWithDetails(Long clientId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT DISTINCT l FROM Loan l " +
                                    "JOIN FETCH l.loanType lt " +
                                    "JOIN FETCH lt.bank " +
                                    "JOIN FETCH l.client " +
                                    "WHERE l.client.userId = :clientId", Loan.class)
                    .setParameter("clientId", clientId)
                    .list();
        }
    }

    public Optional<Loan> findByIdWithAllRelations(Long loanId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT DISTINCT l FROM Loan l " +
                                    "LEFT JOIN FETCH l.client " +
                                    "LEFT JOIN FETCH l.loanType lt " +
                                    "LEFT JOIN FETCH lt.bank " +
                                    "LEFT JOIN FETCH l.payments " +
                                    "WHERE l.loanId = :loanId", Loan.class)
                    .setParameter("loanId", loanId)
                    .uniqueResultOptional();
        }
    }

    public List<Loan> getAllLoansWithBankInfo() {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT DISTINCT l FROM Loan l " +
                                    "JOIN FETCH l.loanType lt " +
                                    "JOIN FETCH lt.bank b " +
                                    "JOIN FETCH l.client c", Loan.class)
                    .list();
        }
    }

    public Optional<Loan> findByIdWithLoanType(Long loanId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT l FROM Loan l " +
                                    "JOIN FETCH l.loanType lt " +
                                    "WHERE l.loanId = :loanId", Loan.class)
                    .setParameter("loanId", loanId)
                    .uniqueResultOptional();
        }
    }

    public double getAverageInterestRate() {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT AVG(lt.interestRate) FROM Loan l JOIN l.loanType lt",
                            Double.class)
                    .uniqueResult();
        }
    }

    public String getMostPopularBank() {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT b.bankName FROM Loan l " +
                                    "JOIN l.loanType lt JOIN lt.bank b " +
                                    "GROUP BY b.bankName " +
                                    "ORDER BY COUNT(l) DESC", String.class)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    public int getTotalLoanCount() {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT COUNT(l) FROM Loan l",
                            Long.class)
                    .uniqueResult()
                    .intValue();
        }
    }

    public double getTotalLoanAmount() {
        try (Session session = getCurrentSession()) {
            BigDecimal result = session.createQuery(
                            "SELECT SUM(l.loanAmount) FROM Loan l",
                            BigDecimal.class)
                    .uniqueResult();
            return result != null ? result.doubleValue() : 0.0;
        }
    }

    public List<BankLoanCountDTO> getLoanCountByBank() {
        try (Session session = getCurrentSession()) {
            List<Object[]> results = session.createQuery(
                            "SELECT b.bankName, COUNT(l) FROM Loan l " +
                                    "JOIN l.loanType lt JOIN lt.bank b " +
                                    "GROUP BY b.bankName", Object[].class)
                    .list();

            return results.stream()
                    .map(o -> new BankLoanCountDTO((String) o[0], ((Long) o[1]).intValue()))
                    .collect(Collectors.toList());
        }
    }

    public List<MonthlyRateDTO> getMonthlyRateTrend() {
        try (Session session = getCurrentSession()) {
            List<Object[]> results = session.createQuery(
                            "SELECT FUNCTION('DATE_FORMAT', l.startDate, '%Y-%m'), AVG(lt.interestRate) " +
                                    "FROM Loan l JOIN l.loanType lt " +
                                    "GROUP BY FUNCTION('DATE_FORMAT', l.startDate, '%Y-%m') " +
                                    "ORDER BY FUNCTION('DATE_FORMAT', l.startDate, '%Y-%m')", Object[].class)
                    .list();

            return results.stream()
                    .map(o -> new MonthlyRateDTO((String) o[0], (Double) o[1]))
                    .collect(Collectors.toList());
        }
    }

}

