package com.bookstore.infrastructure.persistence.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Реалізація патерну Unit of Work.
 *
 * <p>Координує всі зміни у рамках однієї бізнес-транзакції: накопичує операції
 * INSERT, UPDATE, DELETE і виконує їх всі разом в одній JDBC-транзакції при виклику {@link #commit()}.
 *
 * <p>Три категорії відстежуваних об'єктів (за Фаулером):
 *
 * <ul>
 *   <li>{@code newEntities} — нові об'єкти для INSERT
 *   <li>{@code dirtyEntities} — змінені об'єкти для UPDATE
 *   <li>{@code deletedIds} — видалені об'єкти для DELETE
 * </ul>
 *
 * <p>Інтеграція з {@link IdentityMap}: після commit() кеш очищається - стан синхронізуований з БД.
 *
 * <pre>{@code
 * try (UnitOfWork uow = new UnitOfWork(connectionManager)) {
 *     uow.registerNew(author, a -> authorRepo.saveWithConnection(a, uow.getConnection()));
 *     uow.registerNew(book,   b -> bookRepo.saveWithConnection(b, uow.getConnection()));
 *     uow.commit(); // один BEGIN/COMMIT для обох INSERT
 * } catch (Exception e) {
 *     uow.rollback();
 * }
 * }</pre>
 */
public class UnitOfWork implements AutoCloseable {

    private final ConnectionManager connectionManager;

    /**
     * Реєстр нових об'єктів для INSERT. {@link LinkedHashMap} зберігає порядок вставки - \
     * важливо для FK-залежностей (спочатку батьківські, потім дочірні).
     */
    private final Map<UUID, Runnable> newEntities = new LinkedHashMap<> ();

    /**
     * Реєстр змінених об'єктів для UPDATE. {@link LinkedHashMap} зберігає порядок реєстрації
     */
    private final Map<UUID, Runnable> dirtyEntities = new LinkedHashMap<>();
    /** Реєстр видалених об'єктів для DELETE. */
    private final List<Runnable> deletedEntities = new ArrayList<>();

    /** Активне JDBC-з'єднання для поточної транзакції. null до першого registerNew/Dirty. */
    private Connection activeConnection = null;

    /** Прапорець стану — після commit/rollback UoW не можна використовувати повторно. */
    private boolean completed = false;

    /**
     * Створює новий Unit of Work.
     *
     * @param connectionManager менеджер пулу з'єднань
     */
    public UnitOfWork(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Реєструє новий об'єкт для INSERT.
     *
     * @param id UUID нової сутності
     * @param insertAction дія INSERT (lambda що викликає репозиторій)
     */
    public void registerNew(UUID id, Runnable insertAction) {
        checkNotCompleted();
        newEntities.put(id, insertAction);
    }

    /**
     * Реєструє змінений об'єкт для UPDATE.
     *
     * <p>Якщо об'єкт вже зареєстрований як новий — оновлення не потрібне (INSERT включить нові
     * дані).
     *
     * @param id UUID зміненої сутності
     * @param updateAction дія UPDATE (lambda що викликає репозиторій)
     */
    public void registerDirty(UUID id, Runnable updateAction) {
        checkNotCompleted();
        if (!newEntities.containsKey(id)) {
            dirtyEntities.put(id, updateAction);
        }
    }

    /**
     * Реєструє об'єкт для DELETE.
     *
     * @param id UUID видаленої сутності
     * @param deleteAction дія DELETE (lambda що викликає репозиторій)
     */
    public void registerDeleted(UUID id, Runnable deleteAction) {
        checkNotCompleted();
        newEntities.remove(id);
        dirtyEntities.remove(id);
        deletedEntities.add(deleteAction);
    }

    /**
     * Виконує всі накопичені операції в одній JDBC-транзакції.
     *
     * <p>Порядок: INSERT → UPDATE → DELETE (зберігає цілісність FK). При будь-якій помилці —
     * автоматичний ROLLBACK.
     *
     * @throws DatabaseException якщо транзакція провалилась
     */
    public void commit() {
        checkNotCompleted();

        if (newEntities.isEmpty() && dirtyEntities.isEmpty() && deletedEntities.isEmpty()) {
            completed = true;
            return;
        }

        try (Connection conn = connectionManager.getConnection()) {
            activeConnection = conn;
            conn.setAutoCommit(false);

            try {
                // 1. INSERT (зберігаємо порядок для FK)
                for (Runnable action : newEntities.values()) {
                    action.run();
                }

                // 2. UPDATE
                for (Runnable action : dirtyEntities.values()) {
                    action.run();
                }

                // 3. DELETE (останніми — уникаємо FK violations)
                for (Runnable action : deletedEntities) {
                    action.run();
                }

                conn.commit();
                completed = true;

                System.out.printf(
                      "[UoW] Commit: %d INSERT, %d UPDATE, %d DELETE%n",
                      newEntities.size(), dirtyEntities.size(), deletedEntities.size());

            } catch (Exception e) {
                conn.rollback();
                throw new DatabaseException("UnitOfWork commit провалився, виконано rollback", e);
            } finally {
                activeConnection = null;
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка отримання з'єднання для UnitOfWork", e);
        }
    }

    /**
     * Відміняє всі накопичені операції без звернення до БД.
     *
     * <p>Просто очищає реєстри — жодного SQL не виконувалось до commit().
     */
    public void rollback() {
        newEntities.clear();
        dirtyEntities.clear();
        deletedEntities.clear();
        completed = true;
        System.out.println("[UoW] Rollback: всі зміни скасовано");
    }

    /**
     * Повертає кількість зареєстрованих операцій.
     *
     * @return загальна кількість операцій
     */
    public int pendingOperations() {
        return newEntities.size() + dirtyEntities.size() + deletedEntities.size();
    }

    /** Перевіряє що UoW ще не завершений. */
    private void checkNotCompleted() {
        if (completed) {
            throw new DatabaseException(
                  "UnitOfWork вже завершено — створіть новий екземпляр");
        }
    }

    /**
     * AutoCloseable: якщо commit() не викликали — виконує rollback автоматично. Безпечне
     * використання у try-with-resources.
     */
    @Override
    public void close() {
        if (!completed) {
            rollback();
        }
    }
}
