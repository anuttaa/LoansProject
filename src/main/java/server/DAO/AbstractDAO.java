package server.DAO;

import config.HibernateConfig;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public abstract class AbstractDAO<T, ID> implements DAO<T, ID> {
    private final Class<T> entityClass;
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDAO.class);

    protected AbstractDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected Session getCurrentSession() {
        return HibernateConfig.getSessionFactory().openSession();
    }

    @Override
    public T save(T entity) {
        Transaction tx = null;
        try (Session session = getCurrentSession()) {
            tx = session.beginTransaction();
            session.persist(entity);
            tx.commit();
            return entity;
        } catch (Exception e) {
            if (tx != null && tx.getStatus().canRollback()) {
                try {
                    tx.rollback();
                } catch (Exception ex) {
                    LOG.error("Ошибка при rollback транзакции", ex);
                }
            }
            throw new RuntimeException("Failed to save " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        try (Session session = getCurrentSession()) {
            return Optional.ofNullable(session.get(entityClass, id));
        }
    }

    @Override
    public List<T> findAll() {
        try (Session session = getCurrentSession()) {
            return session.createQuery("FROM " + entityClass.getName(), entityClass).list();
        }
    }

    @Override
    public T update(T entity) {
        Transaction tx = null;
        try (Session session = getCurrentSession()) {
            tx = session.beginTransaction();
            // Используем merge, чтобы получить обновлённый объект
            T updatedEntity = (T) session.merge(entity);
            tx.commit();
            return updatedEntity;
        } catch (Exception e) {
            if (tx != null && tx.getStatus().canRollback()) {
                tx.rollback();
            }
            throw new RuntimeException("Failed to update " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public void delete(T entity) {
        Transaction tx = null;
        try (Session session = getCurrentSession()) {
            tx = session.beginTransaction();
            session.delete(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.getStatus().canRollback()) {
                tx.rollback();
            }
            throw new RuntimeException("Failed to delete " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public void deleteById(ID id) {
        Transaction tx = null;
        try (Session session = getCurrentSession()) {
            tx = session.beginTransaction();
            T entity = session.get(entityClass, id);
            if (entity != null) {
                session.delete(entity);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.getStatus().canRollback()) {
                tx.rollback();
            }
            throw new RuntimeException("Failed to delete " + entityClass.getSimpleName() + " by id", e);
        }
    }
}

