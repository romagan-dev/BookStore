package com.bookstore.domain.entity;

import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Сутність користувача системи.
 *
 * <p>Відповідає таблиці {@code users} у базі даних. Тип таблиці: Master Data. Один користувач може
 * оформити багато покупок (1:N). Роль визначає рівень доступу до функцій системи.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User {

  /**
   * Перелік ролей користувача системи.
   *
   * <p>Значення відповідають CHECK-обмеженню у таблиці {@code users}.
   */
  public enum Role {
    /** Повний доступ до всіх функцій системи. */
    admin,
    /** Доступ до оформлення покупок та роботи з клієнтами */
    cashier,
    /** Доступ до управління асортиментом та постачаннями. */
    manager
  }

  /** Первинний ключ. Відповідає {@code user_id} у БД */
  private UUID id;

  /** Логін користувача. Унікальний, не може бути null. */
  private String username;

  /** Хеш паролю (bcrypt). Не може бути null. */
  private String passwordHash;

  /** Email адреса. Унікальна, не може бути null. */
  private String email;

  /** Роль користувача в системі. Не може бути null. */
  private Role role;

  /**
   * Перевіряє чи має користувач роль адміністратора
   *
   * <p>return {@code true} якщо роль - admin
   */
  public boolean isAdmin() {
    return Role.admin.equals(role);
  }

  /**
   * Перевіряє чи має користувач роль касира.
   *
   * <p>return {@code true} якщо роль - cashier
   */
  public boolean isCashier() {
    return Role.cashier.equals(role);
  }

  /**
   * Перевіряє чи має користувач роль менеджера.
   *
   * @return {@code true} якщо роль — manager
   */
  public boolean isManager() {
    return Role.manager.equals(role);
  }

  /** Рівність визначається виключано за первинним ключем -узгоджено з реляційно моделлю */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User other)) return false;
    return id != null && id == other.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
