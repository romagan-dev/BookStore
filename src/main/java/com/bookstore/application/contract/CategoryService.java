package com.bookstore.application.contract;

import com.bookstore.domain.entity.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт сервісу для роботи з категоріями.
 */
public interface CategoryService {

    /**
     * Повертає список усіх категорій.
     *
     * @return список усіх категорій
     */
    List<Category> findAll();

    /**
     * Знаходить категорію за її UUID.
     *
     * @param id UUID категорії
     * @return Optional з категорією
     */
    Optional<Category> findById(UUID id);

    /**
     * Знаходить категорію за точною назвою.
     *
     * @param name назва категорії
     * @return Optional зі знайденою категорією
     */
    Optional<Category> findByName(String name);

    /**
     * Пошук категорій за частиною назви.
     *
     * @param query рядок пошуку
     * @return список знайдених категорій
     */
    List<Category> search(String query);

    /**
     * Створює нову категорію.
     *
     * @param name назва категорії
     * @param description опис категорії (може бути null)
     * @return створена категорія
     * @throws IllegalArgumentException якщо назва порожня або категорія з такою назвою вже існує
     */
    Category create(String name, String description);

    /**
     * Оновлює дані категорії.
     *
     * @param id UUID категорії
     * @param name нова назва
     * @param description новий опис
     * @return оновлена категорія
     * @throws IllegalArgumentException якщо категорію не знайдено або назва вже зайнята
     */
    Category update(UUID id, String name, String description);


    /**
     * Видаляє категорію за UUID.
     *
     * @param id UUID категорії
     * @throws IllegalArgumentException якщо категорію не знайдено
     * @throws IllegalStateException якщо до категорії прив'язані книги
     */
    void delete(UUID id);
}