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
 * Сутність автора книги.
 *
 * <p>Відповідає таблиці {@code authors} у базі даних. Тип таблиці: Master Data — основна бізнес
 * сутність. Зв'язок з книгами — N:M через таблицю {@code book_authors}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {

  /** Первинний ключ. Відповідає {@code author_id} у БД. */
  private UUID id;

  /** Ім'я автора. Не може бути null. */
  private String firstName;

  /** Прізвище автора. Не може бути null. */
  private String lastName;

  /**
   * Повертає повне ім'я автора у форматі "Ім'я Прізвище".
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
    if (o == null || getClass() != o.getClass()) return false;
    Author author = (Author) o;
    return Objects.equals(id, author.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
