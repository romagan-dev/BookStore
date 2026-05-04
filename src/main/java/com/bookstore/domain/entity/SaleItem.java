package com.bookstore.domain.entity;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Сутність позиції покупки.
 *
 * <p>Відповідає таблиці {@code sale_items} у базі даних. Тип таблиці: Transaction. Зв'язки:
 *
 * <ul>
 *   <li>N:1 з {@link Sale} — позиція належить одному чеку
 *   <li>N:1 з {@link Book} — позиція посилається на одну книгу
 * </ul>
 *
 * <p>Поле {@code unitPrice} — знімок ціни на момент продажу (свідома денормалізація для аудиту:
 * ціна книги може змінитись, але факт продажу — ні).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SaleItem {

  /** Первинний ключ. Відповідає {@code sale_item_id} у БД. */
  private UUID id;

  /**
   * Чек, до якого належить позиція. Вирішення розбіжності зв'язків: FK {@code sale_id} → об'єкт
   * {@link Sale}.
   */
  private Sale sale;

  /** Книга в позиції. Вирішення розбіжності зв'язків: FK {@code book_id} → об'єкт {@link Book}. */
  private Book book;

  /** Кількість примірників. Завжди понад 0. */
  private int quantity;

  /** Ціна за одиницю на момент продажу. Завжди понад 0. */
  private BigDecimal unitPrice;

  /**
   * Обчислює загальну вартість позиції.
   *
   * @return {@code unitPrice * quantity}
   */
  public BigDecimal getTotalPrice() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }

  /** Рівність визначається виключно за первинним ключем — узгоджено з реляційною моделлю. */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SaleItem other)) return false;
    return id != null && id == other.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
