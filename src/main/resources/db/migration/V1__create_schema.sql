-- =============================================================================
-- DDL.sql — Схема бази даних книжкового магазину
-- СУБД: SQLite
-- Кодування: UTF-8
-- Автор: [Твоє ім'я]
-- =============================================================================
-- Порядок створення таблиць (від батьківських до дочірніх):
--   1. categories      — статичний довідник
--   2. authors         — master data
--   3. suppliers       — master data
--   4. users           — master data
--   5. clients         — master data
--   6. books           — master data (залежить від categories)
--   7. book_authors    — junction (залежить від books, authors)
--   8. sales           — транзакційна (залежить від users, clients)
--   9. sale_items      — транзакційна (залежить від sales, books)
--  10. supplies        — транзакційна (залежить від suppliers)
--  11. supply_items    — транзакційна (залежить від supplies, books)
-- =============================================================================

-- Увімкнення підтримки зовнішніх ключів у SQLite
PRAGMA foreign_keys = ON;

-- =============================================================================
-- ТАБЛИЦЯ: categories
-- Тип: Статичний довідник (Static Reference Table)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: немає транзитивних залежностей між неключовими атрибутами
-- Опис: Зберігає фіксований перелік категорій/жанрів книг.
--       Змінюється рідко, лише адміністратором.
-- =============================================================================
CREATE TABLE categories (
    PRIMARY KEY (category_id),
    category_id   INTEGER      NOT NULL,
    name          VARCHAR(100) NOT NULL,
                  CONSTRAINT categories_name_key
                      UNIQUE (name),
    description   TEXT
);

-- =============================================================================
-- ТАБЛИЦЯ: authors
-- Тип: Master Data (Core Entity)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: first_name та last_name залежать лише від author_id,
--           немає транзитивних залежностей
-- Опис: Основна сутність авторів книг.
--       Може мати багато книг (зв'язок через book_authors).
-- =============================================================================
CREATE TABLE authors (
    PRIMARY KEY (author_id),
    author_id     INTEGER      NOT NULL,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL
);

-- =============================================================================
-- ТАБЛИЦЯ: suppliers
-- Тип: Master Data (Core Entity)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: всі атрибути залежать лише від supplier_id
-- Опис: Постачальники книг для магазину.
-- =============================================================================
CREATE TABLE suppliers (
    PRIMARY KEY (supplier_id),
    supplier_id   INTEGER      NOT NULL,
    company_name  VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    email         VARCHAR(100),
    address       TEXT
);

-- =============================================================================
-- ТАБЛИЦЯ: users
-- Тип: Master Data (Core Entity)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: немає транзитивних залежностей; role не визначає інших атрибутів
-- Опис: Користувачі системи (адміністратор, касир, менеджер).
--       Використовується для аутентифікації та авторизації.
-- =============================================================================
CREATE TABLE users (
    PRIMARY KEY (user_id),
    user_id       INTEGER      NOT NULL,
    username      VARCHAR(50)  NOT NULL,
                  CONSTRAINT users_username_key
                      UNIQUE (username),
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(100) NOT NULL,
                  CONSTRAINT users_email_key
                      UNIQUE (email),
    role          VARCHAR(20)  NOT NULL,
                  CONSTRAINT users_role_check
                      CHECK (role IN ('admin', 'cashier', 'manager'))
);

CREATE INDEX users_email_idx ON users (email);

-- =============================================================================
-- ТАБЛИЦЯ: clients
-- Тип: Master Data (Core Entity)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: phone та email залежать лише від client_id
-- Опис: Покупці магазину. Продаж може бути анонімним (client_id = NULL).
-- =============================================================================
CREATE TABLE clients (
    PRIMARY KEY (client_id),
    client_id     INTEGER      NOT NULL,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    phone         VARCHAR(20),
                  CONSTRAINT clients_phone_key
                      UNIQUE (phone),
    email         VARCHAR(100),
                  CONSTRAINT clients_email_key
                      UNIQUE (email)
);

-- =============================================================================
-- ТАБЛИЦЯ: books
-- Тип: Master Data (Core Entity)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: category_id — лише FK (не зберігаємо назву категорії),
--           немає транзитивних залежностей
-- Опис: Основна сутність — книги магазину.
--       Зв'язок з авторами — через book_authors (N:M).
--       Зв'язок з категорією — через category_id (N:1).
-- =============================================================================
CREATE TABLE books (
    PRIMARY KEY (book_id),
    book_id       INTEGER        NOT NULL,
    category_id   INTEGER        NOT NULL,
                  CONSTRAINT books_category_id_categories_fkey
                      FOREIGN KEY (category_id)
                      REFERENCES categories (category_id)
                          ON DELETE RESTRICT,
    title         VARCHAR(255)   NOT NULL,
    isbn          VARCHAR(20),
                  CONSTRAINT books_isbn_key
                      UNIQUE (isbn),
    price         DECIMAL(10, 2) NOT NULL,
                  CONSTRAINT books_price_positive_check
                      CHECK (price > 0),
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
                   CONSTRAINT books_stock_quantity_check
                       CHECK (stock_quantity >= 0),
    description   TEXT
);

CREATE INDEX books_category_id_idx ON books (category_id);

-- =============================================================================
-- ТАБЛИЦЯ: book_authors
-- Тип: Junction Table (Таблиця-зв'язка)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є складений первинний ключ
--   - 2НФ: немає неключових атрибутів — порушення неможливе
--   - 3НФ: немає неключових атрибутів — порушення неможливе
-- Опис: Реалізує зв'язок N:M між books та authors.
--       Одна книга може мати кількох авторів,
--       один автор може написати багато книг.
-- =============================================================================
CREATE TABLE book_authors (
    PRIMARY KEY (book_id, author_id),
    book_id       INTEGER NOT NULL,
                  CONSTRAINT book_authors_book_id_books_fkey
                      FOREIGN KEY (book_id)
                      REFERENCES books (book_id)
                          ON DELETE CASCADE,
    author_id     INTEGER NOT NULL,
                  CONSTRAINT book_authors_author_id_authors_fkey
                      FOREIGN KEY (author_id)
                      REFERENCES authors (author_id)
                          ON DELETE CASCADE
);

CREATE INDEX book_authors_book_id_idx   ON book_authors (book_id);
CREATE INDEX book_authors_author_id_idx ON book_authors (author_id);

-- =============================================================================
-- ТАБЛИЦЯ: sales
-- Тип: Транзакційна таблиця (Transaction Table)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: user_id та client_id — лише FK (не зберігаємо дані касира/клієнта),
--           total_amount залежить від sale_id, не від user_id чи client_id
-- Опис: Фіксує факт покупки (чек). Один продаж = кілька позицій (sale_items).
--       client_id може бути NULL (анонімна покупка).
-- =============================================================================
CREATE TABLE sales (
    PRIMARY KEY (sale_id),
    sale_id        INTEGER        NOT NULL,
    user_id        INTEGER        NOT NULL,
                   CONSTRAINT sales_user_id_users_fkey
                       FOREIGN KEY (user_id)
                       REFERENCES users (user_id)
                           ON DELETE RESTRICT,
    client_id      INTEGER,
                   CONSTRAINT sales_client_id_clients_fkey
                       FOREIGN KEY (client_id)
                       REFERENCES clients (client_id)
                           ON DELETE SET NULL,
    sale_date      DATETIME       NOT NULL,
    total_amount   DECIMAL(10, 2) NOT NULL,
                   CONSTRAINT sales_total_amount_positive_check
                       CHECK (total_amount >= 0),
    payment_method VARCHAR(20)    NOT NULL,
                   CONSTRAINT sales_payment_method_check
                       CHECK (payment_method IN ('cash', 'card', 'online'))
);

CREATE INDEX sales_user_id_idx   ON sales (user_id);
CREATE INDEX sales_client_id_idx ON sales (client_id);
CREATE INDEX sales_sale_date_idx ON sales (sale_date);

-- =============================================================================
-- ТАБЛИЦЯ: sale_items
-- Тип: Транзакційна таблиця (Transaction Table)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: quantity та unit_price залежать від sale_item_id;
--           unit_price — знімок ціни на момент продажу (свідома денормалізація
--           для аудиту: ціна книги може змінитись, але факт продажу — ні)
-- Опис: Позиція покупки. Один чек містить багато позицій.
-- =============================================================================
CREATE TABLE sale_items (
    PRIMARY KEY (sale_item_id),
    sale_item_id  INTEGER        NOT NULL,
    sale_id       INTEGER        NOT NULL,
                  CONSTRAINT sale_items_sale_id_sales_fkey
                      FOREIGN KEY (sale_id)
                      REFERENCES sales (sale_id)
                          ON DELETE CASCADE,
    book_id       INTEGER        NOT NULL,
                  CONSTRAINT sale_items_book_id_books_fkey
                      FOREIGN KEY (book_id)
                      REFERENCES books (book_id)
                          ON DELETE RESTRICT,
    quantity      INTEGER        NOT NULL,
                  CONSTRAINT sale_items_quantity_positive_check
                      CHECK (quantity > 0),
    unit_price    DECIMAL(10, 2) NOT NULL,
                  CONSTRAINT sale_items_unit_price_positive_check
                      CHECK (unit_price > 0)
);

CREATE INDEX sale_items_sale_id_idx ON sale_items (sale_id);
CREATE INDEX sale_items_book_id_idx ON sale_items (book_id);

-- =============================================================================
-- ТАБЛИЦЯ: supplies
-- Тип: Транзакційна таблиця (Transaction Table)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: supplier_id — лише FK, total_cost залежить лише від supply_id
-- Опис: Фіксує факт постачання товару від постачальника.
-- =============================================================================
CREATE TABLE supplies (
    PRIMARY KEY (supply_id),
    supply_id     INTEGER        NOT NULL,
    supplier_id   INTEGER        NOT NULL,
                  CONSTRAINT supplies_supplier_id_suppliers_fkey
                      FOREIGN KEY (supplier_id)
                      REFERENCES suppliers (supplier_id)
                          ON DELETE RESTRICT,
    supply_date   DATETIME       NOT NULL,
    total_cost    DECIMAL(10, 2) NOT NULL,
                  CONSTRAINT supplies_total_cost_positive_check
                      CHECK (total_cost > 0)
);

CREATE INDEX supplies_supplier_id_idx ON supplies (supplier_id);
CREATE INDEX supplies_supply_date_idx ON supplies (supply_date);

-- =============================================================================
-- ТАБЛИЦЯ: supply_items
-- Тип: Транзакційна таблиця (Transaction Table)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: quantity та unit_cost залежать від supply_item_id;
--           unit_cost — знімок закупівельної ціни на момент постачання
-- Опис: Позиція постачання. Одне постачання містить багато позицій.
-- =============================================================================
CREATE TABLE supply_items (
    PRIMARY KEY (supply_item_id),
    supply_item_id INTEGER        NOT NULL,
    supply_id      INTEGER        NOT NULL,
                   CONSTRAINT supply_items_supply_id_supplies_fkey
                       FOREIGN KEY (supply_id)
                       REFERENCES supplies (supply_id)
                           ON DELETE CASCADE,
    book_id        INTEGER        NOT NULL,
                   CONSTRAINT supply_items_book_id_books_fkey
                       FOREIGN KEY (book_id)
                       REFERENCES books (book_id)
                           ON DELETE RESTRICT,
    quantity       INTEGER        NOT NULL,
                   CONSTRAINT supply_items_quantity_positive_check
                       CHECK (quantity > 0),
    unit_cost      DECIMAL(10, 2) NOT NULL,
                   CONSTRAINT supply_items_unit_cost_positive_check
                       CHECK (unit_cost > 0)
);

CREATE INDEX supply_items_supply_id_idx ON supply_items (supply_id);
CREATE INDEX supply_items_book_id_idx   ON supply_items (book_id);
