package com.bookstore.infrastructure.persistence;

import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.PoolConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Базовий клас для інтеграційних тестів репозиторіїв.
 *
 * <p>Налаштовує H2 in-memory БД перед кожним тестом та очищає після. Кожен тест отримує чисту БД
 * — гарантія ізоляції між тестами.
 *
 * <p>Патерн: Template Method — підкласи отримують готовий {@link #connectionManager} та
 * перевизначають {@link #setUp()} для ініціалізації своїх репозиторіїв.
 */
public abstract class BaseRepositoryTest {

    /** H2 URL: нова in-memory БД для кожного тестового класу. */
    private static final String H2_URL =
          "jdbc:h2:mem:bookstore_test;DB_CLOSE_DELAY=-1";

    /** Менеджер з'єднань для тестів. */
    protected ConnectionManager connectionManager;

    /**
     * Ініціалізує H2 БД та виконує DDL перед кожним тестом.
     *
     * <p>Порядок: 1) створити ConnectionManager для H2 2) виконати DDL (створити таблиці) 3)
     * викликати setUp() підкласу
     */
    @BeforeEach
    void initDatabase() throws Exception {
        // Singleton скидаємо перед кожним тестом
        ConnectionManager.resetInstance();

        connectionManager =
              ConnectionManager.getInstance(
                    new PoolConfig(H2_URL, 1, 5, 3000L));

        executeSql(loadResource("ddl_h2.sql"));
        setUp();
    }

    /**
     * Очищає БД після кожного теста — дропаємо всі таблиці.
     *
     * <p>H2 підтримує DROP ALL OBJECTS — зручно для повного скидання схеми.
     */
    @AfterEach
     void tearDown() throws Exception {
        executeSql("DROP ALL OBJECTS");
        connectionManager.close();
    }

    /**
     * Хук для підкласів — ініціалізація репозиторіїв після створення БД.
     *
     * <p>Перевизначай у підкласах для створення репозиторіїв.
     */
    protected void setUp() throws Exception {}

    /**
     * Виконує SQL рядок через активне з'єднання.
     *
     * @param sql SQL для виконання
     */
    protected void executeSql(String sql) throws SQLException {
        try (Connection conn = connectionManager.getConnection();
              Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Завантажує SQL файл з ресурсів тестів.
     *
     * @param fileName назва файлу в src/test/resources
     * @return вміст файлу як рядок
     */
    protected String loadResource(String fileName) {
        try (InputStream is =
              getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new IllegalArgumentException("Файл не знайдено: " + fileName);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Помилка читання файлу: " + fileName, e);
        }
    }
}