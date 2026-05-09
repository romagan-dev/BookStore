package com.bookstore.application;

import com.bookstore.application.contract.AuthService;
import com.bookstore.application.contract.AuthorService;
import com.bookstore.application.contract.BookService;
import com.bookstore.application.contract.CategoryService;
import com.bookstore.application.contract.ClientService;
import com.bookstore.application.contract.SaleService;
import com.bookstore.application.contract.SupplierService;
import com.bookstore.application.contract.SupplyService;
import com.bookstore.application.impl.AuthServiceImpl;
import com.bookstore.application.impl.AuthorServiceImpl;
import com.bookstore.application.impl.BookServiceImpl;
import com.bookstore.application.impl.CategoryServiceImpl;
import com.bookstore.application.impl.ClientServiceImpl;
import com.bookstore.application.impl.SaleServiceImpl;
import com.bookstore.application.impl.SupplierServiceImpl;
import com.bookstore.application.impl.SupplyServiceImpl;
import com.bookstore.infrastructure.mail.EmailService;
import com.bookstore.infrastructure.persistence.impl.JdbcAuthorRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcBookRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcCategoryRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcClientRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSaleItemRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSaleRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSupplierRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSupplyItemRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSupplyRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcUserRepository;
import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.security.PasswordEncoder;

/**
 * Фабрика сервісів - централізований контейнер залежностей.
 *
 * <p>Реалізує патерни Singleton та Facade: один екземпляр на весь додаток, приховує складність
 * створення та ін'єкції залежностей. Змінює IoC-контейнер (Spring) для навчального проєкту.
 *
 * <p>Використання
 *
 * <pre> {@code ServiceFactory factory = ServiceFactory.getInstance("bookstore.db");
 * ClientService clientService = factory.clientService();}</pre>
 */
public class ServiceFactory {

    /** Єдиний екземпляр (Siongleton). */
    private static volatile ServiceFactory instance;

    // ====== Інфраструктура =====
    private final ConnectionManager connectionManager;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ===== Репозиторії =====
    private final JdbcUserRepository userRepository;
    private final JdbcClientRepository clientRepository;
    private final JdbcCategoryRepository categoryRepository;
    private final JdbcAuthorRepository authorRepository;
    private final JdbcSupplierRepository supplierRepository;
    private final JdbcBookRepository bookRepository;
    private final JdbcSaleItemRepository saleItemRepository;
    private final JdbcSaleRepository saleRepository;
    private final JdbcSupplyItemRepository supplyItemRepository;
    private final JdbcSupplyRepository supplyRepository;

    // ===== Сервіси =====
    private final AuthService authService;
    private final ClientService clientService;
    private final CategoryService categoryService;
    private final AuthorService authorService;
    private final SupplierService supplierService;
    private final BookService bookService;
    private final SaleService saleService;
    private final SupplyService supplyService;

    /**
     * Приватний конструктор - інціалізує всі залежності у правильному порядку.
     *
     * @param dbPath шлях до SQLite файлу
     */
    private ServiceFactory(String dbPath) {
        // 1. Інфраструктура
        this.connectionManager = ConnectionManager.forSQLite(dbPath);
        this.passwordEncoder = new PasswordEncoder();
        this.emailService = new EmailService();

        // 2. Репозиторії (Dependency Injection знизу вгору)
        this.userRepository = new JdbcUserRepository(connectionManager);
        this.clientRepository = new JdbcClientRepository(connectionManager);
        this.categoryRepository = new JdbcCategoryRepository(connectionManager);
        this.authorRepository = new JdbcAuthorRepository(connectionManager);
        this.supplierRepository = new JdbcSupplierRepository(connectionManager);
        this.bookRepository = new JdbcBookRepository(connectionManager, authorRepository);
        this.saleItemRepository = new JdbcSaleItemRepository(connectionManager);
        this.saleRepository = new JdbcSaleRepository(connectionManager, saleItemRepository);
        this.supplyItemRepository = new JdbcSupplyItemRepository(connectionManager);
        this.supplyRepository = new JdbcSupplyRepository(connectionManager, supplyItemRepository);


        // 3. Сервіси (Dependency Injection)
        this.authService = new AuthServiceImpl(userRepository, passwordEncoder, emailService);
        this.clientService = new ClientServiceImpl(clientRepository);
        this.categoryService = new CategoryServiceImpl(categoryRepository);
        this.authorService = new AuthorServiceImpl(authorRepository);
        this.supplierService = new SupplierServiceImpl(supplierRepository);
        this.bookService = new BookServiceImpl(bookRepository, categoryRepository, authorRepository);
        this.saleService = new SaleServiceImpl(saleRepository, userRepository, clientRepository);
        this.supplyService = new SupplyServiceImpl(supplyRepository, supplierRepository);

    }

    /**
     * Повертає єдиний екземпляр (Double-checked locking Singleton).
     *
     * @param dbPath шлях до SQLite файлу (використовується лише при першому виклику)
     * @return єдиний екземпляр фабрики
     */
    public static ServiceFactory getInstance(String dbPath) {
        if (instance == null) {
            synchronized (ServiceFactory.class) {
                if (instance == null) {
                    instance = new ServiceFactory(dbPath);
                }
            }
        }
        return instance;
    }

    // ===== Геттери сервісів =====

    public AuthService authService() {
        return authService;
    }

    public ClientService clientService() {
        return clientService;
    }

    public CategoryService categoryService() {
        return categoryService;
    }

    public AuthorService authorService() {
        return authorService;
    }

    public SupplierService supplierService() {
        return supplierService;
    }

    public BookService bookService() {
        return bookService;
    }

    public ConnectionManager connectionManager() {
        return connectionManager;
    }

}
