package com.bookstore.application.impl;

import com.bookstore.application.contract.AuthService;
import com.bookstore.domain.entity.User;
import com.bookstore.infrastructure.mail.EmailService;
import com.bookstore.infrastructure.persistence.impl.JdbcUserRepository;
import com.bookstore.infrastructure.security.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;

/**
 * Реалізація сервісу аутентифікації та реєстрації
 *
 * <p>Патерни: Dependency Injection (залежності через конструктор), Facade (приховує складність,
 * хешування, валідації, збереження).
 */
public class AuthServiceImpl implements AuthService {

    private final JdbcUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Конструктор з Dependency Injection.
     *
     * @param userRepository репозиторій користувачів
     * @param passwordEncoder сервіс хешування паролів
     * @param emailService сервіс електронної пошти
     */
    public AuthServiceImpl(
          JdbcUserRepository userRepository,
          PasswordEncoder passwordEncoder,
          EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Реєструє нового користувача.
     *
     * <p>Алгоритм 1) Валідація полів 2) Перевірка унікальності username та email
     * 3) Хешування пароля 4) Збереження у БД 5) Надсилання вітального листа
     */
    @Override
    public User register(String username, String email, String rawPassword, User.Role role) {
        // 1. Валідація
        validateUsername(username);
        validateEmail(email);
        validatePassword(rawPassword);

        // 2. Унікальність username
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Користувач з логіном '" + username + "' вже існує");
        }

        // 3. Унікальність email
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                  "Користувач з email '" + email + "' вже існує");
        }

        // 4. Хешування паролю (інфраструктура!)
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // 5. Створення та збереження
        User user = User.builder()
              .id(UUID.randomUUID())
              .username(username)
              .email(email)
              .passwordHash(hashedPassword)
              .role(role)
              .build();

        userRepository.save(user);

        // 6. Вітальний лист (інфраструктура!)
        emailService.sendWelcomeEmail(email, username);

        return user;
    }

    /**
     * Аутентифікація користувача.
     *
     * <p>Алгоритм 1) Пошук за username 2) Перевірка пароля через bcrypt
     * 3) Повернення користувача або empty
     */
    @Override
    public Optional<User> login(String username, String rawPassword) {
        if (username == null || username.isEmpty()) {
            return Optional.empty();
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByUsername(username)
              .filter(user -> passwordEncoder.verify(rawPassword, user.getPasswordHash()));
    }

    /** Змінює пароль користувача після перевірки старого. */
    @Override
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
              .orElseThrow(() -> new IllegalArgumentException("Користувача не знайдено"));

        if (!passwordEncoder.verify(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Старий пароль невірний");
        }

        validatePassword(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.update(user);

        emailService.sendPassswordChengedEmail(user.getEmail());
    }
    // =========================================================
    // Приватні методи валідації
    // =========================================================

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Логін не може бути порожнім");
        }
        if (username.length() < 3) {
            throw new IllegalArgumentException("Логін має містити мінімум 3 символи");
        }
        if (username.length() > 50) {
            throw new IllegalArgumentException("Логін не може бути довшим за 50 символів");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException(
                  "Логін може містити лише латинські літери, цифри та '_'");
        }
    }

    private void validateEmail(String email) {
        if (!emailService.isValidEmail(email)) {
            throw new IllegalArgumentException("Невалідна email адреса: " + email);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль не може бути порожнім");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Пароль має містити мінімум 6 символів");
        }
    }
}
