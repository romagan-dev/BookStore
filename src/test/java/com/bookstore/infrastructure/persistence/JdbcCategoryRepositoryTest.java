package com.bookstore.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.bookstore.domain.entity.Category;
import com.bookstore.infrastructure.persistence.impl.JdbcCategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Інтеграційні тести для {@link JdbcCategoryRepository}.
 *
 * <p>Тестує CRUD операції та специфічні методи на реальній H2 in-memory БД.
 * Патерн: ААА (Arrange-Act-Assert). Іменування: given_коли_тоді.
 */
@DisplayName("JdbcCategoryRepository - інтеграційні тести")
class JdbcCategoryRepositoryTest  extends BaseRepositoryTest {

    private JdbcCategoryRepository repository;

    @Override
    protected void setUp() {
        repository = new JdbcCategoryRepository(connectionManager);
    }

    // =========================================================
    // save()
    // =========================================================

    @Test
    @DisplayName("save: зберігає категорію і вона доступна через findById")
    void givenValidCategory_whenSave_thenCanBeFoundById() {
        //Arrange
        Category category =
              Category.builder()
                    .id(UUID.randomUUID())
                    .name("Фантастика")
                    .description("Наукова фантастика та фентезі")
                    .build();

        // Act
        repository.save(category);

        // Assert
        Optional<Category> found = repository.findById(category.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Фантастика");
        assertThat(found.get().getDescription()).isEqualTo("Наукова фантастика та фентезі");
    }

    @Test
    @DisplayName("save: зберігає категорію без опису (description = null)")
    void givenCategoryWithoutDescription_whenSave_thennDescriptionIsNull() {
        // Arrange
        Category category =
              Category.builder()
                    .id(UUID.randomUUID())
                    .name("Поезія")
                    .description(null)
                    .build();

        // Act
        repository.save(category);

        // Assert
        Optional<Category> found = repository.findById(category.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isNull();
    }

    @Test
    @DisplayName("save: кидає виняток при дублюванні name (UNIQUE constraint")
    void givenDuplicateName_whenSave_thenThrowsException() {
        // Arrange
        Category first =
              Category.builder().id(UUID.randomUUID()).name("Детектив").build();
        Category duplicate =
              Category.builder().id(UUID.randomUUID()).name("Детектив").build();

        repository.save(first);

        // Act & Assert
        assertThatThrownBy(() -> repository.save(duplicate))
              .isInstanceOf(RuntimeException.class);

    }
    // =========================================================
    // findById()
    // =========================================================

    @Test
    @DisplayName("findById: повертає empty якщо категорія не існує")
    void givenNonExistentId_whenFindById_thenReturnsEmpty() {
        // Act
        Optional<Category> found = repository.findById(UUID.randomUUID());

        // Assert
        assertThat(found).isEmpty();
    }

    // =========================================================
    // findAll()
    // =========================================================

    @Test
    @DisplayName("findAll: повертає всі збережені категорії")
    void givenThreeCategories_whenFindAll_thenReturnsAll() {
        // Arrange
        repository.save(Category.builder().id(UUID.randomUUID()).name("Роман").build());
        repository.save(Category.builder().id(UUID.randomUUID()).name("Казка").build());
        repository.save(Category.builder().id(UUID.randomUUID()).name("Драма").build());

        // Act
        List<Category> all = repository.findAll();

        // Assert
        assertThat(all).hasSize(3);
        assertThat(all).extracting(Category::getName)
              .containsExactlyInAnyOrder("Роман", "Казка", "Драма");
    }

    @Test
    @DisplayName("findAll: повертає порожній список якщо таблиця порожня")
    void givenEmptyTable_whenFindAll_thenReturnsEmptyList() {
        // Act
        List<Category> all = repository.findAll();

        // Assert
        assertThat(all).isEmpty();
    }

    // =========================================================
    // update()
    // =========================================================

    @Test
    @DisplayName("update: оновлює name та description категорії")
    void givenExistingCategory_whenUpdate_thenDataIsChanged() {
        // Arrange
        Category category =
              Category.builder()
                    .id(UUID.randomUUID())
                    .name("Стара назва")
                    .description("Старий опис")
                    .build();
        repository.save(category);

        // Act
        category.setName("Нова назва");
        category.setDescription("Новий опис");
        repository.update(category);

        // Assert
        Category updated = repository.findById(category.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Нова назва");
        assertThat(updated.getDescription()).isEqualTo("Новий опис");
    }

    // =========================================================
    // deleteById()
    // =========================================================

    @Test
    @DisplayName("deleteById: видаляє категорію і вона більше не доступна")
    void givenExistingCategory_whenDeleteById_thenNotFoundAnymore() {
        // Arrange
        Category category =
              Category.builder().id(UUID.randomUUID()).name("Для видалення").build();
        repository.save(category);

        // Act
        boolean deleted = repository.deleteById(category.getId());

        // Assert
        assertThat(deleted).isTrue();
        assertThat(repository.findById(category.getId())).isEmpty();
    }

    @Test
    @DisplayName("deleteById: повертає false якщо категорія не існує")
    void givenNonExistentId_whenDeleteById_thenReturnsFalse() {
        // Act
        boolean deleted = repository.deleteById(UUID.randomUUID());

        // Assert
        assertThat(deleted).isFalse();
    }

    // =========================================================
    // count() та existsById()
    // =========================================================

    @Test
    @DisplayName("count: повертає правильну кількість записів")
    void givenTwoCategories_whenCount_thenReturnsTwo() {
        // Arrange
        repository.save(Category.builder().id(UUID.randomUUID()).name("Перша").build());
        repository.save(Category.builder().id(UUID.randomUUID()).name("Друга").build());

        // Act & Assert
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("existsById: повертає true для існуючої категорії")
    void givenExistingCategory_whenExistsById_thenReturnsTrue() {
        // Arrange
        Category category =
              Category.builder().id(UUID.randomUUID()).name("Існуюча").build();
        repository.save(category);

        // Act & Assert
        assertThat(repository.existsById(category.getId())).isTrue();
    }

    // =========================================================
    // findByName()
    // =========================================================

    @Test
    @DisplayName("findByName: знаходить категорію за точною назвою")
    void givenExistingCategory_whenFindByName_thenReturnsIt() {
        // Arrange
        Category category =
              Category.builder().id(UUID.randomUUID()).name("Містика").build();
        repository.save(category);

        // Act
        Optional<Category> found = repository.findByName("Містика");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("findByName: повертає empty якщо назва не збігається")
    void givenWrongName_whenFindByName_thenReturnsEmpty() {
        // Act
        Optional<Category> found = repository.findByName("Неіснуюча");

        // Assert
        assertThat(found).isEmpty();
    }

    // =========================================================
    // Identity Map
    // =========================================================

    @Test
    @DisplayName("identityMap: повторний findById не виконує SELECT (з кешу)")
    void givenSavedCategory_whenFindByIdTwice_thenSameInstance() {
        // Arrange
        Category category =
              Category.builder().id(UUID.randomUUID()).name("Кешована").build();
        repository.save(category);

        // Act
        Category first = repository.findById(category.getId()).orElseThrow();
        Category second = repository.findById(category.getId()).orElseThrow();

        // Assert — той самий об'єкт з кешу (==)
        assertThat(first).isSameAs(second);
    }
}
