package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.domain.entity.Client;
import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-реалізація репозиторію для сутності {@link Client}.
 *
 * <p>Реалізує лише специфічні для клієнтів частини. Стандартні CRUD успадковані від {@link
 * AbstractJdbcRepository}.
 */
public class JdbcClientRepository extends AbstractJdbcRepository<Client, UUID> {

    private static final String SQL_SELECT_BY_ID =
          """
          SELECT client_id, first_name, last_name, phone, email
          FROM clients
          WHERE client_id = ?
          """;

    private static final String SQL_SELECT_ALL =
          """
          SELECT client_id, first_name, last_name, phone, email
          FROM clients
          ORDER BY last_name, first_name
          """;

    private static final String SQL_INSERT =
          """
          INSERT INTO clients (client_id, first_name, last_name, phone, email)
          VALUES (?, ?, ?, ?, ?)
          """;

    private static final String SQL_UPDATE =
          """
          UPDATE clients
          SET first_name = ?, last_name = ?, phone = ?, email = ?
          WHERE client_id = ?
          """;

    private static final String SQL_FIND_BY_PHONE =
          """
          SELECT client_id, first_name, last_name, phone, email
          FROM clients
          WHERE phone = ?
          """;

    private static final String SQL_FIND_BY_EMAIL =
          """
          SELECT client_id, first_name, last_name, phone, email
          FROM clients
          WHERE email = ?
          """;

    public JdbcClientRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    protected Client mapRow(ResultSet rs) throws SQLException {
        return Client.builder()
              .id(UUID.fromString(rs.getString("client_id")))
              .firstName(rs.getString("first_name"))
              .lastName(rs.getString("last_name"))
              .phone(rs.getString("phone"))
              .email(rs.getString("email"))
              .build();
    }

    @Override
    protected String getTableName() {
        return "clients";
    }

    @Override
    protected String getIdColumnName() {
        return "client_id";
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
    protected void setInsertParams(PreparedStatement stmt, Client client) throws SQLException {
        stmt.setString(1, client.getId().toString());
        stmt.setString(2, client.getFirstName());
        stmt.setString(3, client.getLastName());
        stmt.setString(4, client.getPhone());
        stmt.setString(5, client.getEmail());
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, Client client) throws SQLException {
        stmt.setString(1, client.getFirstName());
        stmt.setString(2, client.getLastName());
        stmt.setString(3, client.getPhone());
        stmt.setString(4, client.getEmail());
        stmt.setString(5, client.getId().toString());
    }

    @Override
    protected UUID getId(Client client) {
        return client.getId();
    }

    /**
     * Знаходить клієнта за номером телефону.
     *
     * @param phone номер телефону
     * @return Optional зі знайденим клієнтом
     */
    public Optional<Client> findByPhone(String phone) {
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_PHONE)) {

            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка findByPhone для: " + phone, e);
        }
    }

    /**
     * Знаходить клієнта за email.
     *
     * @param email email адреса
     * @return Optional зі знайденим клієнтом
     */
    public Optional<Client> findByEmail(String email) {
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_EMAIL)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка findByEmail для: " + email, e);
        }
    }
}