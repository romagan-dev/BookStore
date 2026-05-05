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
 * Сутність категорії/жанру книги.
 *
 * <p>Відповідає таблиці {@code categories} у базі даних. Тип таблиці: Static Reference — змінюється
 * рідко, лише адміністратором.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Category {

  /** Первинний ключ. Відповідає {@code category_id} у БД. */
  private UUID id;

  /** Назва категорії. Унікальна, не може бути null. */
  private String name;

  /** Опис категорії. Може бути null. */
  private String description;

  /** Рівність визначається виключно за первинним ключем — узгоджено з реляційною моделлю. */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Category other)) return false;
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
