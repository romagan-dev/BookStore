package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.domain.entity.Author;
import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-реалізація репозиторію для сутності {@link Author}.
 *
 * <p>Реалізує лише специфічні для авторів частини. Стандартні CRUD успадковані від {@link
 * AbstractJdbcRepository}.
 */
public class JdbcAuthorRepository extends AbstractJdbcRepository<Author, UUID> {
  private static final String SQL_SELECT_BY_ID =
      """
      SELECT author_id, first_name, last_name
      FROM authors
      WHERE author_id = ?
      """;

  private static final String SQL_SELECT_ALL =
      """
      SELECT author_id, first_name, last_name
      FROM authors
      ORDER BY last_name, first_name
      """;

  private static final String SQL_INSERT =
      """
      INSERT INTO authors (author_id, first_name, last_name)
      VALUES (?, ?, ?)
      """;

  private static final String SQL_UPDATE =
      """
      UPDATE authors
      SET first_name = ?, last_name = ?
      WHERE author_id = ?
      """;

  private static final String SQL_FIND_BY_LAST_NAME =
      """
      SELECT author_id, first_name, last_name
      FROM authors
      WHERE LOWER(last_name) LIKE LOWER(?)
      ORDER BY last_name, first_name
      """;

  private static final String SQL_FIND_BY_FULL_NAME =
      """
      SELECT author_id, first_name, last_name
      FROM authors
      WHERE last_name = ? AND first_name = ?
      """;

  private static final String SQL_FIND_BY_BOOK =
      """
      SELECT a.author_id, a.first_name, a.last_name
      FROM authors a
      JOIN book_authors ba ON a.author_id = ba.author_id
      WHERE ba.book_id = ?
      """;

  public JdbcAuthorRepository(ConnectionManager connectionManager) {
    super(connectionManager);
  }

  @Override
  protected Author mapRow(ResultSet rs) throws SQLException {
    return Author.builder()
        .id(UUID.fromString(rs.getString("author_id")))
        .firstName(rs.getString("first_name"))
        .lastName(rs.getString("last_name"))
        .build();
  }

  @Override
  protected String getTableName() {
    return "authors";
  }

  @Override
  protected String getIdColumnName() {
    return "author_id";
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
  protected void setInsertParams(PreparedStatement stmt, Author author) throws SQLException {
    stmt.setString(1, author.getId().toString());
    stmt.setString(2, author.getFirstName());
    stmt.setString(3, author.getLastName());
  }

  @Override
  protected void setUpdateParams(PreparedStatement stmt, Author author) throws SQLException {
    stmt.setString(1, author.getFirstName());
    stmt.setString(2, author.getLastName());
    stmt.setString(3, author.getId().toString());
  }

  @Override
  protected UUID getId(Author author) {
    return author.getId();
  }

  /**
   * Знаходить авторів за частиною прізвища (нечутливий до регістру).
   *
   * @param lastNamePart підрядок прізвища
   * @return список знайдених авторів
   */
  public List<Author> findByLastName(String lastNamePart) {
    List<Author> list = new ArrayList<>();

    try (Connection conn = connectionManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_LAST_NAME)) {

      stmt.setString(1, "%" + lastNamePart + "%");
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          list.add(mapRow(rs));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Помилка findByLastName для: " + lastNamePart, e);
    }
    return list;
  }

  /**
   * Знаходить автора за точним прізвищем та ім'ям.
   *
   * @param lastName прізвище
   * @param firstName ім'я
   * @return Optional зі знайденим автором
   */
  public Optional<Author> findByFullName(String lastName, String firstName) {
    try (Connection conn = connectionManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_FULL_NAME)) {

      stmt.setString(1, lastName);
      stmt.setString(2, firstName);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
      }

    } catch (SQLException e) {
      throw new DatabaseException("Помилка findByFullName для: " + lastName + " " + firstName, e);
    }
  }

  /**
   * Знаходить всіх авторів конкретної книги (N:M через book_authors).
   *
   * @param bookId UUID книги
   * @return список авторів
   */
  public List<Author> findByBookId(UUID bookId) {
    List<Author> list = new ArrayList<>();

    try (Connection conn = connectionManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_BOOK)) {

      stmt.setString(1, bookId.toString());
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          list.add(mapRow(rs));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Помилка findByBookId для: " + bookId, e);
    }
    return list;
  }
}
