package com.bookstore.application.impl;

import com.bookstore.application.contract.BookService;
import com.bookstore.domain.entity.Book;
import com.bookstore.domain.entity.Category;
import com.bookstore.infrastructure.persistence.impl.JdbcAuthorRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcBookRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcCategoryRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Реалізація сервісу для роботи з книгами.
 *
 * <p>Патерни: Dependency Injection, Facade. Інкапсулює бізнес-логіку: валідацію, управління
 * зв'язком книга-автор через проміжну таблицю.
 */
public class BookServiceImpl implements BookService {

    private final JdbcBookRepository bookRepository;
    private final JdbcCategoryRepository categoryRepository;
    private final JdbcAuthorRepository authorRepository;

    public BookServiceImpl(
          JdbcBookRepository bookRepository,
          JdbcCategoryRepository categoryRepository,
          JdbcAuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.authorRepository = authorRepository;
    }

    @Override
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    public Optional<Book> findById(UUID id) {
        return bookRepository.findById(id);
    }

    @Override
    public List<Book> search(String query) {
        if (query == null || query.isBlank()) {
            return bookRepository.findAll();
        }
        return bookRepository.findByTitle(query.trim());
    }

    @Override
    public List<Book> findByCategory(UUID categoryId) {
        return bookRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<Book> findInStock() {
        return bookRepository.findInStock();
    }

    @Override
    public List<Book> findByMaxPrice(BigDecimal maxPrice) {
        if (maxPrice == null || maxPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ціна має бути більше 0");
        }
        return bookRepository.findByMaxPrice(maxPrice);
    }

    @Override
    public Book create(String title, String isbn, BigDecimal price,
          int stockQuantity, String description, UUID categoryId) {
        validateTitle(title);
        validatePrice(price);
        validateStockQuantity(stockQuantity);

        Category category = categoryRepository.findById(categoryId)
              .orElseThrow(() -> new IllegalArgumentException(
                    "Категорію не знайдено: " + categoryId));

        Book book = Book.builder()
              .id(UUID.randomUUID())
              .title(title.trim())
              .isbn(isbn != null ? isbn.trim() : null)
              .price(price)
              .stockQuantity(stockQuantity)
              .description(description != null ? description.trim() : null)
              .category(category)
              .build();

        bookRepository.save(book);
        return book;
    }

    @Override
    public Book update(UUID id, String title, String isbn, BigDecimal price,
          int stockQuantity, String description, UUID categoryId) {
        Book book = bookRepository.findById(id)
              .orElseThrow(() -> new IllegalArgumentException(
                    "Книгу не знайдено: " + id));

        validateTitle(title);
        validatePrice(price);
        validateStockQuantity(stockQuantity);

        Category category = categoryRepository.findById(categoryId)
              .orElseThrow(() -> new IllegalArgumentException(
                    "Категорію не знайдено: " + categoryId));

        book.setTitle(title.trim());
        book.setIsbn(isbn != null ? isbn.trim() : null);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        book.setDescription(description != null ? description.trim() : null);
        book.setCategory(category);

        bookRepository.update(book);
        return book;
    }

    @Override
    public void delete(UUID id) {
        if (!bookRepository.existsById(id)) {
            throw new IllegalArgumentException("Книгу не знайдено: " + id);
        }
        bookRepository.deleteById(id);
    }

    /**
     * Додає автора до книги через проміжну таблицю book_authors.
     *
     * @param bookId UUID книги
     * @param authorId UUID автора
     */
    @Override
    public void addAuthor(UUID bookId, UUID authorId) {
        if (!bookRepository.existsById(bookId)) {
            throw new IllegalArgumentException("Книгу не знайдено: " + bookId);
        }
        if (!authorRepository.existsById(authorId)) {
            throw new IllegalArgumentException("Автора не знайдено: " + authorId);
        }

        String sql = "INSERT OR IGNORE INTO book_authors (book_id, author_id) VALUES (?, ?)";
        try (Connection conn = bookRepository.connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bookId.toString());
            stmt.setString(2, authorId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка додавання автора до книги", e);
        }
    }

    /**
     * Видаляє автора з книги.
     *
     * @param bookId UUID книги
     * @param authorId UUID автора
     */
    @Override
    public void removeAuthor(UUID bookId, UUID authorId) {
        String sql = "DELETE FROM book_authors WHERE book_id = ? AND author_id = ?";
        try (Connection conn = bookRepository.connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bookId.toString());
            stmt.setString(2, authorId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка видалення автора з книги", e);
        }
    }

    // =========================================================
    // Валідація
    // =========================================================

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Назва книги не може бути порожньою");
        }
        if (title.trim().length() > 255) {
            throw new IllegalArgumentException(
                  "Назва книги не може бути довшою за 255 символів");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Ціна не може бути порожньою");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ціна має бути більше 0");
        }
    }

    private void validateStockQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException(
                  "Кількість на складі не може бути від'ємною");
        }
    }
}