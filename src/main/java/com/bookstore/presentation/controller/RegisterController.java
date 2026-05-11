package com.bookstore.presentation.controller;

import com.bookstore.application.ServiceFactory;
import com.bookstore.domain.entity.User;
import com.bookstore.presentation.SceneManager;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * контролер вікна реєстрації (MVC - Controller).
 *
 * <p>Обробляє реєстрацію нового користувача. Валідація відбувається на двох рівнях:
 * UI (миттєвий зворотний зв'язок) та сервісному (бізнес-логіка).
 */
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<User.Role> roleComboBox;
    @FXML private Label usernameError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label generalError;
    @FXML private Button registerButton;

    /** Ініціалізація після завантаження FXML. Заповнює ComboBox ролей. */
    @FXML
    public void initialize(){
        roleComboBox.setItems(FXCollections.observableArrayList(User.Role.values()));
        roleComboBox.setValue(User.Role.cashier);

        // Миттєва валідація при зміні полів
        usernameField.textProperty().addListener((o, old, val) -> validateUsername(val));
        emailField.textProperty().addListener((o,old,val) -> validateEmail(val));
        passwordField.textProperty().addListener((o, old, val) -> validatePassword(val));
        confirmPasswordField.textProperty().addListener((o, old, val) -> validateConfirmPassword(val));
    }

    /** Обробляє реєстрацію нового користувача. */
    @FXML
    private void handleRegister() {
        clearAllErrors();

        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        User.Role role = roleComboBox.getValue();

        // UI валідація
        boolean hasErrors = false;
        if (!validateUsername(username)) hasErrors = true;
        if (!validateEmail(email)) hasErrors = true;
        if (!validatePassword(password)) hasErrors = true;
        if (!validateConfirmPassword(confirmPassword)) hasErrors = true;
        if (!password.equals(confirmPassword)) {
            showFieldError(confirmPasswordError, "Паролі не збігаються");
            hasErrors = true;
        }
        if (role == null) {
            showGeneralError("Оберіть роль");
            hasErrors = true;
        }
        if (hasErrors) return;

        // Асинхронна реєстрація
        registerButton.setDisable(true);
        registerButton.setText("Реєстрація...");

        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return ServiceFactory.getInstance("bookstore.db")
                      .authService()
                      .register(username, email, password, role);
            }
        };

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showGeneralError(ex.getMessage());
            registerButton.setDisable(false);
            registerButton.setText("Зареєструватися");
        });
        task.setOnSucceeded(e -> {
            registerButton.setDisable(false);
            registerButton.setText("Зареєструватися");

            handleGoToLogin();
        });

        new Thread(task).start();
    }
    /** Повертається до вікна входу. */
    @FXML
    private void handleGoToLogin() {
        SceneManager.getInstance().showLogin();
    }

    // =========================================================
    // Валідація полів (миттєвий зворотний зв'язок)
    // =========================================================

    private boolean validateUsername(String value) {
        if (value == null || value.isBlank()) {
            showFieldError(usernameError, "Логін не може бути порожнім");
            return false;
        }
        if (value.length() < 3) {
            showFieldError(usernameError, "Мінімум 3 символи");
            return false;
        }
        if (!value.matches("^[a-zA-Z0-9_]+$")) {
            showFieldError(usernameError, "Лише латинські літери, цифри та '_'");
            return false;
        }
        hideFieldError(usernameError);
        return true;
    }

    private boolean validateEmail(String value) {
        if (value == null || value.isBlank()) {
            showFieldError(emailError, "Email не може бути порожнім");
            return false;
        }
        if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showFieldError(emailError, "Невалідний формат email");
            return false;
        }
        hideFieldError(emailError);
        return true;
    }

    private boolean validatePassword(String value) {
        if (value == null || value.length() < 6) {
            showFieldError(passwordError, "Мінімум 6 символів");
            return false;
        }
        hideFieldError(passwordError);
        return true;
    }

    private boolean validateConfirmPassword(String value) {
        if (!value.equals(passwordField.getText())) {
            showFieldError(confirmPasswordError, "Паролі не збігаються");
            return false;
        }
        hideFieldError(confirmPasswordError);
        return true;
    }

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideFieldError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    private void showGeneralError(String message) {
        generalError.setText(message);
        generalError.setVisible(true);
        generalError.setManaged(true);
    }

    private void clearAllErrors() {
        hideFieldError(usernameError);
        hideFieldError(emailError);
        hideFieldError(passwordError);
        hideFieldError(confirmPasswordError);
        hideFieldError(generalError);
    }
}
