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
 * Сутність покупки (чека).
 *
 * <p>Відповідає таблиці {@code sales} у базі даних. Тип таблиці: Transaction — фіксує факт покупки.
 * Зв'язки:
 *
 * <ul>
 *   <li>N:1 з {@link User} — покупку оформлює касир
 *   <li>N:1 з {@link Client} — покупець (може бути null — анонімна покупка)
 *   <li>1:N з {@link SaleItem} — один чек містить багато позицій
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Sale {

  /**
   * Перелік способів оплати.
   *
   * <p>Значення відповідають CHECK-обмеженню у таблиці {@code sales}.
   */
  public enum PaymentMethod {
    cash,
    card,
    online
  }

  /** Первинний ключ. Відповідає {@code sale_id} у БД. */
  private UUID id;

  /**
   * Касир, що оформив покупку. Вирішення розбіжності зв'язків: FK {@code user_id} → об'єкт {@link
   * User}.
   */
  private User user;

  /**
   * Покупець. Може бути null (анонімна покупка). Вирішення розбіжності зв'язків: FK {@code
   * client_id} → об'єкт {@link Client}.
   */
  private Client client;

  /** Дата та час оформлення покупки. Не може бути null. */
  private LocalDateTime saleDate;

  /** Загальна сума покупки. Не може бути від'ємною. */
  private BigDecimal totalAmount;

  /** Спосіб оплати. Не може бути null. */
  private PaymentMethod paymentMethod;

  /** Позиції чека. Lazy-завантаження: список може бути порожнім до явного запиту. */
  @Builder.Default private List<SaleItem> items = new ArrayList<>();

  /**
   * Перевіряє чи є покупка анонімною (без прив'язки до клієнта).
   *
   * @return {@code true} якщо клієнт не вказаний
   */
  public boolean isAnonymous() {
    return client == null;
  }

  /** Рівність визначається виключно за первинним ключем — узгоджено з реляційною моделлю. */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Sale other)) return false;
    return id != null && id == other.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
