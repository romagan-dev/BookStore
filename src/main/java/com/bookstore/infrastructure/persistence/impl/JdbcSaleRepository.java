package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.domain.entity.Client;
import com.bookstore.domain.entity.Sale;
import com.bookstore.domain.entity.User;
import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC-реалізація репозиторію для сутності {@link Sale}.
 *
 * <p>Читаючі операції використовують JOIN для завантаження пов'язаних User та Client. Стандартні
 * CRUD успадковані від {@link AbstractJdbcRepository}.
 */
public class JdbcSaleRepository extends AbstractJdbcRepository<Sale, UUID> {

    private static final String SQL_BASE_SELECT =
          """
          SELECT s.sale_id, s.sale_date, s.total_amount, s.payment_method,
                 u.user_id, u.username, u.password_hash, u.email, u.role,
                 c.client_id, c.first_name, c.last_name, c.phone, c.email AS client_email
          FROM sales s
          JOIN users u ON s.user_id = u.user_id
          LEFT JOIN clients c ON s.client_id = c.client_id
          """;

    private static final String SQL_SELECT_BY_ID =
          SQL_BASE_SELECT + "WHERE s.sale_id = ?";

    private static final String SQL_SELECT_ALL =
          SQL_BASE_SELECT + "ORDER BY s.sale_date DESC";

    private static final String SQL_INSERT =
          """
          INSERT INTO sales (sale_id, user_id, client_id, sale_date, total_amount, payment_method)
          VALUES (?, ?, ?, ?, ?, ?)
          """;

    private static final String SQL_UPDATE =
          """
          UPDATE sales
          SET user_id = ?, client_id = ?, sale_date = ?, total_amount = ?, payment_method = ?
          WHERE sale_id = ?
          """;

    private static final String SQL_FIND_BY_CLIENT =
          SQL_BASE_SELECT + "WHERE s.client_id = ? ORDER BY s.sale_date DESC";

    private static final String SQL_FIND_BY_USER =
          SQL_BASE_SELECT + "WHERE s.user_id = ? ORDER BY s.sale_date DESC";

    public JdbcSaleRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    /**
     * Data Mapper: ResultSet → Sale з вкладеними User та Client.
     *
     * <p>Client може бути null (анонімна покупка) — використовуємо LEFT JOIN та перевіряємо
     * client_id на null.
     */
    @Override
    protected Sale mapRow(ResultSet rs) throws SQLException {
        User user =
              User.builder()
                    .id(UUID.fromString(rs.getString("user_id")))
                    .username(rs.getString("username"))
                    .passwordHash(rs.getString("password_hash"))
                    .email(rs.getString("email"))
                    .role(User.Role.valueOf(rs.getString("role")))
                    .build();

        // client_id може бути NULL (анонімна покупка)
        String clientIdStr = rs.getString("client_id");
        Client client = null;
        if (clientIdStr != null) {
            client =
                  Client.builder()
                        .id(UUID.fromString(clientIdStr))
                        .firstName(rs.getString("first_name"))
                        .lastName(rs.getString("last_name"))
                        .phone(rs.getString("phone"))
                        .email(rs.getString("client_email"))
                        .build();
        }

        return Sale.builder()
              .id(UUID.fromString(rs.getString("sale_id")))
              .user(user)
              .client(client) // може бути null
              .saleDate(rs.getObject("sale_date", LocalDateTime.class))
              .totalAmount(rs.getBigDecimal("total_amount"))
              .paymentMethod(Sale.PaymentMethod.valueOf(rs.getString("payment_method")))
              .items(new ArrayList<>()) // позиції завантажуються окремо (Lazy)
              .build();
    }

    @Override
    protected String getTableName() {
        return "sales";
    }

    @Override
    protected String getIdColumnName() {
        return "sale_id";
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
    protected void setInsertParams(PreparedStatement stmt, Sale sale) throws SQLException {
        stmt.setString(1, sale.getId().toString());
        stmt.setString(2, sale.getUser().getId().toString());
        // client може бути null — анонімна покупка
        stmt.setString(3, sale.getClient() != null ? sale.getClient().getId().toString() : null);
        stmt.setObject(4, sale.getSaleDate());
        stmt.setBigDecimal(5, sale.getTotalAmount());
        stmt.setString(6, sale.getPaymentMethod().name());
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, Sale sale) throws SQLException {
        stmt.setString(1, sale.getUser().getId().toString());
        stmt.setString(2, sale.getClient() != null ? sale.getClient().getId().toString() : null);
        stmt.setObject(3, sale.getSaleDate());
        stmt.setBigDecimal(4, sale.getTotalAmount());
        stmt.setString(5, sale.getPaymentMethod().name());
        stmt.setString(6, sale.getId().toString());
    }

    @Override
    protected UUID getId(Sale sale) {
        return sale.getId();
    }

    /**
     * Знаходить всі покупки конкретного клієнта.
     *
     * @param clientId UUID клієнта
     * @return список покупок
     */
    public List<Sale> findByClientId(UUID clientId) {
        List<Sale> list = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_CLIENT)) {

            stmt.setString(1, clientId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка findByClientId для: " + clientId, e);
        }
        return list;
    }

    /**
     * Знаходить всі покупки оформлені конкретним касиром.
     *
     * @param userId UUID касира
     * @return список покупок
     */
    public List<Sale> findByUserId(UUID userId) {
        List<Sale> list = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USER)) {

            stmt.setString(1, userId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка findByUserId для: " + userId, e);
        }
        return list;
    }
}