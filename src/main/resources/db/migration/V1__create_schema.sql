-- =============================================================================
-- V1__create_schema.sql — Схема бази даних книжкового магазину
-- СУБД: SQLite
-- Кодування: UTF-8
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

-- =============================================================================
-- ТАБЛИЦЯ: categories
-- Тип: Статичний довідник (Static Reference Table)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: немає транзитивних залежностей між неключовими атрибутами
-- =============================================================================
CREATE TABLE categories (
    category_id TEXT        NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    CONSTRAINT categories_pk PRIMARY KEY (category_id),
    CONSTRAINT categories_name_key UNIQUE (name)
);

-- =============================================================================
-- ТАБЛИЦЯ: authors
-- Тип: Master Data (Core Entity)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: first_name та last_name залежать лише від author_id
-- =============================================================================
CREATE TABLE authors (
    author_id  TEXT        NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name  VARCHAR(50) NOT NULL,
    CONSTRAINT authors_pk PRIMARY KEY (author_id)
);

-- =============================================================================
-- ТАБЛИЦЯ: suppliers
-- Тип: Master Data (Core Entity)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: всі атрибути залежать лише від supplier_id
-- =============================================================================
CREATE TABLE suppliers (
    supplier_id  TEXT         NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    phone        VARCHAR(20),
    email        VARCHAR(100),
    address      TEXT,
    CONSTRAINT suppliers_pk PRIMARY KEY (supplier_id)
);

-- =============================================================================
-- ТАБЛИЦЯ: users
-- Тип: Master Data (Core Entity)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: немає транзитивних залежностей; role не визначає інших атрибутів
-- =============================================================================
CREATE TABLE users (
    user_id       TEXT         NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    CONSTRAINT users_pk PRIMARY KEY (user_id),
    CONSTRAINT users_username_key UNIQUE (username),
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('admin', 'cashier', 'manager'))
);

CREATE INDEX users_email_idx ON users (email);

-- =============================================================================
-- ТАБЛИЦЯ: clients
-- Тип: Master Data (Core Entity)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: phone та email залежать лише від client_id
-- =============================================================================
CREATE TABLE clients (
    client_id  TEXT        NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name  VARCHAR(50) NOT NULL,
    phone      VARCHAR(20),
    email      VARCHAR(100),
    CONSTRAINT clients_pk PRIMARY KEY (client_id),
    CONSTRAINT clients_phone_key UNIQUE (phone),
    CONSTRAINT clients_email_key UNIQUE (email)
);

-- =============================================================================
-- ТАБЛИЦЯ: books
-- Тип: Master Data (Core Entity)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: category_id — лише FK, немає транзитивних залежностей
-- =============================================================================
CREATE TABLE books (
    book_id        TEXT           NOT NULL,
    category_id    TEXT           NOT NULL,
    title          VARCHAR(255)   NOT NULL,
    isbn           VARCHAR(20),
    price          DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    description    TEXT,
    CONSTRAINT books_pk PRIMARY KEY (book_id),
    CONSTRAINT books_isbn_key UNIQUE (isbn),
    CONSTRAINT books_price_positive_check CHECK (price > 0),
    CONSTRAINT books_stock_quantity_check CHECK (stock_quantity >= 0),
    CONSTRAINT books_category_id_fkey FOREIGN KEY (category_id)
        REFERENCES categories (category_id) ON DELETE RESTRICT
);

CREATE INDEX books_category_id_idx ON books (category_id);

-- =============================================================================
-- ТАБЛИЦЯ: book_authors
-- Тип: Junction Table (Таблиця-зв'язка)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є складений первинний ключ
--   - 2НФ: немає неключових атрибутів — порушення неможливе
--   - 3НФ: немає неключових атрибутів — порушення неможливе
-- =============================================================================
CREATE TABLE book_authors (
    book_id   TEXT NOT NULL,
    author_id TEXT NOT NULL,
    CONSTRAINT book_authors_pk PRIMARY KEY (book_id, author_id),
    CONSTRAINT book_authors_book_fkey FOREIGN KEY (book_id)
        REFERENCES books (book_id) ON DELETE CASCADE,
    CONSTRAINT book_authors_author_fkey FOREIGN KEY (author_id)
        REFERENCES authors (author_id) ON DELETE CASCADE
);

CREATE INDEX book_authors_book_id_idx   ON book_authors (book_id);
CREATE INDEX book_authors_author_id_idx ON book_authors (author_id);

-- =============================================================================
-- ТАБЛИЦЯ: sales
-- Тип: Транзакційна таблиця (Transaction Table)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: total_amount залежить від sale_id, не від user_id чи client_id
-- =============================================================================
CREATE TABLE sales (
    sale_id        TEXT           NOT NULL,
    user_id        TEXT           NOT NULL,
    client_id      TEXT,
    sale_date      DATETIME       NOT NULL,
    total_amount   DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20)    NOT NULL,
    CONSTRAINT sales_pk PRIMARY KEY (sale_id),
    CONSTRAINT sales_total_amount_check CHECK (total_amount >= 0),
    CONSTRAINT sales_payment_method_check CHECK (payment_method IN ('cash', 'card', 'online')),
    CONSTRAINT sales_user_fkey FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT sales_client_fkey FOREIGN KEY (client_id)
        REFERENCES clients (client_id) ON DELETE SET NULL
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
--   - 3НФ: unit_price — знімок ціни на момент продажу (свідома денормалізація)
-- =============================================================================
CREATE TABLE sale_items (
    sale_item_id TEXT           NOT NULL,
    sale_id      TEXT           NOT NULL,
    book_id      TEXT           NOT NULL,
    quantity     INTEGER        NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    CONSTRAINT sale_items_pk PRIMARY KEY (sale_item_id),
    CONSTRAINT sale_items_quantity_check CHECK (quantity > 0),
    CONSTRAINT sale_items_unit_price_check CHECK (unit_price > 0),
    CONSTRAINT sale_items_sale_fkey FOREIGN KEY (sale_id)
        REFERENCES sales (sale_id) ON DELETE CASCADE,
    CONSTRAINT sale_items_book_fkey FOREIGN KEY (book_id)
        REFERENCES books (book_id) ON DELETE RESTRICT
);

CREATE INDEX sale_items_sale_id_idx ON sale_items (sale_id);
CREATE INDEX sale_items_book_id_idx ON sale_items (book_id);

-- =============================================================================
-- ТАБЛИЦЯ: supplies
-- Тип: Транзакційна таблиця (Transaction Table)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: total_cost залежить лише від supply_id
-- =============================================================================
CREATE TABLE supplies (
    supply_id   TEXT           NOT NULL,
    supplier_id TEXT           NOT NULL,
    supply_date DATETIME       NOT NULL,
    total_cost  DECIMAL(10, 2) NOT NULL,
    CONSTRAINT supplies_pk PRIMARY KEY (supply_id),
    CONSTRAINT supplies_total_cost_check CHECK (total_cost > 0),
    CONSTRAINT supplies_supplier_fkey FOREIGN KEY (supplier_id)
        REFERENCES suppliers (supplier_id) ON DELETE RESTRICT
);

CREATE INDEX supplies_supplier_id_idx ON supplies (supplier_id);
CREATE INDEX supplies_supply_date_idx ON supplies (supply_date);

-- =============================================================================
-- ТАБЛИЦЯ: supply_items
-- Тип: Транзакційна таблиця (Transaction Table)
-- Нормальна форма: 3НФ
--   - 1НФ: всі атрибути атомарні, є первинний ключ
--   - 2НФ: простий PK — часткові залежності неможливі
--   - 3НФ: unit_cost — знімок закупівельної ціни на момент постачання
-- =============================================================================
CREATE TABLE supply_items (
    supply_item_id TEXT           NOT NULL,
    supply_id      TEXT           NOT NULL,
    book_id        TEXT           NOT NULL,
    quantity       INTEGER        NOT NULL,
    unit_cost      DECIMAL(10, 2) NOT NULL,
    CONSTRAINT supply_items_pk PRIMARY KEY (supply_item_id),
    CONSTRAINT supply_items_quantity_check CHECK (quantity > 0),
    CONSTRAINT supply_items_unit_cost_check CHECK (unit_cost > 0),
    CONSTRAINT supply_items_supply_fkey FOREIGN KEY (supply_id)
        REFERENCES supplies (supply_id) ON DELETE CASCADE,
    CONSTRAINT supply_items_book_fkey FOREIGN KEY (book_id)
        REFERENCES books (book_id) ON DELETE RESTRICT
);

CREATE INDEX supply_items_supply_id_idx ON supply_items (supply_id);
CREATE INDEX supply_items_book_id_idx   ON supply_items (book_id);