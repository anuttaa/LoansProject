package server.DAO;

import org.hibernate.Session;
import server.Entities.User;
import java.util.Optional;

public class UserDAO extends AbstractDAO<User, Long> {
    public UserDAO() {
        super(User.class);
    }

    public Optional<User> findByUsername(String username) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM User WHERE username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResultOptional();
        }
    }

    public long count() {
        try (Session session = getCurrentSession()) {
            return session.createQuery("SELECT COUNT(*) FROM User", Long.class)
                    .uniqueResult();
        }
    }
}

