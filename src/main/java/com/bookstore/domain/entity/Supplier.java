package com.bookstore.domain.entity;

import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Сутність постачальника книг.
 *
 * <p>Відповідає таблиці {@code suppliers} у базі даних. Тип таблиці: Master Data — основна бізнес
 * сутність. Один постачальник може здійснювати багато постачань (1:N).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Supplier {

  /** Первинний ключ. Відповідає {@code supplier_id} у БД. */
  private UUID id;

  /** Назва компанії-постачальника. Не може бути null. */
  private String companyName;

  /** Номер телефону. Може бути null. */
  private String phone;

  /** Email адреса. Може бути null. */
  private String email;

  /** Фізична адреса. Може бути null. */
  private String address;

  /** Рівність визначається виключно за первинним ключем — узгоджено з реляційною моделлю. */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Supplier other)) return false;
    return id != null && id == other.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
