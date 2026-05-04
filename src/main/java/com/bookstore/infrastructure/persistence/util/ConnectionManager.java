package com.bookstore.infrastructure.persistence.util;

/**
 * Фасад для отримання з'єднань з бази даних.
 *
 * <p>Абстрагує клієнтський код (Repository) від деталей реалізації пулу. Реалізує патерн Singleton
 * — один екземпляр на весь додаток.
 *
 * <p>Приклад використання:
 *
 * <pre>{@code
 * try (Connection conn = connectionManager.getConnection()) {
 *     // conn.close() → PooledConnection.close() → повернення до пулу
 * }
 * }</pre>
 */
public class ConnectionManager {}
