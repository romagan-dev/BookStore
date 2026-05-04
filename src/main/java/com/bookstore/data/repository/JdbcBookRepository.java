package com.bookstore.data.repository;

import com.bookstore.data.connection.SimpleConnectionPool;
import com.bookstore.domain.entity.Book;
import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

public class JdbcBookRepository implements BookRepository {
  private final SimpleConnectionPool connectionPool;
  private final BookMapper mapper;

  public JdbcBookRepository(SimpleConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
    this.mapper = new BookMapper();
  }

  @Override
  public Optional<Book> findById(Long id) {
    String sql = "SELECT * FROM books WHERE book_id = ?";

    // отримуємо конекшн із нашого пулу
    try (Connection conn = connectionPool.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setLong(1, id);
     try (ResultSet rs = stmt.executeQuery()) {
         if (rs.next()) {
             return Optional.of(mapper.map(rs));
         }
     }
    } catch (SQLException e) {
      throw new RuntimeException("Помилка при пошуку книги за ID:", e);
    }
    return Optional.empty();
  }

    @Override
    public List<Book> findAll() {
        String sql = "SELECT * FROM books";
        List<Book> books = new ArrayList<>();

        try (Connection conn = connectionPool.getConnection();
              Statement stmt = conn.createStatement();
              ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Мапер робить всю брудну роботу з перетворення рядка в об'єкт
                books.add(mapper.map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні списку книг", e);
        }
        return books;
    }

    @Override
    public void save(Book book) {
        String sql = "INSERT INTO books (category_id, title, price, stock_quantity, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionPool.getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, book.getCategory().getId());
            stmt.setString(2, book.getTitle());
            stmt.setBigDecimal(3, book.getPrice());
            stmt.setInt(4, book.getStockQuantity());
            stmt.setString(5, book.getDescription());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при збереженні книги", e);
        }
    }
  // Реалізація findAll...

}
