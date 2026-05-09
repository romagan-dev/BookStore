package com.bookstore.application.contract;

import com.bookstore.domain.entity.Book;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт сервісу для роботи з книгами.
 */
public interface BookService {

    /**
     * Повертає вс книги
     *
     * @return список книг
     */
    List<Book> findAll();

    /**
     * Знаходить книгу за id.
     *
     * @param id
     * @return Optional з книгою
     */
    Optional<Book> findById(UUID id);

    /**
     * Пошук книги за назвою
     *
     * @param query рядок пошуку
     * @return список знайдених книг
     */
    List<Book> search(String query);

    /**
     * Пошук книги використовуючи категорію
     *
     * @param categoryId
     * @return список знайдених книг
     */
    List<Book> findByCategory(UUID categoryId);

    /**
     * Пошук книги в наявності
     *
     * @return Чи книга в наявності, чи ні.
     */
    List<Book> findInStock();

    /**
     * Пошук найдорожчих книг
     *
     * @param maxPrice найвища ціна книги
     * @return список найдорощих книг
     */
    List<Book> findByMaxPrice(BigDecimal maxPrice);

    /**
     * Створює нову книгу
     *
     * @param title назва книги
     * @param isbn ідентифікатор видавництва книги
     * @param price ціна книги
     * @param stockQuantity кількість книг в наявності
     * @param description опис
     * @param categoryId UUID категорії
     * @return створена книга
     * @throws IllegalArgumentException якщо книга вже існує, або дані невалідні.
     */
    Book create(String title, String isbn, BigDecimal price, int stockQuantity,
          String description, UUID categoryId);

    /**
     * Оновлює дані книги
     *
     * @param id UUID книги
     * @param title назва книги
     * @param isbn ідентифікатор видавництва книги
     * @param price ціна книги
     * @param stockQuantity кількість книг в наявності
     * @param description опис
     * @param categoryId UUID категорії
     * @return оновлену книгу
     * @throws IllegalArgumentException якщо дані не валідні, або книга не знайдена
     */
    Book update(UUID id, String title, String isbn, BigDecimal price, int stockQuantity, String description, UUID categoryId);

    /**
     * Видаляє книгу
     *
     * @param id UUID книги
     * @throws IllegalArgumentException якщо книга не знайдена
     */
    void delete(UUID id);

    /**
     * Додає автора до книги
     *
     * @param bookId UUID книги
     * @param authorId UUID автора
     * @throws IllegalArgumentException якщо книгу або автора не знайдено
     */
    void addAuthor(UUID bookId, UUID authorId);

    /**
     * Видаляє автора книги
     *
     * @param bookId UUID книги
     * @param authorId UUID автора
     * @throws IllegalArgumentException якщо автора або книгу не знайдено.
     */
    void removeAuthor(UUID bookId, UUID authorId);
}
