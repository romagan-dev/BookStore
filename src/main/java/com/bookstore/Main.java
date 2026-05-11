package com.bookstore;

import com.bookstore.application.ServiceFactory;
import com.bookstore.presentation.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.Flyway;

public class Main extends Application {

    private static final String DB_PATH = "bookstore.db";
    private static final String DB_URL = "jdbc:sqlite:bookstore.db";

    /**
     * Точка входу Java - запускає JavaFX Application.
     *
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        launch(args);
    }

    Flyway flyway = Flyway.configure()
          .dataSource(DB_URL, null, null)
          .locations("classpath:db/migration")
          .baselineOnMigrate(true)
          .outOfOrder(true)
          .validateOnMigrate(false) // ← додай це!
          .load();
    /**
     * Ініціалізація JavaFX додатку. Виконується перед {@link #start(Stage)}.
     *
     * <p>Запускаємо Flyway та ServiceFactory тут - не в start() - щоб не блокувати JavaFX потік.
     */
    @Override
    public void init() {
        // 1. Flyway - міграції БД
        System.out.println("[Main] Запуск Flyway міграцій...");
        Flyway flyway = Flyway.configure()
              .dataSource(DB_URL, null, null)
              .locations("classpath:db/migration")
              .baselineOnMigrate(true)
              .load();
        flyway.migrate();
        System.out.println("[Main] Flyway міграції завершено");

        // 2. ServiceFactory - ініціалізація всіх сервісів та репозиторіїв
        System.out.println("[Main] Ініціалізація сервісів...");
        ServiceFactory.getInstance(DB_PATH);
        System.out.println("[Main] Сервіси готові");
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