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
 * Сутність позиції постачання.
 *
 * <p>Відповідає таблиці {@code supply_items} у базі даних. Тип таблиці: Transaction. Зв'язки:
 *
 * <ul>
 *   <li>N:1 з {@link Supply} — позиція належить одному постачанню
 *   <li>N:1 з {@link Book} — позиція посилається на одну книгу
 * </ul>
 *
 * <p>Поле {@code unitCost} — знімок закупівельної ціни на момент постачання.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SupplyItem {

  /** Первинний ключ. Відповідає {@code supply_item_id} у БД. */
  private UUID id;

  /**
   * Постачання, до якого належить позиція. Вирішення розбіжності зв'язків: FK {@code supply_id} →
   * об'єкт {@link Supply}.
   */
  private Supply supply;

  /** Книга в позиції. Вирішення розбіжності зв'язків: FK {@code book_id} → об'єкт {@link Book}. */
  private Book book;

  /** Кількість примірників. Завжди понад 0. */
  private int quantity;

  /** Закупівельна ціна за одиницю. Завжди понад 0. */
  private BigDecimal unitCost;

  /**
   * Обчислює загальну вартість позиції постачання.
   *
   * @return {@code unitCost * quantity}
   */
  public BigDecimal getTotalCost() {
    return unitCost.multiply(BigDecimal.valueOf(quantity));
  }

  /** Рівність визначається виключно за первинним ключем — узгоджено з реляційною моделлю. */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SupplyItem other)) return false;
    return id != null && id == other.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
