package com.bookstore.application.impl;

import com.bookstore.application.contract.AuthorService;
import com.bookstore.domain.entity.Author;
import com.bookstore.infrastructure.persistence.impl.JdbcAuthorRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Реалізація сервісу для роботи з авторами.
 */
public class AuthorServiceImpl implements AuthorService {

    private final JdbcAuthorRepository authorRepository;

    public AuthorServiceImpl(JdbcAuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Override
    public List<Author> findAll() {
        return authorRepository.findAll();
    }

    @Override
    public Optional<Author> findById(UUID id) {
        return authorRepository.findById(id);
    }

    @Override
    public List<Author> search(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return authorRepository.findAll();
        }
        return authorRepository.findByLastName(lastName.trim());
    }

    @Override
    public Author create(String firstName, String lastName) {
        validateName(firstName, "Ім'я");
        validateName(lastName, "Прізвище");

        Author author = Author.builder()
              .id(UUID.randomUUID())
              .firstName(firstName.trim())
              .lastName(lastName.trim())
              .build();

        authorRepository.save(author);
        return author;
    }

    @Override
    public Author update(UUID id, String firstName, String lastName) {
        Author author = authorRepository.findById(id)
              .orElseThrow(() -> new IllegalArgumentException(
                    "Автора не знайдено: " + id));

        validateName(firstName, "Ім'я");
        validateName(lastName, "Прізвище");

        author.setFirstName(firstName.trim());
        author.setLastName(lastName.trim());
        authorRepository.update(author);
        return author;
    }

    @Override
    public void delete(UUID id) {
        if (!authorRepository.existsById(id)) {
            throw new IllegalArgumentException("Автора не знайдено: " + id);
        }
        authorRepository.deleteById(id);
    }

    private void validateName(String name, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не може бути порожнім");
        }
        if (name.trim().length() < 2) {
            throw new IllegalArgumentException(
                  fieldName + " має містити мінімум 2 символи");
        }
        if (name.trim().length() > 50) {
            throw new IllegalArgumentException(
                  fieldName + " не може бути довшим за 50 символів");
        }
    }
}