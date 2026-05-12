package com.bookstore.presentation.controller;

import com.bookstore.domain.entity.User;
import com.bookstore.presentation.SceneManager;
import com.bookstore.presentation.SessionManager;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Контролер головного вікна (MVC - Controller).
 *
 * <p>Управляє навігацією між розділами через sidebar. Завантажує дочірні FXML у {@code contentArea}
 * (StackPane) не перевантажуючи сцену.
 */
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label usernameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Button btnClients;
    @FXML private Button btnBooks;
    @FXML private Button btnSales;
    @FXML private Button btnCategories;
    @FXML private Button btnAuthors;
    @FXML private Button btnSuppliers;
    @FXML private Button btnSupplies;

    /** Ініціалізація після завантаження FXML. Відображає дані поточного користувача. */
    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            usernameLabel.setText(user.getUsername());
            userRoleLabel.setText(getRoleLabel(user.getRole()));
        }

        // за замовчуванням відкриваємо клієнтів
        showClients();
    }

    @FXML
    private void showClients() {
        setActive(btnClients);
        loadContent("/view/Clients.fxml");
    }

    @FXML
    private void showBooks() {
        setActive(btnBooks);
        loadContent("/view/books.fxml");
    }

    @FXML
    private void showSales() {
        setActive(btnSales);
        loadContent("/view/sales.fxml");
    }

    @FXML
    private void showCategories() {
        setActive(btnCategories);
        loadContent("/view/categories.fxml");
    }

    @FXML
    private void showAuthors() {
        setActive(btnAuthors);
        loadContent("/view/authors.fxml");
    }

    @FXML
    private void showSuppliers() {
        setActive(btnSuppliers);
        loadContent("/view/suppliers.fxml");
    }

    @FXML
    private void showSupplies() {
        setActive(btnSupplies);
        loadContent("/view/supplies.fxml");
    }

    /** Виходить з системи та повертається до вікна входу. */
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.getInstance().showLogin();
    }

    /**
     * Завантажує дочірній FXML у contentArea.
     *
     * @param fxmlPath шлях до FXML файлу
     */
    private void loadContent(String fxmlPath) {
        try {
            var resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                // Тепер ми точно знатимемо, який шлях "битий"
                System.err.println("[DEBUG] Ресурс не знайдено: " + fxmlPath);

                Label errorLabel = new Label("❌ Помилка: Не знайдено " + fxmlPath);
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
                contentArea.getChildren().setAll(errorLabel);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Node content = loader.load();
            contentArea.getChildren().setAll(content);

        } catch (IOException e) {
            // КРИТИЧНО: виводимо помилку в консоль, щоб зрозуміти, ЧОМУ не завантажився FXML
            e.printStackTrace();

            Label errorLabel = new Label("❌ Помилка завантаження: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
            contentArea.getChildren().setAll(errorLabel);
        }
    }

    /**
     * Позначає активну кнопку sidebar.
     *
     * @param activeBtn активна кнопка
     */
    private void setActive(Button activeBtn) {
        //Скидаємо всі
        for (Button btn : new Button[] {
            btnClients, btnBooks, btnSales,
            btnCategories, btnAuthors, btnSuppliers, btnSuppliers}) {
    btn.getStyleClass().remove("sidebar-item-active");
        }
        //Позначаємо активну
        if (!activeBtn.getStyleClass().contains("sidebar-item-active")) {
            activeBtn.getStyleClass().add("sidebar-item-active");
    }
    }

    private String getRoleLabel(User.Role role) {
        return switch (role) {
            case admin -> "Адміністратор";
            case cashier -> "Касир";
            case manager -> "Менеджер";
        };
    }
}
