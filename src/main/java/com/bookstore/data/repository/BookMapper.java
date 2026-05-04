package com.bookstore.data.repository;

import com.bookstore.domain.entity.Book;
import com.bookstore.domain.entity.Category;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookMapper {
  public Book map(ResultSet rs) throws SQLException {
      //Використовуємо Builder від Lombok
    return Book.builder()
          .id(rs.getLong("book_id"))
          .title(rs.getString("title"))
          .price(rs.getBigDecimal("price"))
          .stockQuantity(rs.getInt("description"))
          .category(Category.builder().id(rs.getLong("category_id")).build())
          .build();
  }
}
