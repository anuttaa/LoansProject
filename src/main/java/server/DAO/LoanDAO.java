package server.DAO;

import org.hibernate.Session;
import server.Entities.Loan;
import java.util.List;

public class LoanDAO extends AbstractDAO<Loan, Long> {
    public LoanDAO() {
        super(Loan.class);
    }

    public List<Loan> findByClientId(Long clientId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM Loan WHERE client.userId = :clientId", Loan.class)
                    .setParameter("clientId", clientId)
                    .list();
        }
    }

    public List<Loan> findByBankId(Long bankId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM Loan WHERE bank.bankId = :bankId", Loan.class)
                    .setParameter("bankId", bankId)
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
}

