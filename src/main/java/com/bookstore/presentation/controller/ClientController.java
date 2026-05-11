package com.bookstore.presentation.controller;

import com.bookstore.application.ServiceFactory;
import com.bookstore.application.contract.ClientService;
import com.bookstore.domain.entity.Client;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Контролер управління клієнтами (MVC — Controller).
 *
 * <p>Реалізує повний CRUD для клієнтів: перегляд таблиці, пошук, додавання, редагування,
 * видалення. Асинхронне завантаження даних через {@link Task}.
 */
public class ClientController {

    // ===== Таблиця =====
    @FXML private TableView<Client> clientTable;
    @FXML private TableColumn<Client, String> colFirstName;
    @FXML private TableColumn<Client, String> colLastName;
    @FXML private TableColumn<Client, String> colPhone;
    @FXML private TableColumn<Client, String> colEmail;
    @FXML private TableColumn<Client, Void> colActions;

    // ===== Заголовок =====
    @FXML private TextField searchField;
    @FXML private Label countBadge;

    // ===== Панель редагування =====
    @FXML private VBox editPanel;
    @FXML private Label editPanelTitle;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label phoneError;
    @FXML private Label emailError;
    @FXML private Button saveButton;

    private final ClientService clientService =
          ServiceFactory.getInstance("bookstore.db").clientService();

    private final ObservableList<Client> clientData = FXCollections.observableArrayList();

    /** Поточний клієнт для редагування (null = новий). */
    private Client editingClient = null;

    /** Ініціалізація після завантаження FXML. */
    @FXML
    public void initialize() {
        setupTable();
        loadClients();
    }

    // =========================================================
    // Налаштування таблиці
    // =========================================================

    private void setupTable() {
        // Прив'язка колонок до властивостей
        colFirstName.setCellValueFactory(
              data -> new SimpleStringProperty(data.getValue().getFirstName()));
        colLastName.setCellValueFactory(
              data -> new SimpleStringProperty(data.getValue().getLastName()));
        colPhone.setCellValueFactory(
              data -> new SimpleStringProperty(
                    data.getValue().getPhone() != null ? data.getValue().getPhone() : "—"));
        colEmail.setCellValueFactory(
              data -> new SimpleStringProperty(
                    data.getValue().getEmail() != null ? data.getValue().getEmail() : "—"));

        // Колонка дій — кнопки Редагувати / Видалити
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏");
            private final Button deleteBtn = new Button("🗑");
            private final HBox box = new HBox(4, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-icon");
                deleteBtn.getStyleClass().add("btn-icon");
                editBtn.setStyle("-fx-text-fill: #7c6af7;");
                deleteBtn.setStyle("-fx-text-fill: #f05a5a;");

                editBtn.setOnAction(e -> {
                    Client client = getTableView().getItems().get(getIndex());
                    handleEdit(client);
                });
                deleteBtn.setOnAction(e -> {
                    Client client = getTableView().getItems().get(getIndex());
                    handleDelete(client);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        clientTable.setItems(clientData);
    }

    // =========================================================
    // Завантаження даних
    // =========================================================

    private void loadClients() {
        Task<List<Client>> task = new Task<>() {
            @Override
            protected List<Client> call() {
                String query = searchField.getText();
                return query == null || query.isBlank()
                      ? clientService.findAll()
                      : clientService.search(query);
            }
        };

        task.setOnSucceeded(e -> {
            clientData.setAll(task.getValue());
            countBadge.setText(String.valueOf(clientData.size()));
        });

        new Thread(task).start();
    }

    // =========================================================
    // Пошук
    // =========================================================

    @FXML
    private void handleSearch() {
        loadClients();
    }

    // =========================================================
    // CRUD операції
    // =========================================================

    /** Відкриває панель для додавання нового клієнта. */
    @FXML
    private void handleAdd() {
        editingClient = null;
        editPanelTitle.setText("Новий клієнт");
        saveButton.setText("Зберегти");
        clearForm();
        showEditPanel(true);
    }

    /** Відкриває панель для редагування клієнта. */
    private void handleEdit(Client client) {
        editingClient = client;
        editPanelTitle.setText("Редагувати клієнта");
        saveButton.setText("Оновити");

        firstNameField.setText(client.getFirstName());
        lastNameField.setText(client.getLastName());
        phoneField.setText(client.getPhone() != null ? client.getPhone() : "");
        emailField.setText(client.getEmail() != null ? client.getEmail() : "");
        clearErrors();
        showEditPanel(true);
    }

    /** Видаляє клієнта після підтвердження. */
    private void handleDelete(Client client) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                clientService.delete(client.getId());
                return null;
            }
        };

        task.setOnSucceeded(e -> loadClients());
        task.setOnFailed(e -> showFieldError(firstNameError,
              "Помилка видалення: " + task.getException().getMessage()));

        new Thread(task).start();
    }

    /** Зберігає або оновлює клієнта. */
    @FXML
    private void handleSave() {
        clearErrors();

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        // UI валідація
        boolean hasErrors = false;
        if (firstName.isEmpty()) {
            showFieldError(firstNameError, "Ім'я не може бути порожнім");
            hasErrors = true;
        }
        if (lastName.isEmpty()) {
            showFieldError(lastNameError, "Прізвище не може бути порожнім");
            hasErrors = true;
        }
        if (hasErrors) return;

        saveButton.setDisable(true);

        Task<Client> task = new Task<>() {
            @Override
            protected Client call() {
                if (editingClient == null) {
                    // CREATE
                    return clientService.create(
                          firstName, lastName,
                          phone.isEmpty() ? null : phone,
                          email.isEmpty() ? null : email);
                } else {
                    // UPDATE
                    return clientService.update(
                          editingClient.getId(),
                          firstName, lastName,
                          phone.isEmpty() ? null : phone,
                          email.isEmpty() ? null : email);
                }
            }
        };

        task.setOnSucceeded(e -> {
            saveButton.setDisable(false);
            showEditPanel(false);
            loadClients();
        });

        task.setOnFailed(e -> {
            saveButton.setDisable(false);
            showFieldError(firstNameError, task.getException().getMessage());
        });

        new Thread(task).start();
    }

    /** Скасовує редагування. */
    @FXML
    private void handleCancel() {
        showEditPanel(false);
        clearForm();
    }

    // =========================================================
    // Допоміжні методи
    // =========================================================

    private void showEditPanel(boolean visible) {
        editPanel.setVisible(visible);
        editPanel.setManaged(visible);
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        emailField.clear();
        clearErrors();
    }

    private void clearErrors() {
        hideFieldError(firstNameError);
        hideFieldError(lastNameError);
        hideFieldError(phoneError);
        hideFieldError(emailError);
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
}