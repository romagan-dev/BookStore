package com.bookstore.data.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SimpleConnectionPool {
  private final String url;
  private final String user;
  private final String password;

  // Черга де зберігаються наші готові з'єднання
  private final BlockingQueue<Connection> connectionPool;

  // Конструктор
  public SimpleConnectionPool(String url, String password, String user, int poolSize) {
    this.url = url;
    this.user = user;
    this.password = password;

    this.connectionPool = new ArrayBlockingQueue<>(poolSize);
    initializePool(poolSize);
  }

  // Метод, який створює з'єднання при старті
  private void initializePool(int poolSize) {
    try {
      for (int i = 0; i < poolSize; i++) {
        Connection physicalConnection = DriverManager.getConnection(url, user, password);
        connectionPool.add(new PooledConnection(physicalConnection, this));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не вдалося ініціалізувати пул з'єднань", e);
    }
  }

  // Метод який віддає з'єднання репозиторію
  public Connection getConnection() {
    try {

      // take() бере з'єднання. Якщо пул пусти, потік чекатиме, поки хтось не поверне з'єдання
      return connectionPool.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Помилка отримання з'єдання з пулу", e);
    }
  }

  // Метод, який повертає з'єдання назад у чергу, коли репозиторій закінчив роботу
  public void releaseConnection(Connection connection) {
    if (connection != null) {
      connectionPool.offer(connection);
    }
  }
}
