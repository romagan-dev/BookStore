package com.bookstore.infrastructure.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Інфраструктурний сервіс для хешування та перевірки паролів.
 *
 * <p>Використовує алгоритм BCrypt - industry standart для зберігання паролів.
 * BCrypt автоматично генерує сіль і включає її в хеш, що захищає від rainbow table атак.
 *
 * <p>Відповідальність: інфраструктура (не бізнес-логіка) - згідно з вимогами завдання
 */
public class PasswordEncoder {

    /**
     * Складність хешування (2^cost раундів). 10 - баланс між безпекою та швидкістю
     */
    private static final int BCRYPT_COST = 10;

    /**
     * Хешує пароль за допомогою BCrypt.
     *
     * @param rawPassword пароль у відкритому вигляді
     * @return BCrypt хеш для зберігання у БД
     * @throws IllegalArgumentException якщо пароль порожній
     */
    public String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Пароль не може бути порожнім");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Перевіряє відповідність пароля його BCrypt хешу.
     *
     * @param rawPassword    пароль у відкритому вигляді
     * @param hashedPassword збережений BCrypt хеш
     * @return {@code true} якщо пароль відповідає хешу
     */
    public boolean verify(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
