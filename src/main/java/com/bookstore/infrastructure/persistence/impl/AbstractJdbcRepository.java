package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Абстрактний базовий клас для JDBC-репозиторіїв.
 *
 * <p>Реалізує патерн Template Method: стандартні CRUD-операції визначені у цьому класі,
 * варіативні частини (SQL, маппінг, встановлення параметрів) делегуються підкласам через
 * abstract-методи.
 *
 * <p>Підкласи зобов'язані реалізувати:
 *
 * <ul>
 *   <li>{@link #mapRow(ResultSet)} — перетворення рядка ResultSet у доменний об'єкт
 *   <li>{@link #getTableName()} — назва таблиці для SELECT COUNT та DELETE
 *   <li>{@link #getIdColumnName()} — назва колонки первинного ключа
 *   <li>{@link #getSelectByIdSql()} — SQL для пошуку за id
 *   <li>{@link #getSelectAllSql()} — SQL для вибірки всіх записів
 *   <li>{@link #getInsertSql()} — SQL для INSERT
 *   <li>{@link #getUpdateSql()} — SQL для UPDATE
 *   <li>{@link #setInsertParams(PreparedStatement, T)} — параметри для INSERT
 *   <li>{@link #setUpdateParams(PreparedStatement, T)} — параметри для UPDATE
 *   <li>{@link #getId(T)} — отримати ID з сутності
 * </ul>
 *
 * @param <T> тип доменної сутності
 * @param <ID> тип первинного ключа (UUID)
 */
public abstract class AbstractJdbcRepository<T, ID> {

    /**
     * Менеджер з'єднань — єдина залежність базового класу від інфраструктури. {@code protected}
     * дозволяє підкласам звертатися напряму для специфічних запитів.
     */
    protected final ConnectionManager connectionManager;

    /**
     * Конструктор з ін'єкцією залежності ConnectionManager.
     *
     * @param connectionManager менеджер пулу з'єднань
     */
    protected AbstractJdbcRepository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    // =========================================================
    // Абстрактні методи — підкласи зобов'язані їх реалізувати
    // =========================================================

    /**
     * Перетворює поточний рядок ResultSet у доменний об'єкт типу T. Реалізація патерну Data
     * Mapper.
     *
     * @param rs ResultSet, що вже позиціонований на рядку (rs.next() = true)
     * @return доменний об'єкт
     */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    /**
     * Назва таблиці у БД (наприклад, "books", "authors").
     *
     * @return назва таблиці
     */
    protected abstract String getTableName();

    /**
     * Назва колонки первинного ключа (наприклад, "book_id", "author_id").
     *
     * @return назва колонки PK
     */
    protected abstract String getIdColumnName();

    /**
     * SQL-запит для пошуку одного запису за первинним ключем.
     *
     * @return SQL рядок
     */
    protected abstract String getSelectByIdSql();

    /**
     * SQL-запит для вибірки всіх записів.
     *
     * @return SQL рядок
     */
    protected abstract String getSelectAllSql();

    /**
     * SQL-запит для вставки нового запису.
     *
     * @return SQL рядок
     */
    protected abstract String getInsertSql();

    /**
     * SQL-запит для оновлення існуючого запису.
     *
     * @return SQL рядок
     */
    protected abstract String getUpdateSql();

    /**
     * Встановлює параметри PreparedStatement для INSERT. Data Mapper: Object → PreparedStatement.
     *
     * @param stmt підготовлений запит
     * @param entity сутність з якої беруться значення
     */
    protected abstract void setInsertParams(PreparedStatement stmt, T entity) throws SQLException;

    /**
     * Встановлює параметри PreparedStatement для UPDATE. ID передається останнім параметром (для
     * WHERE).
     *
     * @param stmt підготовлений запит
     * @param entity сутність з оновленими даними
     */
    protected abstract void setUpdateParams(PreparedStatement stmt, T entity) throws SQLException;

    /**
     * Повертає первинний ключ сутності. Використовується у deleteById та existsById.
     *
     * @param entity сутність
     * @return первинний ключ типу ID
     */
    protected abstract ID getId(T entity);

    // =========================================================
    // Конкретні реалізації Template Methods
    // =========================================================

    /**
     * Знаходить сутність за первинним ключем.
     *
     * <p>Template Method: варіативні частини — {@link #getSelectByIdSql()} та {@link
     * #mapRow(ResultSet)}.
     *
     * @param id первинний ключ
     * @return Optional зі знайденою сутністю або empty
     */
    public Optional<T> findById(ID id) {
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(getSelectByIdSql())) {

            stmt.setObject(1, id.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                  "Помилка findById у таблиці " + getTableName() + " для id=" + id, e);
        }
    }

    /**
     * Повертає всі сутності.
     *
     * <p>Template Method: варіативна частина — {@link #getSelectAllSql()}.
     *
     * @return список всіх сутностей
     */
    public List<T> findAll() {
        List<T> list = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(getSelectAllSql());
              ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка findAll у таблиці " + getTableName(), e);
        }
        return list;
    }

    /**
     * Зберігає нову сутність.
     *
     * <p>Template Method: варіативні частини — {@link #getInsertSql()} та {@link
     * #setInsertParams(PreparedStatement, T)}.
     *
     * @param entity сутність для збереження
     */
    public void save(T entity) {
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(getInsertSql())) {

            setInsertParams(stmt, entity);

            int rows = stmt.executeUpdate();
            if (rows != 1) {
                throw new DatabaseException(
                      "Очікувався 1 вставлений рядок у "
                            + getTableName()
                            + ", отримано: "
                            + rows);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка save у таблиці " + getTableName(), e);
        }
    }

    /**
     * Оновлює існуючу сутність.
     *
     * <p>Template Method: варіативні частини — {@link #getUpdateSql()} та {@link
     * #setUpdateParams(PreparedStatement, T)}.
     *
     * @param entity сутність з оновленими даними
     */
    public void update(T entity) {
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(getUpdateSql())) {

            setUpdateParams(stmt, entity);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException(
                      "Сутність не знайдена у "
                            + getTableName()
                            + " для оновлення: id="
                            + getId(entity));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка update у таблиці " + getTableName(), e);
        }
    }

    /**
     * Видаляє сутність за первинним ключем. SQL генерується через {@link #getTableName()} та
     * {@link #getIdColumnName()} — однаковий для всіх таблиць.
     *
     * @param id первинний ключ
     * @return true якщо сутність існувала і була видалена
     */
    public boolean deleteById(ID id) {
        String sql =
              "DELETE FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";

        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id.toString());
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException(
                  "Помилка deleteById у таблиці " + getTableName() + " для id=" + id, e);
        }
    }

    /**
     * Повертає кількість записів у таблиці. SQL генерується через {@link #getTableName()}.
     *
     * @return кількість записів
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + getTableName();

        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql);
              ResultSet rs = stmt.executeQuery()) {

            rs.next();
            return rs.getLong(1);

        } catch (SQLException e) {
            throw new DatabaseException("Помилка count() у таблиці " + getTableName(), e);
        }
    }

    /**
     * Перевіряє існування запису за id без завантаження даних. {@code SELECT 1} — найефективніший
     * спосіб перевірки існування.
     *
     * @param id первинний ключ
     * @return true якщо сутність існує
     */
    public boolean existsById(ID id) {
        String sql =
              "SELECT 1 FROM "
                    + getTableName()
                    + " WHERE "
                    + getIdColumnName()
                    + " = ? LIMIT 1";

        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                  "Помилка existsById у таблиці " + getTableName() + " для id=" + id, e);
        }
    }
}