package server.service;

import java.util.List;
import java.util.Optional;

public interface Service<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    T update(T entity);
    void delete(T entity);
}