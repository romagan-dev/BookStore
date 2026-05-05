package com.bookstore.infrastructure.persistence.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Реалізація патерну Identity Map.
 *
 * <p>Гарантує що кожна сутність завантажується з БД лише один раз у рамках сесії. При повторному
 * запиті того самого UUID — повертає вже існуючий об'єкт з кешу без звернення до БД.
 *
 * <p>Вирішує три проблеми:
 *
 * <ul>
 *   <li>Надлишкові SELECT-запити до БД
 *   <li>Розбіжність стану (два об'єкти з одним UUID мають різні дані)
 *   <li>Порушення семантики ідентичності (один UUID = один об'єкт у пам'яті)
 * </ul>
 *
 * <p>Область видимості: один екземпляр на одну бізнес-операцію (сесію). НЕ є глобальним
 * синглтоном — кожна сесія/транзакція має власний IdentityMap.
 *
 * @param <T> тип доменної сутності
 */
public class IdentityMap<T> {

    /**
     * Внутрішній кеш: UUID → об'єкт сутності.
     *
     * <p>{@link HashMap} (не ConcurrentHashMap) — IdentityMap не є потокобезпечним навмисно:
     * кожен потік/сесія має власний екземпляр.
     */
    private final Map<UUID, T> cache = new HashMap<>();

    /**
     * Повертає сутність з кешу за UUID.
     *
     * @param id первинний ключ
     * @return Optional зі знайденою сутністю або empty якщо не в кеші
     */
    public Optional<T> get(UUID id) {
        return Optional.ofNullable(cache.get(id));
    }

    /**
     * Поміщає сутність у кеш.
     *
     * <p>Викликається репозиторієм після кожного SELECT з БД. Якщо сутність вже є в кеші —
     * перезаписує (актуалізує стан).
     *
     * @param id первинний ключ
     * @param entity сутність для кешування
     */
    public void put(UUID id, T entity) {
        cache.put(id, entity);
    }

    /**
     * Видаляє сутність з кешу.
     *
     * <p>Викликається після видалення з БД або при інвалідації кешу.
     *
     * @param id первинний ключ
     */
    public void remove(UUID id) {
        cache.remove(id);
    }

    /**
     * Перевіряє наявність сутності в кеші.
     *
     * @param id первинний ключ
     * @return true якщо сутність є в кеші
     */
    public boolean contains(UUID id) {
        return cache.containsKey(id);
    }

    /**
     * Очищає весь кеш.
     *
     * <p>Викликається після {@code commit()} або {@code rollback()} у Unit of Work.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Повертає кількість об'єктів у кеші.
     *
     * @return розмір кешу
     */
    public int size() {
        return cache.size();
    }
}