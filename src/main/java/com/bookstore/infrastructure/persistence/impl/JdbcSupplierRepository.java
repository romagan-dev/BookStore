package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.domain.entity.Supplier;
import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC-реалізація репозиторію для сутності {@link Supplier}.
 *
 * <p>Реалізує лише специфічні для постачальників частини. Стандартні CRUD успадковані від {@link
 * AbstractJdbcRepository}.
 */
public class JdbcSupplierRepository extends AbstractJdbcRepository<Supplier, UUID> {

  private static final String SQL_SELECT_BY_ID =
      """
      SELECT supplier_id, company_name, phone, email, address
      FROM suppliers
      WHERE supplier_id = ?
      """;

  private static final String SQL_SELECT_ALL =
      """
      SELECT supplier_id, company_name, phone, email, address
      FROM suppliers
      ORDER BY company_name
      """;

  private static final String SQL_INSERT =
      """
      INSERT INTO suppliers (supplier_id, company_name, phone, email, address)
      VALUES (?, ?, ?, ?, ?)
      """;

  private static final String SQL_UPDATE =
      """
      UPDATE suppliers
      SET company_name = ?, phone = ?, email = ?, address = ?
      WHERE supplier_id = ?
      """;

  private static final String SQL_FIND_BY_NAME =
      """
      SELECT supplier_id, company_name, phone, email, address
      FROM suppliers
      WHERE LOWER(company_name) LIKE LOWER(?)
      ORDER BY company_name
      """;

  public JdbcSupplierRepository(ConnectionManager connectionManager) {
    super(connectionManager);
  }

  @Override
  protected Supplier mapRow(ResultSet rs) throws SQLException {
    return Supplier.builder()
        .id(UUID.fromString(rs.getString("supplier_id")))
        .companyName(rs.getString("company_name"))
        .phone(rs.getString("phone"))
        .email(rs.getString("email"))
        .address(rs.getString("address"))
        .build();
  }

  @Override
  protected String getTableName() {
    return "suppliers";
  }

  @Override
  protected String getIdColumnName() {
    return "supplier_id";
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
  protected void setInsertParams(PreparedStatement stmt, Supplier supplier) throws SQLException {
    stmt.setString(1, supplier.getId().toString());
    stmt.setString(2, supplier.getCompanyName());
    stmt.setString(3, supplier.getPhone());
    stmt.setString(4, supplier.getEmail());
    stmt.setString(5, supplier.getAddress());
  }

  @Override
  protected void setUpdateParams(PreparedStatement stmt, Supplier supplier) throws SQLException {
    stmt.setString(1, supplier.getCompanyName());
    stmt.setString(2, supplier.getPhone());
    stmt.setString(3, supplier.getEmail());
    stmt.setString(4, supplier.getAddress());
    stmt.setString(5, supplier.getId().toString());
  }

  @Override
  protected UUID getId(Supplier supplier) {
    return supplier.getId();
  }

  /**
   * Знаходить постачальників за частиною назви компанії.
   *
   * @param namePart підрядок назви
   * @return список знайдених постачальників
   */
  public List<Supplier> findByName(String namePart) {
    List<Supplier> list = new ArrayList<>();

    try (Connection conn = connectionManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_NAME)) {

      stmt.setString(1, "%" + namePart + "%");
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          list.add(mapRow(rs));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Помилка findByName для: " + namePart, e);
    }
    return list;
  }
}
