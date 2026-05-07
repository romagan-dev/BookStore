-- =============================================================================
-- ddl_h2.sql — Схема БД для інтеграційних тестів (H2 in-memory)
-- =============================================================================
-- H2 відмінності від SQLite:
--   - UUID тип підтримується нативно
--   - DATETIME → TIMESTAMP
--   - TEXT → VARCHAR або CLOB
-- =============================================================================

CREATE TABLE IF NOT EXISTS categories (
    category_id  VARCHAR(36)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  CLOB,
    CONSTRAINT categories_pk PRIMARY KEY (category_id),
    CONSTRAINT categories_name_key UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS authors (
    author_id  VARCHAR(36) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name  VARCHAR(50) NOT NULL,
    CONSTRAINT authors_pk PRIMARY KEY (author_id)
);

CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id  VARCHAR(36)  NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    phone        VARCHAR(20),
    email        VARCHAR(100),
    address      CLOB,
    CONSTRAINT suppliers_pk PRIMARY KEY (supplier_id)
);

CREATE TABLE IF NOT EXISTS users (
    user_id       VARCHAR(36)  NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    CONSTRAINT users_pk PRIMARY KEY (user_id),
    CONSTRAINT users_username_key UNIQUE (username),
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('admin', 'cashier', 'manager'))
);

CREATE TABLE IF NOT EXISTS clients (
    client_id  VARCHAR(36) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name  VARCHAR(50) NOT NULL,
    phone      VARCHAR(20),
    email      VARCHAR(100),
    CONSTRAINT clients_pk PRIMARY KEY (client_id),
    CONSTRAINT clients_phone_key UNIQUE (phone),
    CONSTRAINT clients_email_key UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS books (
    book_id        VARCHAR(36)    NOT NULL,
    category_id    VARCHAR(36)    NOT NULL,
    title          VARCHAR(255)   NOT NULL,
    isbn           VARCHAR(20),
    price          DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    description    CLOB,
    CONSTRAINT books_pk PRIMARY KEY (book_id),
    CONSTRAINT books_isbn_key UNIQUE (isbn),
    CONSTRAINT books_price_check CHECK (price > 0),
    CONSTRAINT books_stock_check CHECK (stock_quantity >= 0),
    CONSTRAINT books_category_fkey FOREIGN KEY (category_id)
        REFERENCES categories (category_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS book_authors (
    book_id   VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    CONSTRAINT book_authors_pk PRIMARY KEY (book_id, author_id),
    CONSTRAINT book_authors_book_fkey FOREIGN KEY (book_id)
        REFERENCES books (book_id) ON DELETE CASCADE,
    CONSTRAINT book_authors_author_fkey FOREIGN KEY (author_id)
        REFERENCES authors (author_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sales (
    sale_id        VARCHAR(36)    NOT NULL,
    user_id        VARCHAR(36)    NOT NULL,
    client_id      VARCHAR(36),
    sale_date      TIMESTAMP      NOT NULL,
    total_amount   DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20)    NOT NULL,
    CONSTRAINT sales_pk PRIMARY KEY (sale_id),
    CONSTRAINT sales_amount_check CHECK (total_amount >= 0),
    CONSTRAINT sales_payment_check CHECK (payment_method IN ('cash', 'card', 'online')),
    CONSTRAINT sales_user_fkey FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT sales_client_fkey FOREIGN KEY (client_id)
        REFERENCES clients (client_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS sale_items (
    sale_item_id VARCHAR(36)    NOT NULL,
    sale_id      VARCHAR(36)    NOT NULL,
    book_id      VARCHAR(36)    NOT NULL,
    quantity     INTEGER        NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    CONSTRAINT sale_items_pk PRIMARY KEY (sale_item_id),
    CONSTRAINT sale_items_qty_check CHECK (quantity > 0),
    CONSTRAINT sale_items_price_check CHECK (unit_price > 0),
    CONSTRAINT sale_items_sale_fkey FOREIGN KEY (sale_id)
        REFERENCES sales (sale_id) ON DELETE CASCADE,
    CONSTRAINT sale_items_book_fkey FOREIGN KEY (book_id)
        REFERENCES books (book_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS supplies (
    supply_id   VARCHAR(36)    NOT NULL,
    supplier_id VARCHAR(36)    NOT NULL,
    supply_date TIMESTAMP      NOT NULL,
    total_cost  DECIMAL(10, 2) NOT NULL,
    CONSTRAINT supplies_pk PRIMARY KEY (supply_id),
    CONSTRAINT supplies_cost_check CHECK (total_cost > 0),
    CONSTRAINT supplies_supplier_fkey FOREIGN KEY (supplier_id)
        REFERENCES suppliers (supplier_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS supply_items (
    supply_item_id VARCHAR(36)    NOT NULL,
    supply_id      VARCHAR(36)    NOT NULL,
    book_id        VARCHAR(36)    NOT NULL,
    quantity       INTEGER        NOT NULL,
    unit_cost      DECIMAL(10, 2) NOT NULL,
    CONSTRAINT supply_items_pk PRIMARY KEY (supply_item_id),
    CONSTRAINT supply_items_qty_check CHECK (quantity > 0),
    CONSTRAINT supply_items_cost_check CHECK (unit_cost > 0),
    CONSTRAINT supply_items_supply_fkey FOREIGN KEY (supply_id)
        REFERENCES supplies (supply_id) ON DELETE CASCADE,
    CONSTRAINT supply_items_book_fkey FOREIGN KEY (book_id)
        REFERENCES books (book_id) ON DELETE RESTRICT
);