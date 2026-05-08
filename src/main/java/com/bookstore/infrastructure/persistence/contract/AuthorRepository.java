package com.bookstore.infrastructure.persistence.contract;

import com.bookstore.domain.entity.Author;

public interface AuthorRepository extends Repository<Author> {
    // Тут можна додати специфічні методи, наприклад пошук за прізвищем
}