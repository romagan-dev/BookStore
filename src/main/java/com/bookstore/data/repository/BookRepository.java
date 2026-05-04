package com.bookstore.data.repository;

import com.bookstore.domain.entity.Book;
import java.util.List;
import java.util.Optional;

public interface BookRepository {
  Optional<Book> findById(Long id);

  List<Book> findAll();

  void save(Book book);
}
