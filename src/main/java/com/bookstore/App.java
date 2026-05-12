package com.bookstore;

import com.bookstore.application.ServiceFactory;
import com.bookstore.presentation.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.flywaydb.core.Flyway;
import java.io.File;

public class App extends Application {

    private static final String DB_FILE_NAME = "bookstore.db";

    private static final String DB_PATH = System.getProperty("user.dir")
          + File.separator + DB_FILE_NAME;
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    /**
     * Точка входу Java - запускає JavaFX Application.
     *
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Ініціалізація JavaFX додатку. Виконується перед {@link #start(Stage)}.
     *
     * <p>Запускаємо Flyway та ServiceFactory тут - не в start() - щоб не блокувати JavaFX потік.
     */
    @Override
    public void init() {
        // Виведемо шлях у консоль, щоб ти міг знайти файл бази під час тестів
        System.out.println("[Main] Шлях до бази даних: " + DB_PATH);

        // 1. Flyway
        Flyway flyway = Flyway.configure()
              .dataSource(DB_URL, null, null)
              .locations("classpath:db/migration")
              .baselineOnMigrate(true)
              .load();
        flyway.migrate();

        // 2. ServiceFactory - передаємо ПОВНИЙ шлях
        ServiceFactory.getInstance(DB_PATH);
    }

    /**
     * Запуск JavaFX Application Thread. Ініціалізує SceneManager та відображає вікно входу.
     *
     * @param primaryStage головний стейдж
     */
    @Override
    public void start(Stage primaryStage) {
        SceneManager sceneManager = SceneManager.getInstance();
        sceneManager.init(primaryStage);
        sceneManager.showLogin();
    }

    /**
     * Очищення ресурсів при закритті додатка.
     *
     * <p> Закриває пул з'єднань
     */
    @Override
    public void stop() {
        System.out.println("[Main] Закриття додатку...");
    }
}