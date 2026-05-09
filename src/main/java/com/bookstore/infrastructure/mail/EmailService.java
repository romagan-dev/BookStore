package com.bookstore.infrastructure.mail;

/**
 * Інфраструктурний сервіс для надсилання електронних листів
 *
 * <p>Відповідальність: інфраструктура - перевірка існування email та надсилання сповіщень.
 * Використовується сервісним шаром для перевірки унікальності email при реєстрації.
 *
 * <p>Поточна реалізація - заглушка (stub). У production замінити на JavaMail або SendGrid.
 */

public class EmailService {

    /**
     * Перевіряє чи є email адреса валідною за форматом
     *
     * @param email email адреса для перевірки
     * @return {@code true} якщо формат валідний
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Надсилає вітальний лист після реєстрації.
     *
     * <p>Поточна реалізація логує в консоль. У production замінити на реальне надсилання.
     *
     * @param email адреса отримувача
     * @param username  логін нового користувача
     */
    public void sendWelcomeEmail(String email, String username) {
        //TODO: замінити на реальну відправку через JavaMail
        System.out.printf("[EmailService] Вітальний лист надіслано: @s -> @s@n", username, email );
    }

    /**
     * Надсилає сповіщення про зміну пароля.
     *
     * @param email адреса отримувача
     */
    public void sendPassswordChengedEmail(String email) {
        System.out.printf("[EmailService] Сповіщення про зміну паролю надіслано -> @s@n", email);
    }
}
