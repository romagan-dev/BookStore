package com.bookstore.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.bookstore.domain.entity.Author;
import com.bookstore.domain.entity.Book;
import com.bookstore.domain.entity.Category;
import com.bookstore.infrastructure.persistence.impl.JdbcAuthorRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcBookRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcCategoryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Інтеграційні тести для {@link JdbcBookRepository}.
 *
 * <p>Тестує CRUD, FK-обмеження, Lazy Loading авторів та специфічні методи пошуку.
 */
@DisplayName("JdbcBookRepository — інтеграційні тести")
class JdbcBookRepositoryTest extends BaseRepositoryTest {

    private JdbcBookRepository bookRepository;
    private JdbcCategoryRepository categoryRepository;
    private JdbcAuthorRepository authorRepository;

    // Спільні тестові дані
    private Category testCategory;

    @Override
    protected void setUp() {
        authorRepository = new JdbcAuthorRepository(connectionManager);
        categoryRepository = new JdbcCategoryRepository(connectionManager);
        bookRepository = new JdbcBookRepository(connectionManager, authorRepository);

        // Спільна категорія для всіх тестів
        testCategory = Category.builder()
              .id(UUID.randomUUID())
              .name("Тестова категорія")
              .build();
        categoryRepository.save(testCategory);
    }

    // =========================================================
    // Хелпер — швидке створення книги
    // =========================================================
    private Book buildBook(String title, BigDecimal price) {
        return Book.builder()
              .id(UUID.randomUUID())
              .category(testCategory)
              .title(title)
              .isbn(UUID.randomUUID().toString().substring(0, 13))
              .price(price)
              .stockQuantity(10)
              .build();
    }

    // =========================================================
    // save()
    // =========================================================

    @Test
    @DisplayName("save: зберігає книгу і можна знайти через findById")
    void givenValidBook_whenSave_thenCanBeFoundById() {
        // Arrange
        Book book = buildBook("Кобзар", new BigDecimal("150.00"));

        // Act
        bookRepository.save(book);

        // Assert
        Optional<Book> found = bookRepository.findById(book.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Кобзар");
        assertThat(found.get().getPrice()).isEqualByComparingTo("150.00");
        assertThat(found.get().getCategory().getName()).isEqualTo("Тестова категорія");
    }

    @Test
    @DisplayName("save: кидає виняток при неіснуючому category_id (FK constraint)")
    void givenBookWithNonExistentCategory_whenSave_thenThrowsException() {
        // Arrange
        Category fakeCategory = Category.builder()
              .id(UUID.randomUUID())
              .name("Неіснуюча")
              .build();
        Book book = Book.builder()
              .id(UUID.randomUUID())
              .category(fakeCategory)
              .title("Книга без категорії")
              .price(new BigDecimal("100.00"))
              .stockQuantity(5)
              .build();

        // Act & Assert
        assertThatThrownBy(() -> bookRepository.save(book))
              .isInstanceOf(RuntimeException.class);
    }

    // =========================================================
    // findAll()
    // =========================================================

    @Test
    @DisplayName("findAll: повертає всі збережені книги")
    void givenThreeBooks_whenFindAll_thenReturnsAll() {
        // Arrange
        bookRepository.save(buildBook("Книга А", new BigDecimal("100.00")));
        bookRepository.save(buildBook("Книга Б", new BigDecimal("200.00")));
        bookRepository.save(buildBook("Книга В", new BigDecimal("300.00")));

        // Act
        List<Book> all = bookRepository.findAll();

        // Assert
        assertThat(all).hasSize(3);
    }

    // =========================================================
    // update()
    // =========================================================

    @Test
    @DisplayName("update: оновлює ціну та кількість книги")
    void givenExistingBook_whenUpdate_thenDataIsChanged() {
        // Arrange
        Book book = buildBook("Оновлювана книга", new BigDecimal("100.00"));
        bookRepository.save(book);

        // Act
        book.setPrice(new BigDecimal("199.99"));
        book.setStockQuantity(25);
        bookRepository.update(book);

        // Assert
        bookRepository.clearCache();
        Book updated = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(updated.getPrice()).isEqualByComparingTo("199.99");
        assertThat(updated.getStockQuantity()).isEqualTo(25);
    }

    // =========================================================
    // deleteById()
    // =========================================================

    @Test
    @DisplayName("deleteById: видаляє книгу")
    void givenExistingBook_whenDeleteById_thenNotFound() {
        // Arrange
        Book book = buildBook("Видалювана", new BigDecimal("50.00"));
        bookRepository.save(book);

        // Act
        boolean deleted = bookRepository.deleteById(book.getId());

        // Assert
        assertThat(deleted).isTrue();
        bookRepository.clearCache();
        assertThat(bookRepository.findById(book.getId())).isEmpty();
    }

    // =========================================================
    // findByTitle()
    // =========================================================

    @Test
    @DisplayName("findByTitle: знаходить книги за частиною назви")
    void givenBooks_whenFindByTitle_thenReturnsMatching() {
        // Arrange
        bookRepository.save(buildBook("Гаррі Поттер і філософський камінь",
              new BigDecimal("285.00")));
        bookRepository.save(buildBook("Гаррі Поттер і таємна кімната",
              new BigDecimal("285.00")));
        bookRepository.save(buildBook("Володар Перснів", new BigDecimal("320.00")));

        // Act
        List<Book> found = bookRepository.findByTitle("Гаррі");

        // Assert
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Book::getTitle)
              .allMatch(t -> t.contains("Гаррі"));
    }

    // =========================================================
    // findInStock()
    // =========================================================

    @Test
    @DisplayName("findInStock: повертає лише книги з наявністю > 0")
    void givenMixedStock_whenFindInStock_thenReturnsOnlyAvailable() {
        // Arrange
        Book inStock = buildBook("В наявності", new BigDecimal("100.00"));
        inStock.setStockQuantity(5);

        Book outOfStock = buildBook("Немає в наявності", new BigDecimal("100.00"));
        outOfStock.setStockQuantity(0);

        bookRepository.save(inStock);
        bookRepository.save(outOfStock);

        // Act
        List<Book> available = bookRepository.findInStock();

        // Assert
        assertThat(available).hasSize(1);
        assertThat(available.get(0).getTitle()).isEqualTo("В наявності");
    }

    // =========================================================
    // findByMaxPrice()
    // =========================================================

    @Test
    @DisplayName("findByMaxPrice: повертає книги дешевші або рівні заданій ціні")
    void givenBooks_whenFindByMaxPrice_thenReturnsAffordable() {
        // Arrange
        bookRepository.save(buildBook("Дешева", new BigDecimal("50.00")));
        bookRepository.save(buildBook("Середня", new BigDecimal("150.00")));
        bookRepository.save(buildBook("Дорога", new BigDecimal("500.00")));

        // Act
        List<Book> affordable = bookRepository.findByMaxPrice(new BigDecimal("150.00"));

        // Assert
        assertThat(affordable).hasSize(2);
        assertThat(affordable).extracting(Book::getTitle)
              .containsExactlyInAnyOrder("Дешева", "Середня");
    }

    // =========================================================
    // Lazy Loading авторів
    // =========================================================

    @Test
    @DisplayName("lazyLoading: автори завантажуються при першому зверненні")
    void givenBookWithAuthors_whenGetAuthors_thenAuthorsAreLoaded() throws Exception {
        // Arrange
        Author author = Author.builder()
              .id(UUID.randomUUID())
              .firstName("Тарас")
              .lastName("Шевченко")
              .build();
        authorRepository.save(author);

        Book book = buildBook("Кобзар", new BigDecimal("150.00"));
        bookRepository.save(book);

        // Зв'язуємо книгу з автором через book_authors
        executeSql(String.format(
              "INSERT INTO book_authors (book_id, author_id) VALUES ('%s', '%s')",
              book.getId(), author.getId()));

        // Act — очищаємо кеш і завантажуємо книгу заново
        bookRepository.clearCache();
        Book loaded = bookRepository.findById(book.getId()).orElseThrow();

        // Assert — автори НЕ завантажені до getAuthors()
        assertThat(loaded.getAuthors()).hasSize(1);
        assertThat(loaded.getAuthors().get(0).getLastName()).isEqualTo("Шевченко");
    }

    // =========================================================
    // Бізнес-методи Book
    // =========================================================

    @Test
    @DisplayName("decreaseStock: зменшує кількість книг на складі")
    void givenBookInStock_whenDecreaseStock_thenQuantityDecreases() {
        // Arrange
        Book book = buildBook("Книга на складі", new BigDecimal("100.00"));
        book.setStockQuantity(10);

        // Act
        book.decreaseStock(3);

        // Assert
        assertThat(book.getStockQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("decreaseStock: кидає виняток якщо недостатньо на складі")
    void givenInsufficientStock_whenDecreaseStock_thenThrowsException() {
        // Arrange
        Book book = buildBook("Мало на складі", new BigDecimal("100.00"));
        book.setStockQuantity(2);

        // Act & Assert
        assertThatThrownBy(() -> book.decreaseStock(5))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Недостатньо");
    }
}