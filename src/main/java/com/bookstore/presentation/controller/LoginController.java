package com.bookstore.presentation.controller;

import com.bookstore.application.ServiceFactory;
import com.bookstore.domain.entity.User;
import com.bookstore.presentation.SceneManager;
import com.bookstore.presentation.SessionManager;
import java.util.Optional;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/**
 * Контролер вікна входу до системи (MVC - controller).
 *
 * <p>Обробляє події від View (login.fxml), звертається до Model (AuthService) для
 * аутентифікації. Асинхронно виконує запит до БД, щоб не блокувати JavaFX UI потік.
  */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    /**
     * Ініціалізація після завантаження FXML. Налаштовує обробники клавіатури.
     */
    @FXML
    public void initialize(){
        //Enter у полі логіна -> фокус на пароль
        passwordField.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });

        // Скидаємо помилку при зміні тексту
        usernameField.textProperty().addListener((o, old, val) -> clearError());
        passwordField.textProperty().addListener((o, old, val) -> clearError());
    }

    /**
     * Обробляє спробу входу. Виконується асинхроннно через {@link Task} щоб не блокувати UI.
     */
    @FXML
    private void handleLogin(){
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Валідація на рівні UI
        if (username.isEmpty()) {
            showError("Введіть логін");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Введіть пароль");
            passwordField.requestFocus();
            return;
        }

        // Асинхронний запит до БД
        loginButton.setDisable(true);
        loginButton.setText("Вхід...");

        Task<Optional<User>> task = new Task<>() {
            @Override
            protected Optional<User> call() {
                return ServiceFactory.getInstance("bookstore.db")
                      .authService()
                      .login(username, password);
            }
        };

        task.setOnSucceeded(e -> {
            Optional<User> result = task.getValue();
            if (result.isPresent()) {
                SessionManager.getInstance().setCurrentUser(result.get());
                SceneManager.getInstance().showMain();
            } else {
                showError("Невірний логін або пароль");
                passwordField.clear();
                passwordField.requestFocus();
                loginButton.setDisable(false);
                loginButton.setText("Увійти");
            }
        });
        task.setOnFailed(e -> {
            showError("Помилка з'єднання з БД");
            loginButton.setDisable(false);
            loginButton.setText("Увійти");
        });

        new Thread(task).start();
    }

    /** Переходить на сторінку реєстрації. */
    @FXML
    private void handleGoToRegister() {
        SceneManager.getInstance().showRegister();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}

