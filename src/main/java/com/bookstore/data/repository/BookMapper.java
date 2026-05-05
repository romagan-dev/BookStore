package com.bookstore.data.repository;

import com.bookstore.domain.entity.Book;
import com.bookstore.domain.entity.Category;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class BookMapper {
  public Book map(ResultSet rs) throws SQLException {
      //Використовуємо Builder від Lombok
    return Book.builder()
          .id(rs.getObject("book_id", UUID.class))
          .title(rs.getString("title"))
          .price(rs.getBigDecimal("price"))
          .stockQuantity(rs.getInt("stock_quantity"))
          .category(Category.builder().id((UUID) rs.getObject("category_id")).build())
          .build();
  }
}
