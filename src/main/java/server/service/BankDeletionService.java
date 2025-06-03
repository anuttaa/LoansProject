package server.service;

import config.HibernateConfig;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Entities.Bank;
import server.Entities.Loan;
import server.Entities.LoanType;
import server.Entities.User;

import java.util.ArrayList;

public class BankDeletionService {
    private static final Logger LOG = LoggerFactory.getLogger(BankDeletionService.class);

    public void deleteBankCascading(Long bankId) {
        Session session = HibernateConfig.getSessionFactory().openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            session.createNativeQuery(
                            "DELETE FROM payment WHERE loan_id IN " +
                                    "(SELECT loan_id FROM loan WHERE loan_type_id IN " +
                                    "(SELECT loan_type_id FROM loan_type WHERE bank_id = :bankId))")
                    .setParameter("bankId", bankId)
                    .executeUpdate();

            session.createNativeQuery(
                            "DELETE FROM loan WHERE loan_type_id IN " +
                                    "(SELECT loan_type_id FROM loan_type WHERE bank_id = :bankId)")
                    .setParameter("bankId", bankId)
                    .executeUpdate();

            session.createNativeQuery(
                            "DELETE FROM loan_type WHERE bank_id = :bankId")
                    .setParameter("bankId", bankId)
                    .executeUpdate();

            session.createNativeQuery(
                            "DELETE FROM bank WHERE bank_id = :bankId")
                    .setParameter("bankId", bankId)
                    .executeUpdate();

            tx.commit();
            LOG.info("Bank {} and all related data deleted successfully", bankId);

        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOG.error("Error deleting bank {}", bankId, e);
            throw new RuntimeException("Bank deletion failed", e);
        } finally {
            session.close();
        }
    }
}
