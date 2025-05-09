package server.DAO;

import org.hibernate.Session;
import server.Entities.Payment;
import java.util.List;

public class PaymentDAO extends AbstractDAO<Payment, Long> {
    public PaymentDAO() {
        super(Payment.class);
    }

    public List<Payment> findByLoanId(Long loanId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM Payment WHERE loan.loanId = :loanId", Payment.class)
                    .setParameter("loanId", loanId)
                    .list();
        }
    }

    public List<Payment> findByPaymentType(String paymentType) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM Payment WHERE paymentType = :paymentType", Payment.class)
                    .setParameter("paymentType", paymentType)
                    .list();
        }
    }
}

