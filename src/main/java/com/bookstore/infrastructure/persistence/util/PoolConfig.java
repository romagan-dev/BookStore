package com.bookstore.infrastructure.persistence.util;

/**
 * Незмінна конфігурація пулу з'єднань
 *
 * <p>Record-клас гарантує, що параметри не можуть бути змінені після створення.
 */
public record PoolConfig (

    String url,
    int minConnections,
    int maxConnections,
    long timeoutMs){


        /** Валідація параметрів при створенні конфігурації.*/
        public PoolConfig {
        if (minConnections < 1)
            throw new IllegalArgumentException("minConnections має бути >= 1");
        if (maxConnections < minConnections)
            throw new IllegalArgumentException("maxConnections >= minConnections");
        if (timeoutMs < 0)
            throw new IllegalArgumentException("timeoutMs >= 0");
    }


    /**
     * Фабричний метод для SQLite з налаштуваннями за замовчуванням.
     *
     * @param dbPath шлях до файлу БД
     * @return конфігурація пулу
     */
    public static PoolConfig forSQLite(String dbPath) {
        return new PoolConfig(
              "jdbc:sqlite" + dbPath,
              2,
              10,
              5000L);
    }
}

