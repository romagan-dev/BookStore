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
 * Сутність покупця магазину.
 *
 * <p>Відповідає таблиці {@code clients} у базі даних. Тип таблиці: Master Data. Покупка може бути
 * анонімною — {@code client_id} у таблиці {@code sales} допускає NULL.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Client {

  /** Первинний ключ. Відповідає {@code client_id} у БД. */
  private UUID id;

  /** Ім'я покупця. Не може бути null. */
  private String firstName;

  /** Прізвище покупця. Не може бути null. */
  private String lastName;

  /** Номер телефону. Унікальний, може бути null. */
  private String phone;

  /** Email адреса. Унікальна, може бути null. */
  private String email;

  /**
   * Повертає повне ім'я покупця у форматі "Ім'я Прізвище".
   *
   * @return повне ім'я
   */
  public String getFullName() {
    return firstName + " " + lastName;
  }

  /** Рівність визначається виключно за первинним ключем — узгоджено з реляційною моделлю. */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Client other)) return false;
    return id != null && id == other.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
