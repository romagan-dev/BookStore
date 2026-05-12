package com.bookstore.presentation;

import com.bookstore.domain.entity.User;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Менеджер сцен — централізована навігація між вікнами JavaFX.
 *
 * <p>Патерни: Singleton, Facade. Приховує деталі завантаження FXML та перемикання сцен.
 */
public class SceneManager {

    private static volatile SceneManager instance;
    private Stage primaryStage;

    private static final String CSS_PATH = "css/main.css";
    private static final double DEFAULT_WIDTH = 1200;
    private static final double DEFAULT_HEIGHT = 750;
    private static final double AUTH_WIDTH = 480;
    private static final double AUTH_HEIGHT = 600;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) {
            synchronized (SceneManager.class) {
                if (instance == null) {
                    instance = new SceneManager();
                }
            }
        }
        return instance;
    }

    /**
     * Ініціалізує менеджер зі стейджем JavaFX.
     *
     * @param stage головний стейдж додатку
     */
    public void init(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("BookStore — Система обліку");
        stage.setMinWidth(800);
        stage.setMinHeight(500);
    }

    /** Відкриває вікно входу. */
    public void showLogin() {
        loadScene("view/Login.fxml", AUTH_WIDTH, AUTH_HEIGHT, false);
        primaryStage.setResizable(false);
    }

    /** Відкриває вікно реєстрації. */
    public void showRegister() {
        loadScene("view/Register.fxml", AUTH_WIDTH, AUTH_HEIGHT, false);
        primaryStage.setResizable(false);
    }

    /** Відкриває головне вікно після входу. */
    public void showMain() {
        User user = SessionManager.getInstance().getCurrentUser();
        primaryStage.setTitle("BookStore — " + (user != null ? user.getUsername() : ""));
        primaryStage.setResizable(true);
        loadScene("view/Main.fxml", DEFAULT_WIDTH, DEFAULT_HEIGHT, true);
    }

    /**
     * Завантажує FXML сцену і відображає її.
     *
     * <p>Використовує ClassLoader для завантаження ресурсів — це працює як в IDE,
     * так і в Fat JAR та після встановлення через jpackage.
     *
     * @param fxmlPath шлях до FXML файлу (БЕЗ початкового слешу!)
     * @param width ширина вікна
     * @param height висота вікна
     * @param centered чи центрувати вікно
     */
    private void loadScene(String fxmlPath, double width, double height, boolean centered) {
        try {
            // ClassLoader — єдиний надійний спосіб для Fat JAR та jpackage
            var resource = Thread.currentThread()
                  .getContextClassLoader()
                  .getResource(fxmlPath);

            if (resource == null) {
                throw new RuntimeException(
                      "FXML файл не знайдено: " + fxmlPath
                            + "\nПеревір структуру папок у resources/");
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);

            // CSS через ClassLoader — той самий підхід
            var cssResource = Thread.currentThread()
                  .getContextClassLoader()
                  .getResource(CSS_PATH);

            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("[SceneManager] CSS не знайдено: " + CSS_PATH);
            }

            primaryStage.setScene(scene);

            if (centered) {
                primaryStage.centerOnScreen();
            }
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Помилка завантаження сцени: " + fxmlPath, e);
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}