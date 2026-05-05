package com.bookstore.infrastructure.persistence.util;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Virtual Proxy для відкладеного завантаження колекції (Lazy Loading).
 *
 * <p>Є прозорим замінником {@link List} - клієнтський код не знає що працює з Proxy,
 * а не зі звичайним списком. Реальне завантаження відбувається лише при першому зверненні до даних.
 *
 * <p>Розв'язує проблему N+1 Query: при завантаженні 100 книг не виконуються 100 додаткових
 * SELECT-запитів для авторів - автори завантажуються лише коли вони реально потрібні.
 *
 * <p>Приклад використання репозиторії:
 *
 * <pre>{@code
 * // У mapRow() JdbcBookRepository:
 *      .id(...)
 *      .title(...)
 *      .authors(new LazyList<>(() -> authorRepository.findByBookId(bookId)))
 *      .build();
 *
 * // Автори НЕ завантажуються при створенні книги.
 * // Завантажування відбувається лише тут:
 * List<Author> authors = book.getAuthors(); // <- перше звернення -> SELECT
 * }</pre>
 *
 * @param <T> тип елементів колекції
 */
public class LazyList<T> extends AbstractList<T> {

    /** Фабрика для завантаження даних - виконується лише один раз. */
    private final Supplier<List<T>> loader;

    /** Завантажені дані. null означає що дані ще не завантажені. */
    private List<T> loaded = null;

    /** Прапорець ініціалізації - уникає повторного виклику loader. */
    private boolean initialized = false;

    /**
     * Створює LazyList з функцією завантаження.
     *
     * @param loader функція що виконує реальний SELECT з БД
     */
    public LazyList(Supplier<List<T>> loader) {
        this.loader = loader;
    }
    /**
     * Повертає елемент за індексом. Ініціює завантаження при першому виклику.
     *
     * @param index індекс елемента
     * @return елемент
     */
    @Override
    public T get(int index) {
        return delegate().get(index);
    }

    /**
     * Повертає розмір колекції. Ініціює завантаження при першому виклику.
     *
     * @return кількість елементів
     */
    @Override
    public int size() {
        return delegate().size();
    }

    /**
     * Перевіряє чи вже завантажені дані.
     *
     * <p>Дозволяє перевірити стан без ініціації завантаження.
     *
     * @return true якщо дані вже завантажені
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Повертає делегат — ініціює завантаження якщо ще не відбулося. Патерн Proxy: прозоре
     * делегування до реального об'єкта.
     */
    private List<T> delegate() {
        if (!initialized) {
            loaded = loader.get();
            initialized = true;
        }
        return loaded;
    }
}
