package server.service;

import config.HibernateConfig;
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

            // 1. Получаем банк со всеми связями
            Bank bank = session.get(Bank.class, bankId);
            if (bank == null) {
                throw new IllegalArgumentException("Bank not found with id: " + bankId);
            }

            // 2. Удаляем платежи → кредиты → типы кредитов → банк
            deleteBankRelationsCascade(session, bank);

            // 3. Удаляем сам банк
            session.delete(bank);

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

    private void deleteBankRelationsCascade(Session session, Bank bank) {
        // Для каждого типа кредита банка
        for (LoanType loanType : new ArrayList<>(bank.getLoanTypes())) {

            // Для каждого кредита этого типа
            for (Loan loan : new ArrayList<>(loanType.getLoans())) {

                // Удаляем все платежи по кредиту
                session.createQuery("DELETE FROM Payment p WHERE p.loan.loanId = :loanId")
                        .setParameter("loanId", loan.getLoanId())
                        .executeUpdate();

                // Удаляем ссылку у пользователя
                User user = loan.getClient();
                user.getLoans().remove(loan);
                session.update(user);

                // Удаляем сам кредит
                session.delete(loan);
            }

            // Удаляем тип кредита
            session.delete(loanType);
        }
    }
}
