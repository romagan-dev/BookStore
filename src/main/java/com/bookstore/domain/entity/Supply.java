package com.bookstore.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Сутність постачання товару.
 *
 * <p>Відповідає таблиці {@code supplies} у базі даних. Тип таблиці: Transaction — фіксує факт
 * постачання. Зв'язки:
 *
 * <ul>
 *   <li>N:1 з {@link Supplier} — постачання від одного постачальника
 *   <li>1:N з {@link SupplyItem} — одне постачання містить багато позицій
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Supply {

  /** Первинний ключ. Відповідає {@code supply_id} у БД. */
  private UUID id;

  /**
   * Постачальник. Вирішення розбіжності зв'язків: FK {@code supplier_id} → об'єкт {@link Supplier}.
   */
  private Supplier supplier;

  /** Дата та час постачання. Не може бути null. */
  private LocalDateTime supplyDate;

  /** Загальна вартість постачання. Завжди понад 0. */
  private BigDecimal totalCost;

  /** Позиції постачання. Lazy-завантаження: список може бути порожнім до явного запиту. */
  @Builder.Default private List<SupplyItem> items = new ArrayList<>();

  /** Рівність визначається виключно за первинним ключем — узгоджено з реляційною моделлю. */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Supply other)) return false;
    return id != null && id == other.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
