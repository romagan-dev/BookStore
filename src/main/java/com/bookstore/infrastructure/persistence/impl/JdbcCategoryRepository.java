package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.domain.entity.Category;
import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-реалізація репозиторію для сутності {@link Category}.
 *
 * <p>Реалізує лише специфічні для категорій частини. Стандартні CRUD успадковані від {@link
 * AbstractJdbcRepository}.
 */
public class JdbcCategoryRepository extends AbstractJdbcRepository<Category, UUID> {

  private static final String SQL_SELECT_BY_ID =
      """
      SELECT category_id, name, description
      FROM categories
      WHERE category_id = ?
      """;

  private static final String SQL_SELECT_ALL =
      """
      SELECT category_id, name, description
      FROM categories
      ORDER BY name
      """;

  private static final String SQL_INSERT =
      """
      INSERT INTO categories (category_id, name, description)
      VALUES (?, ?, ?)
      """;

  private static final String SQL_UPDATE =
      """
      UPDATE categories
      SET name = ?, description = ?
      WHERE category_id = ?
      """;

  private static final String SQL_FIND_BY_NAME =
      """
      SELECT category_id, name, description
      FROM categories
      WHERE name = ?
      """;

  public JdbcCategoryRepository(ConnectionManager connectionManager) {
    super(connectionManager);
  }

  @Override
  protected Category mapRow(ResultSet rs) throws SQLException {
    return Category.builder()
        .id(UUID.fromString(rs.getString("category_id")))
        .name(rs.getString("name"))
        .description(rs.getString("description"))
        .build();
  }

  @Override
  protected String getTableName() {
    return "categories";
  }

  @Override
  protected String getIdColumnName() {
    return "category_id";
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
  protected void setInsertParams(PreparedStatement stmt, Category category) throws SQLException {
    stmt.setString(1, category.getId().toString());
    stmt.setString(2, category.getName());
    stmt.setString(3, category.getDescription());
  }

  @Override
  protected void setUpdateParams(PreparedStatement stmt, Category category) throws SQLException {
    stmt.setString(1, category.getName());
    stmt.setString(2, category.getDescription());
    stmt.setString(3, category.getId().toString());
  }

  @Override
  protected UUID getId(Category category) {
    return category.getId();
  }

  /**
   * Знаходить категорію за точною назвою.
   *
   * @param name назва категорії
   * @return Optional зі знайденою категорією
   */
  public Optional<Category> findByName(String name) {
    try (Connection conn = connectionManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_NAME)) {

      stmt.setString(1, name);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
      }

    } catch (SQLException e) {
      throw new DatabaseException("Помилка findByName для категорії: " + name, e);
    }
  }
}
