package com.bookstore.infrastructure.persistence.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Власна реалізація Connection Pool на основі патерну Object Pool.
 *
 * <p>Ключові компоненти:
 *
 * <ul>
 *   <li>{@code availableConnections} — черга вільних з'єднань ({@link BlockingQueue})
 *   <li>{@code totalConnections} — атомарний лічильник всіх відкритих з'єднань
 *   <li>{@link PooledConnection} — Proxy-обгортка, що перехоплює {@code close()}
 * </ul>
 *
 * <p>Патерни: Object Pool, Proxy, Template Method. SQLite не потребує user/password — лише URL.
 */
public class SimpleConnectionPool implements AutoCloseable {

    private final PoolConfig config;

    /**
     * Потокобезпечна черга вільних з'єднань. {@code poll(timeout)} блокує поточний потік до появи
     * вільного з'єднання або закінчення timeout.
     */
    private final BlockingQueue<Connection> availableConnections;

    /**
     * Атомарний лічильник без synchronized. Відстежує загальну кількість відкритих з'єднань
     * (вільних + зайнятих).
     */
    private final AtomicInteger totalConnections = new AtomicInteger(0);

    /** Прапорець закриття пулу — після close() нові з'єднання не видаються. */
    private volatile boolean closed = false;

    /**
     * Ініціалізує пул і відкриває мінімальну кількість з'єднань.
     *
     * <p>При помилці вже відкриті з'єднання закриваються — без джерел ресурсів.
     *
     * @param config конфігурація пулу
     * @throws DatabaseException якщо не вдалося ініціалізувати з'єднання
     */
    public SimpleConnectionPool(PoolConfig config) {
        this.config = config;
        this.availableConnections = new ArrayBlockingQueue<>(config.maxConnections());

        try {
            for (int i = 0; i < config.minConnections(); i++) {
                availableConnections.add(createRealConnection());
            }
        } catch (SQLException e) {
            close();
            throw new DatabaseException("Не вдалося ініціалізувати Connection Pool", e);
        }

        System.out.printf(
              "[Pool] Ініціалізовано: %d з'єднань готові%n", config.minConnections());
    }

    /**
     * Повертає з'єднання з пулу або створює нове, якщо не перевищено максимум.
     *
     * <p>Якщо пул вичерпано — блокує поточний потік на {@code timeoutMs} мілісекунди.
     *
     * @return з'єднання обгорнуте у {@link PooledConnection}
     * @throws DatabaseException якщо з'єднання недоступне впродовж timeout
     */
    public Connection getConnection() {
        if (closed) {
            throw new DatabaseException("Connection Pool закрито");
        }

        // Спроба 1: взяти вільне з'єднання без очікування
        Connection conn = availableConnections.poll();

        if (conn == null) {
            // Вільних немає — чи можемо створити нове?
            int current = totalConnections.get();
            if (current < config.maxConnections()) {
                // Атомарна операція: перевірити й збільшити без race condition
                if (totalConnections.compareAndSet(current, current + 1)) {
                    try {
                        conn = createRealConnection();
                        System.out.printf(
                              "[Pool] Нове з'єднання: всього %d/%d%n",
                              totalConnections.get(), config.maxConnections());
                    } catch (SQLException e) {
                        totalConnections.decrementAndGet();
                        throw new DatabaseException("Не вдалося створити з'єднання", e);
                    }
                }
            }

            if (conn == null) {
                // Пул вичерпано — чекаємо вільного з'єднання
                try {
                    conn =
                          availableConnections.poll(
                                config.timeoutMs(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DatabaseException("Очікування з'єднання перервано", e);
                }

                if (conn == null) {
                    throw new DatabaseException(
                          String.format(
                                "З'єднання недоступне впродовж %d мс. Пул вичерпано"
                                      + " (%d/%d)",
                                config.timeoutMs(),
                                totalConnections.get(),
                                config.maxConnections()));
                }
            }
        }

        return new PooledConnection(conn, this);
    }

    /**
     * Повертає з'єднання до пулу.
     *
     * <p>Викликається з {@link PooledConnection#close()} — не безпосередньо клієнтом. Якщо пул
     * закрито або з'єднання не валідне — закриває реально.
     *
     * @param realConnection реальне з'єднання для повернення
     */
    void returnConnection(Connection realConnection) {
        if (closed) {
            closeQuietly(realConnection);
            totalConnections.decrementAndGet();
            return;
        }

        if (isConnectionValid(realConnection)) {
            availableConnections.offer(realConnection);
        } else {
            closeQuietly(realConnection);
            totalConnections.decrementAndGet();
            System.out.println("[Pool] З'єднання відхилено (не валідне), видалено з пулу");
        }
    }

    /**
     * Виводить поточну статистику пулу.
     *
     * <p>Показує кількість вільних, зайнятих та максимальну кількість з'єднань.
     */
    public void printStats() {
        int available = availableConnections.size();
        int total = totalConnections.get();
        int busy = total - available;
        int max = config.maxConnections();
        double load = max > 0 ? (double) busy / max * 100 : 0;

        System.out.printf(
              "[Pool] Вільних: %d | Зайнятих: %d | Всього: %d/%d | Завантаженість:"
                    + " %.1f%%%n",
              available, busy, total, max, load);
    }

    /** @return кількість вільних з'єднань */
    public int availableCount() {
        return availableConnections.size();
    }

    /** @return загальна кількість відкритих з'єднань */
    public int totalCount() {
        return totalConnections.get();
    }

    /** @return конфігурація пулу */
    public PoolConfig config() {
        return config;
    }

    /** Перевіряє чи з'єднання ще активне через JDBC ping. */
    private boolean isConnectionValid(Connection conn) {
        try {
            return !conn.isClosed() && conn.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Створює реальне фізичне з'єднання з SQLite БД.
     *
     * <p>SQLite не потребує user/password — лише URL у форматі {@code jdbc:sqlite:path/to/db}.
     */
    private Connection createRealConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(config.url());
        totalConnections.incrementAndGet();
        return conn;
    }

    /** Закриває з'єднання без викидання виключень. */
    private void closeQuietly(Connection conn) {
        try {
            conn.close();
        } catch (SQLException ignored) {
        }
    }

    /**
     * Закриває пул: встановлює прапорець {@code closed} і закриває всі вільні з'єднання.
     *
     * <p>З'єднання, що зараз використовуються, закриються при поверненні в пул.
     */
    @Override
    public void close() {
        closed = true;
        List<Connection> toClose = new ArrayList<>();
        availableConnections.drainTo(toClose);
        toClose.forEach(this::closeQuietly);
        System.out.printf("[Pool] Закрито. Закрито %d з'єднань%n", toClose.size());
    }
}