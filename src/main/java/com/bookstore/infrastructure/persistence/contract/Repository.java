package com.bookstore.infrastructure.persistence.contract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Repository<T> {
    Optional<T> findById(UUID id);
    List<T> findAll();
    void save(T entity);
    void update(T entity);
    void delete(UUID id);
}