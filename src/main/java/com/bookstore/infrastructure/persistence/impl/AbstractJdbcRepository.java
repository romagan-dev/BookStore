package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import com.bookstore.infrastructure.persistence.util.IdentityMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Абстрактний базовий клас для JDBC-репозиторіїв з підтримкою Identity Map.
 *
 * <p>Реалізує патерн Template Method: стандартні CRUD-операції визначені у цьому класі, варіативні
 * частини делегуються підкласам через abstract-методи.
 *
 * <p>Інтеграція Identity Map: {@link #findById(UUID)} спочатку перевіряє кеш, потім БД. Після
 * завантаження з БД — автоматично кешує. {@link #deleteById(UUID)} — видаляє з кешу.
 *
 * @param <T> тип доменної сутності
 * @param <ID> тип первинного ключа (UUID)
 */
public abstract class AbstractJdbcRepository<T, ID> {

    /** Менеджер з'єднань — єдина залежність від інфраструктури. */
    protected final ConnectionManager connectionManager;

    /**
     * Identity Map — кеш завантажених сутностей у рамках сесії. Один екземпляр на репозиторій.
     */
    protected final IdentityMap<T> identityMap = new IdentityMap<>();

    /**
     * Конструктор з ін'єкцією залежності.
     *
     * @param connectionManager менеджер пулу з'єднань
     */
    protected AbstractJdbcRepository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    // =========================================================
    // Абстрактні методи — підкласи зобов'язані їх реалізувати
    // =========================================================

    /** Перетворює поточний рядок ResultSet у доменний об'єкт. */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    /** Назва таблиці у БД. */
    protected abstract String getTableName();

    /** Назва колонки первинного ключа. */
    protected abstract String getIdColumnName();

    /** SQL для пошуку за id. */
    protected abstract String getSelectByIdSql();

    /** SQL для вибірки всіх записів. */
    protected abstract String getSelectAllSql();

    /** SQL для INSERT. */
    protected abstract String getInsertSql();

    /** SQL для UPDATE. */
    protected abstract String getUpdateSql();

    /** Встановлює параметри PreparedStatement для INSERT. */
    protected abstract void setInsertParams(PreparedStatement stmt, T entity) throws SQLException;

    /**
     * Встановлює параметри PreparedStatement для UPDATE. ID — останній параметр (для WHERE).
     */
    protected abstract void setUpdateParams(PreparedStatement stmt, T entity) throws SQLException;

    /** Повертає UUID сутності. */
    protected abstract UUID getId(T entity);

    // =========================================================
    // Template Methods з інтеграцією Identity Map
    // =========================================================

    /**
     * Знаходить сутність за UUID.
     *
     * <p>Алгоритм: 1) перевіряємо Identity Map → якщо є, повертаємо без SELECT 2) виконуємо
     * SELECT з БД 3) кешуємо результат в Identity Map 4) повертаємо результат
     *
     * @param id UUID сутності
     * @return Optional зі знайденою сутністю
     */
    public Optional<T> findById(UUID id) {
        // 1. Перевіряємо кеш — уникаємо зайвого SELECT
        Optional<T> cached = identityMap.get(id);
        if (cached.isPresent()) {
            return cached;
        }

        // 2. Завантажуємо з БД
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(getSelectByIdSql())) {

            stmt.setObject(1, id.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    T entity = mapRow(rs);
                    // 3. Кешуємо для майбутніх запитів
                    identityMap.put(id, entity);
                    return Optional.of(entity);
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                  "Помилка findById у таблиці " + getTableName() + " для id=" + id, e);
        }
    }

    /**
     * Повертає ConnectionManager для використання у сервісному шарі.
     *
     * @return менеджер з'єднань
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * Повертає всі сутності та кешує кожну.
     *
     * @return список всіх сутностей
     */
    public List<T> findAll() {
        List<T> list = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(getSelectAllSql());
              ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                T entity = mapRow(rs);
                // Кешуємо кожну завантажену сутність
                identityMap.put(getId(entity), entity);
                list.add(entity);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка findAll у таблиці " + getTableName(), e);
        }
        return list;
    }

    /**
     * Зберігає нову сутність та кешує її.
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
                      "Очікувався 1 рядок у " + getTableName() + ", отримано: " + rows);
            }

            // Кешуємо після успішного INSERT
            identityMap.put(getId(entity), entity);

        } catch (SQLException e) {
            throw new DatabaseException("Помилка save у таблиці " + getTableName(), e);
        }
    }

    /**
     * Оновлює існуючу сутність та оновлює кеш.
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
                            + " для id="
                            + getId(entity));
            }

            // Оновлюємо кеш актуальним об'єктом
            identityMap.put(getId(entity), entity);

        } catch (SQLException e) {
            throw new DatabaseException("Помилка update у таблиці " + getTableName(), e);
        }
    }

    /**
     * Видаляє сутність та прибирає з кешу.
     *
     * @param id UUID для видалення
     * @return true якщо сутність існувала
     */
    public boolean deleteById(UUID id) {
        String sql = "DELETE FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";

        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id.toString());
            boolean deleted = stmt.executeUpdate() > 0;

            // Видаляємо з кешу незалежно від результату
            if (deleted) {
                identityMap.remove(id);
            }

            return deleted;

        } catch (SQLException e) {
            throw new DatabaseException(
                  "Помилка deleteById у таблиці " + getTableName() + " для id=" + id, e);
        }
    }

    /**
     * Повертає кількість записів у таблиці.
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
     * Перевіряє існування запису.
     *
     * <p>Спочатку перевіряє кеш — якщо є в Identity Map, точно існує в БД.
     *
     * @param id UUID для перевірки
     * @return true якщо сутність існує
     */
    public boolean existsById(UUID id) {
        // Якщо є в кеші — точно існує
        if (identityMap.contains(id)) {
            return true;
        }

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

    /**
     * Очищає Identity Map кеш.
     *
     * <p>Викликати після завершення бізнес-операції або при потребі перезавантажити дані з БД.
     */
    public void clearCache() {
        identityMap.clear();
    }
}