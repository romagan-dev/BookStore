package com.bookstore.application.contract;

import com.bookstore.domain.entity.Client;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт сервісу для роботи з клієнтами.
 */
public interface ClientService {

    /**
     * Повертає всіх клієнтів.
     *
     * @return список клієнтів
     */
    List<Client> findAll();

    /**
     * Знаходить клієнта за UUID.
     *
     * @param id UUID клієнта
     * @return Optional з клієнтом
     */
    Optional<Client> findById(UUID id);

    /**
     * Пошук клієнтів за іменем або прізвищем.
     *
     * @param query рядок пошуку
     * @return список знайдених клієнтів
     */
    List<Client> search(String query);

    /**
     * Створює нового клієнта.
     *
     * @param firstName ім'я
     * @param lastName прізвище
     * @param phone телефон (може бути null)
     * @param email email (може бути null)
     * @return створений клієнт
     * @throws IllegalArgumentException якщо дані невалідні або телефон/email вже існує
     */
    Client create(String firstName, String lastName, String phone, String email);

    /**
     * Оновлює дані клієнта.
     *
     * @param id UUID клієнта
     * @param firstName нове ім'я
     * @param lastName нове прізвище
     * @param phone новий телефон
     * @param email новий email
     * @return оновлений клієнт
     * @throws IllegalArgumentException якщо клієнт не знайдений або дані невалідні
     */
    Client update(UUID id, String firstName, String lastName, String phone, String email);

    /**
     * Видаляє клієнта.
     *
     * @param id UUID клієнта
     * @throws IllegalArgumentException якщо клієнт не знайдений
     */
    void delete(UUID id);

    /**
     * Перевіряє чи існує клієнт з таким телефоном.
     *
     * @param phone телефон
     * @return true якщо існує
     */
    boolean existsByPhone(String phone);
}
