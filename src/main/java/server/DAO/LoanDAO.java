package server.DAO;

import org.hibernate.Session;
import server.Entities.Loan;
import server.Entities.LoanType;

import java.util.List;
import java.util.Optional;

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


}

