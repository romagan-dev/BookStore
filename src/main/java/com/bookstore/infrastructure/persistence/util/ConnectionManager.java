package com.bookstore.infrastructure.persistence.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
public class ConnectionManager implements AutoCloseable {

  /** Єдиний екземпляр (Singleton) */
  private static volatile ConnectionManager instance;

  private final SimpleConnectionPool pool;

  /**
   * Приватний конструктор для Singleton
   *
   * @param config конфігурація пулу
   */
  private ConnectionManager(PoolConfig config) {
    this.pool = new SimpleConnectionPool(config);
  }

  /**
   * Повертає єдиний екземпляр ConnectionManager (Double-checked locking Singleton)
   *
   * @param config конфігурація пулу (використовується лише при першому виклику
   * @return єдиний екземпляр
   */
  public static ConnectionManager getInstance(PoolConfig config) {
    if (instance == null) {
      synchronized (ConnectionManager.class) {
        if (instance == null) {
          instance = new ConnectionManager(config);
        }
      }
    }
    return instance;
  }

    public static void resetInstance() {
        instance = null;
    }

  /**
   * Зручний фабричний метод для SQLite.
   *
   * @param dbPath шлях до файлу БД
   * @return єдиний екземпляр
   */
  public static ConnectionManager forSQLite(String dbPath) {
    return getInstance(PoolConfig.forSQLite(dbPath));
  }

  /**
   * Повертає зв'язок з пулу.
   *
   * <p>Клієнтський код зобов'язаний викликати {@code close()} через try-with-resources. Реального
   * закриття ТСР не відбудеться - з'єднання повернеться до пулу
   *
   * @return з'єднання обгорнуте у Proxy
   */
  public Connection getConnection() {
    return pool.getConnection();
  }

  /**
   * Повертає статистику пулу у вигляді рядка
   *
   * @return рядок з кількістю вільних, зайнятих та максимальною кількістю з'єднань
   */
  public String poolStats() {
    return String.format(
        "Pool: %available / %d total / %d max",
        pool.availableCount(), pool.totalCount(), pool.config().maxConnections());
  }


    /** Виводить детальну статистику пулу в консоль */
  public void printStats() {
    pool.printStats();
  }

  /**
   * Закриває пул і всі з'єднання.
   *
   * <p>Викликати при завершенні роботи додатка.
   */
  @Override
  public void close() {
    pool.close();
    instance = null;
  }
}
