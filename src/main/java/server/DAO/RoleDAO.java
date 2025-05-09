package server.DAO;

import org.hibernate.Session;
import server.Entities.Role;
import lombok.Getter;
import lombok.Setter;
import java.util.Optional;

@Getter
@Setter
public class RoleDAO extends AbstractDAO<Role, Long> {
    public RoleDAO() {
        super(Role.class);
    }

    public Optional<Role> findByName(String name) {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM Role WHERE roleName = :name", Role.class)
                    .setParameter("name", name)
                    .uniqueResultOptional();
        }
    }
}

