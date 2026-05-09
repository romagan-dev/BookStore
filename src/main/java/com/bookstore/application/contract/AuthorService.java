package com.bookstore.application.contract;

import com.bookstore.domain.entity.Author;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт сервісу для роботи з авторами.
 */
public interface AuthorService {

    /**
     * Повертає всіх авторів.
     *
     * @return список авторів
     */
    List<Author> findAll();

    /**
     * Знаходить автора за UUID.
     *
     * @param id UUID автора
     * @return Optional з автором
     */
    Optional<Author> findById(UUID id);

    /**
     * Пошук авторів за прізвищем.
     *
     * @param lastName прізвище або його частина
     * @return список знайдених авторів
     */
    List<Author> search(String lastName);

    /**
     * Створює нового автора.
     *
     * @param firstName ім'я
     * @param lastName прізвище
     * @return створений автор
     * @throws IllegalArgumentException якщо дані невалідні
     */
    Author create(String firstName, String lastName);

    /**
     * Оновлює дані автора.
     *
     * @param id UUID автора
     * @param firstName нове ім'я
     * @param lastName нове прізвище
     * @return оновлений автор
     * @throws IllegalArgumentException якщо автор не знайдений або дані невалідні
     */
    Author update(UUID id, String firstName, String lastName);

    /**
     * Видаляє автора.
     *
     * @param id UUID автора
     * @throws IllegalArgumentException якщо автор не знайдений
     * @throws IllegalStateException якщо автор прив'язаний до книг і не може бути видалений
     */
    void delete(UUID id);
}