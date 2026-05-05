package com.bookstore.infrastructure.persistence.util;

/**
 * Виняток для помилок роботи з базою даних.
 *
 * <p>Обгоротає перевірені {@link java.sql.SQLException} у неперевірений виняток,
 * щоб не засмічувати сигнатури методів у шарі даних.
 */

public class DatabaseException extends RuntimeException {

    /**
     * Створює виняток з повідомленням та причиною
     *
     * @param message опис помилки
     * @param cause оригінальна причина
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Створює виняток лише з повідомленням.
     *
     * @param message опис помилки
     */
    public DatabaseException(String message) {
        super(message);
    }
}
