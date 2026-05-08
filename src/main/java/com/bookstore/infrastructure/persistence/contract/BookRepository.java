package com.bookstore.infrastructure.persistence.contract;

import com.bookstore.domain.entity.Book;
import java.util.List;

public interface BookRepository extends Repository<Book> {
    List<Book> findByTitle(String title);
    List<Book> findByCategoryId(java.util.UUID categoryId);
}