package com.bookstore.data.repository;

import com.bookstore.domain.entity.Book;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository {
  Optional<Book> findById(UUID id);

  List<Book> findAll();

  void save(Book book);
}
