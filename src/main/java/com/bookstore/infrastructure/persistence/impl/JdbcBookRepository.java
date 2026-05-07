package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.domain.entity.Author;
import com.bookstore.domain.entity.Book;
import com.bookstore.domain.entity.Category;
import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import com.bookstore.infrastructure.persistence.util.LazyList;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-реалізація репозиторію для сутності {@link Book}.
 *
 * <p>Наслідує {@link AbstractJdbcRepository} — реалізує лише специфічні для книг частини. Автори
 * завантажуються через {@link LazyList} — відкладено при першому зверненні до {@code
 * book.getAuthors()}.
 */
public class JdbcBookRepository extends AbstractJdbcRepository<Book, UUID> {

    /** Репозиторій авторів — потрібен для LazyList завантаження авторів книги. */
    private final JdbcAuthorRepository authorRepository;

    // =========================================================
    // SQL константи
    // =========================================================

    private static final String SQL_SELECT_BY_ID =
          """
          SELECT b.book_id, b.title, b.isbn, b.price,
                 b.stock_quantity, b.description,
                 c.category_id, c.name AS category_name,
                 c.description AS category_description
          FROM books b
          JOIN categories c ON b.category_id = c.category_id
          WHERE b.book_id = ?
          """;

    private static final String SQL_SELECT_ALL =
          """
          SELECT b.book_id, b.title, b.isbn, b.price,
                 b.stock_quantity, b.description,
                 c.category_id, c.name AS category_name,
                 c.description AS category_description
          FROM books b
          JOIN categories c ON b.category_id = c.category_id
          ORDER BY b.title
          """;

    private static final String SQL_INSERT =
          """
          INSERT INTO books
              (book_id, category_id, title, isbn, price, stock_quantity, description)
          VALUES (?, ?, ?, ?, ?, ?, ?)
          """;

    private static final String SQL_UPDATE =
          """
          UPDATE books
          SET category_id = ?, title = ?, isbn = ?,
              price = ?, stock_quantity = ?, description = ?
          WHERE book_id = ?
          """;

    private static final String SQL_FIND_BY_TITLE =
          """
          SELECT b.book_id, b.title, b.isbn, b.price,
                 b.stock_quantity, b.description,
                 c.category_id, c.name AS category_name,
                 c.description AS category_description
          FROM books b
          JOIN categories c ON b.category_id = c.category_id
          WHERE LOWER(b.title) LIKE LOWER(?)
          ORDER BY b.title
          """;

    private static final String SQL_FIND_BY_ISBN =
          """
          SELECT b.book_id, b.title, b.isbn, b.price,
                 b.stock_quantity, b.description,
                 c.category_id, c.name AS category_name,
                 c.description AS category_description
          FROM books b
          JOIN categories c ON b.category_id = c.category_id
          WHERE b.isbn = ?
          """;

    private static final String SQL_FIND_BY_CATEGORY =
          """
          SELECT b.book_id, b.title, b.isbn, b.price,
                 b.stock_quantity, b.description,
                 c.category_id, c.name AS category_name,
                 c.description AS category_description
          FROM books b
          JOIN categories c ON b.category_id = c.category_id
          WHERE b.category_id = ?
          ORDER BY b.title
          """;

    private static final String SQL_FIND_BY_MAX_PRICE =
          """
          SELECT b.book_id, b.title, b.isbn, b.price,
                 b.stock_quantity, b.description,
                 c.category_id, c.name AS category_name,
                 c.description AS category_description
          FROM books b
          JOIN categories c ON b.category_id = c.category_id
          WHERE b.price <= ?
          ORDER BY b.price, b.title
          """;

    private static final String SQL_FIND_IN_STOCK =
          """
          SELECT b.book_id, b.title, b.isbn, b.price,
                 b.stock_quantity, b.description,
                 c.category_id, c.name AS category_name,
                 c.description AS category_description
          FROM books b
          JOIN categories c ON b.category_id = c.category_id
          WHERE b.stock_quantity > 0
          ORDER BY b.title
          """;

    /**
     * Конструктор з ін'єкцією залежностей.
     *
     * @param connectionManager менеджер пулу з'єднань
     * @param authorRepository репозиторій авторів для Lazy Loading
     */
    public JdbcBookRepository(
          ConnectionManager connectionManager, JdbcAuthorRepository authorRepository) {
        super(connectionManager);
        this.authorRepository = authorRepository;
    }

    // =========================================================
    // Реалізація абстрактних методів
    // =========================================================

    /**
     * Data Mapper: ResultSet → Book з вкладеною Category та LazyList авторів.
     *
     * <p>Автори НЕ завантажуються тут — лише реєструється функція завантаження. SELECT для авторів
     * виконається лише при першому виклику {@code book.getAuthors()}.
     */
    @Override
    protected Book mapRow(ResultSet rs) throws SQLException {
        UUID bookId = UUID.fromString(rs.getString("book_id"));

        Category category =
              Category.builder()
                    .id(UUID.fromString(rs.getString("category_id")))
                    .name(rs.getString("category_name"))
                    .description(rs.getString("category_description"))
                    .build();

        return Book.builder()
              .id(bookId)
              .category(category)
              .title(rs.getString("title"))
              .isbn(rs.getString("isbn"))
              .price(rs.getBigDecimal("price"))
              .stockQuantity(rs.getInt("stock_quantity"))
              .description(rs.getString("description"))
              // LazyList: автори завантажуються лише при першому book.getAuthors()
              .authors(new LazyList<>(() -> authorRepository.findByBookId(bookId)))
              .build();
    }

    @Override
    protected String getTableName() {
        return "books";
    }

    @Override
    protected String getIdColumnName() {
        return "book_id";
    }

    @Override
    protected String getSelectByIdSql() {
        return SQL_SELECT_BY_ID;
    }

    @Override
    protected String getSelectAllSql() {
        return SQL_SELECT_ALL;
    }

    @Override
    protected String getInsertSql() {
        return SQL_INSERT;
    }

    @Override
    protected String getUpdateSql() {
        return SQL_UPDATE;
    }

    @Override
    protected void setInsertParams(PreparedStatement stmt, Book book) throws SQLException {
        stmt.setString(1, book.getId().toString());
        stmt.setString(2, book.getCategory().getId().toString());
        stmt.setString(3, book.getTitle());
        stmt.setString(4, book.getIsbn());
        stmt.setBigDecimal(5, book.getPrice());
        stmt.setInt(6, book.getStockQuantity());
        stmt.setString(7, book.getDescription());
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, Book book) throws SQLException {
        stmt.setString(1, book.getCategory().getId().toString());
        stmt.setString(2, book.getTitle());
        stmt.setString(3, book.getIsbn());
        stmt.setBigDecimal(4, book.getPrice());
        stmt.setInt(5, book.getStockQuantity());
        stmt.setString(6, book.getDescription());
        stmt.setString(7, book.getId().toString());
    }

    @Override
    protected UUID getId(Book book) {
        return book.getId();
    }

    // =========================================================
    // Специфічні методи
    // =========================================================

    /**
     * Знаходить книги за частиною назви.
     *
     * @param titlePart підрядок назви
     * @return список книг
     */
    public List<Book> findByTitle(String titlePart) {
        List<Book> list = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_TITLE)) {
            stmt.setString(1, "%" + titlePart + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Помилка findByTitle: " + titlePart, e);
        }
        return list;
    }

    /**
     * Знаходить книгу за ISBN.
     *
     * @param isbn ISBN книги
     * @return Optional зі знайденою книгою
     */
    public Optional<Book> findByIsbn(String isbn) {
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ISBN)) {
            stmt.setString(1, isbn);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Помилка findByIsbn: " + isbn, e);
        }
    }

    /**
     * Знаходить книги за категорією.
     *
     * @param categoryId UUID категорії
     * @return список книг
     */
    public List<Book> findByCategoryId(UUID categoryId) {
        List<Book> list = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_CATEGORY)) {
            stmt.setString(1, categoryId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Помилка findByCategoryId: " + categoryId, e);
        }
        return list;
    }

    /**
     * Знаходить книги з ціною не вище заданої.
     *
     * @param maxPrice максимальна ціна
     * @return список книг
     */
    public List<Book> findByMaxPrice(BigDecimal maxPrice) {
        List<Book> list = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_MAX_PRICE)) {
            stmt.setBigDecimal(1, maxPrice);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Помилка findByMaxPrice: " + maxPrice, e);
        }
        return list;
    }

    /**
     * Знаходить книги що є в наявності.
     *
     * @return список книг з stock_quantity > 0
     */
    public List<Book> findInStock() {
        List<Book> list = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_IN_STOCK);
              ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Помилка findInStock", e);
        }
        return list;
    }
}