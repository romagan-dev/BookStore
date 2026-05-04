package com.bookstore.domain.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Сутність книги магазину.
 *
 * <p>Відповідає таблиці {@code books} у базі даних. Тип таблиці: Master Data — основна бізнес
 * сутність. Зв'язки:
 *
 * <ul>
 *   <li>N:1 з {@link Category} — книга належить одній категорії
 *   <li>N:M з {@link Author} — книга може мати кількох авторів (через {@code book_authors})
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {
  /** Первинний ключ. Відповідає {@code book_id} у БД. */
  private Long id;

  /**
   * Категорія книги. Вирішення розбіжності зв'язків: FK {@code category_id} → об'єкт {@link
   * Category}.
   */
  private Category category;

  /** Назва книги. Не може бути null. */
  private String title;

  // ** Ціна книги. Завжди більше 0. */
  private BigDecimal price;

  /** Кількість на складі. Не може бути від'ємною. */
  private int stockQuantity;

  /** Опис книги. Може бути null. */
  private String description;

  /**
   * Список авторів книги. Вирішення розбіжності N:M: проміжна таблиця {@code book_authors} →
   * колекція об'єктів {@link Author}.
   *
   * <p>Lazy-завантаження: список може бути порожнім до явного запиту.
   */
  @Builder.Default private List<Author> authors = new ArrayList<>();

  /**
   * Перевіряє чи є книга в наявності на складі.
   *
   * @return {@code true} якщо кількість більше 0
   */
  public boolean isInStock() {
    return stockQuantity > 0;
  }

  /**
   * Зменшує кількість книг на складі після продажу.
   *
   * @param quantity кількість проданих примірників
   * @throws IllegalArgumentException якщо кількість від'ємна або перевищує залишок
   */
  public void decreaseStock(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Кількість має бути більше 0");
    }
    if (quantity > stockQuantity) {
      throw new IllegalArgumentException("Недостатньо книг на складі. Доступно: " + stockQuantity);
    }
    this.stockQuantity -= quantity;
  }

  /**
   * Збільшує кількість книг на складі після постачання.
   *
   * @param quantity кількість отриманих примірників
   * @throws IllegalArgumentException якщо кількість від'ємна
   */
  public void increaseStock(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Кількість має бути більше 0");
    }
    this.stockQuantity += quantity;
  }

  /** Рівність визначається виключно за первинним ключем — узгоджено з реляційною моделлю. */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Book other)) return false;
    return id != 0 && id == other.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
