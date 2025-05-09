package server.DAO;

import org.hibernate.Session;
import server.Entities.Bank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class BankDAO extends AbstractDAO<Bank, Long> {
    public BankDAO() {
        super(Bank.class);
    }

    public Optional<Bank> findByBankName(String bankName) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM Bank WHERE bankName = :bankName", Bank.class)
                    .setParameter("bankName", bankName)
                    .uniqueResultOptional();
        }
    }

    public List<Bank> findByUser(Long userId) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("SELECT b FROM Bank b JOIN b.users u WHERE u.userId = :userId", Bank.class)
                    .setParameter("userId", userId)
                    .list();
        }
    }
}

