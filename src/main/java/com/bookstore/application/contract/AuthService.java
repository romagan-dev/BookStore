package com.bookstore.application.contract;

import com.bookstore.domain.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт сервісу аутентифікації та реєстрації користувачів.
 */
public interface AuthService {

    /**
     * Реєструє нового користувача.
     *
     * @param username логін
     * @param email email адреса
     * @param rawPassword пароль у відкритому вигляді
     * @param role роль користувача
     * @return зареєстрований користувач
     * @throws IllegalArgumentException якщо дані невалідні або email вже зайнятий
     */
    User register (String username, String email, String rawPassword, User.Role role);

    /**
     * Аутентифікація користувача за логіном та паролем.
     *
     * @param username логін
     * @param rawPassword пароль у відкритому вигляді
     * @return Optional з користувачем або empty якщо дані невірні
     */
    Optional<User> login (String username, String rawPassword);

    /**
     * Змінює пароль корситувача.
     *
     * @param userId UUID користувача
     * @param oldPassword старий пароль
     * @param newPassword новий пароль
     * @throws IllegalArgumentException якщо старий пароль невірний
     */
    void changePassword (UUID userId, String oldPassword, String newPassword);

}
