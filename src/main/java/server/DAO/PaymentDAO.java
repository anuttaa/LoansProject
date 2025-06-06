package server.DAO;

import org.hibernate.Session;
import server.Entities.Payment;

import java.math.BigDecimal;
import java.util.List;

public class PaymentDAO extends AbstractDAO<Payment, Long> {
    public PaymentDAO() {
        super(Payment.class);
    }

    public BigDecimal sumPaymentsByLoan(Long loanId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.loan.loanId = :loanId",
                            BigDecimal.class)
                    .setParameter("loanId", loanId)
                    .uniqueResult();
        }
    }

    public List<Payment> findByLoanIdOrderByPaymentDateAsc(Long loanId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "FROM Payment p WHERE p.loan.loanId = :loanId ORDER BY p.paymentDate ASC",
                            Payment.class)
                    .setParameter("loanId", loanId)
                    .list();
        }
    }

    public List<Payment> findByLoanIdOrderByPaymentDateDesc(Long loanId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery(
                            "FROM Payment p WHERE p.loan.loanId = :loanId ORDER BY p.paymentDate DESC",
                            Payment.class)
                    .setParameter("loanId", loanId)
                    .list();
        }
    }
}

