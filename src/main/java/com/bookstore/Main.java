package com.bookstore;

import org.flywaydb.core.Flyway;

public class Main {
    public static void main(String[] args) {
        Flyway flyway = Flyway.configure()
              .dataSource("jdbc:sqlite:bookstore.db", null, null)
              .locations("classpath:db/migration")
              .load();
        flyway.migrate();
        System.out.println("БД створена успішно!");
    }
}