package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.domain.entity.User;
import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-реалізація репозиторію для сутності {@link User}.
 *
 * <p>Реалізує лише специфічні для користувачів частини. Стандартні CRUD успадковані від {@link
 * AbstractJdbcRepository}.
 */
public class JdbcUserRepository extends AbstractJdbcRepository<User, UUID> {

    private static final String SQL_SELECT_BY_ID =
          """
          SELECT user_id, username, password_hash, email, role
          FROM users
          WHERE user_id = ?
          """;

    private static final String SQL_SELECT_ALL =
          """
          SELECT user_id, username, password_hash, email, role
          FROM users
          ORDER BY username
          """;

    private static final String SQL_INSERT =
          """
          INSERT INTO users (user_id, username, password_hash, email, role)
          VALUES (?, ?, ?, ?, ?)
          """;

    private static final String SQL_UPDATE =
          """
          UPDATE users
          SET username = ?, password_hash = ?, email = ?, role = ?
          WHERE user_id = ?
          """;

    private static final String SQL_FIND_BY_USERNAME =
          """
          SELECT user_id, username, password_hash, email, role
          FROM users
          WHERE username = ?
          """;

    private static final String SQL_FIND_BY_EMAIL =
          """
          SELECT user_id, username, password_hash, email, role
          FROM users
          WHERE email = ?
          """;

    public JdbcUserRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        return User.builder()
              .id(UUID.fromString(rs.getString("user_id")))
              .username(rs.getString("username"))
              .passwordHash(rs.getString("password_hash"))
              .email(rs.getString("email"))
              .role(User.Role.valueOf(rs.getString("role")))
              .build();
    }

    @Override
    protected String getTableName() {
        return "users";
    }

    @Override
    protected String getIdColumnName() {
        return "user_id";
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
    protected void setInsertParams(PreparedStatement stmt, User user) throws SQLException {
        stmt.setString(1, user.getId().toString());
        stmt.setString(2, user.getUsername());
        stmt.setString(3, user.getPasswordHash());
        stmt.setString(4, user.getEmail());
        stmt.setString(5, user.getRole().name());
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, User user) throws SQLException {
        stmt.setString(1, user.getUsername());
        stmt.setString(2, user.getPasswordHash());
        stmt.setString(3, user.getEmail());
        stmt.setString(4, user.getRole().name());
        stmt.setString(5, user.getId().toString());
    }

    @Override
    protected UUID getId(User user) {
        return user.getId();
    }

    /**
     * Знаходить користувача за логіном. Використовується при аутентифікації.
     *
     * @param username логін
     * @return Optional зі знайденим користувачем
     */
    public Optional<User> findByUsername(String username) {
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка findByUsername для: " + username, e);
        }
    }

    /**
     * Знаходить користувача за email.
     *
     * @param email email адреса
     * @return Optional зі знайденим користувачем
     */
    public Optional<User> findByEmail(String email) {
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