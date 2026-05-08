package com.bookstore.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.bookstore.domain.entity.Supplier;
import com.bookstore.infrastructure.persistence.impl.JdbcSupplierRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Інтеграційні тести для {@link JdbcSupplierRepository}.
 */
@DisplayName("JdbcSupplierRepository — інтеграційні тести")
class JdbcSupplierRepositoryTest extends BaseRepositoryTest {

    private JdbcSupplierRepository repository;

    @Override
    protected void setUp() {
        repository = new JdbcSupplierRepository(connectionManager);
    }

    private Supplier buildSupplier(String companyName) {
        return Supplier.builder()
              .id(UUID.randomUUID())
              .companyName(companyName)
              .phone("+380441234567")
              .email("info@" + companyName.toLowerCase() + ".ua")
              .address("вул. Книжкова, 1, Київ")
              .build();
    }

    @Test
    @DisplayName("save: зберігає постачальника і можна знайти через findById")
    void givenValidSupplier_whenSave_thenCanBeFoundById() {
        // Arrange
        Supplier supplier = buildSupplier("Старий Лев");

        // Act
        repository.save(supplier);

        // Assert
        Optional<Supplier> found = repository.findById(supplier.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCompanyName()).isEqualTo("Старий Лев");
        assertThat(found.get().getAddress()).isEqualTo("вул. Книжкова, 1, Київ");
    }

    @Test
    @DisplayName("save: зберігає постачальника без контактів")
    void givenSupplierWithoutContacts_whenSave_thenSavedSuccessfully() {
        // Arrange
        Supplier supplier = Supplier.builder()
              .id(UUID.randomUUID())
              .companyName("Мінімальний постачальник")
              .phone(null)
              .email(null)
              .address(null)
              .build();

        // Act
        repository.save(supplier);

        // Assert
        Optional<Supplier> found = repository.findById(supplier.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPhone()).isNull();
    }

    @Test
    @DisplayName("findAll: повертає всіх постачальників відсортованих за назвою")
    void givenThreeSuppliers_whenFindAll_thenReturnsSortedByName() {
        // Arrange
        repository.save(buildSupplier("Фоліо"));
        repository.save(buildSupplier("А-БА-БА"));
        repository.save(buildSupplier("Основи"));

        // Act
        List<Supplier> all = repository.findAll();

        // Assert
        assertThat(all).hasSize(3);
        assertThat(all).extracting(Supplier::getCompanyName)
              .containsExactly("А-БА-БА", "Основи", "Фоліо");
    }

    @Test
    @DisplayName("update: оновлює дані постачальника")
    void givenExistingSupplier_whenUpdate_thenDataIsChanged() {
        // Arrange
        Supplier supplier = buildSupplier("Стара назва");
        repository.save(supplier);

        // Act
        supplier.setCompanyName("Нова назва");
        supplier.setPhone("+380501111111");
        repository.update(supplier);

        // Assert
        repository.clearCache();
        Supplier updated = repository.findById(supplier.getId()).orElseThrow();
        assertThat(updated.getCompanyName()).isEqualTo("Нова назва");
        assertThat(updated.getPhone()).isEqualTo("+380501111111");
    }

    @Test
    @DisplayName("deleteById: видаляє постачальника")
    void givenExistingSupplier_whenDeleteById_thenNotFound() {
        // Arrange
        Supplier supplier = buildSupplier("Видалити");
        repository.save(supplier);

        // Act
        boolean deleted = repository.deleteById(supplier.getId());

        // Assert
        assertThat(deleted).isTrue();
        repository.clearCache();
        assertThat(repository.findById(supplier.getId())).isEmpty();
    }

    @Test
    @DisplayName("findByName: знаходить постачальників за частиною назви")
    void givenSuppliers_whenFindByName_thenReturnsMatching() {
        // Arrange
        repository.save(buildSupplier("Видавництво Фоліо"));
        repository.save(buildSupplier("Видавництво Основи"));
        repository.save(buildSupplier("Старий Лев"));

        // Act
        List<Supplier> found = repository.findByName("Видавництво");

        // Assert
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Supplier::getCompanyName)
              .containsExactlyInAnyOrder("Видавництво Фоліо", "Видавництво Основи");
    }

    @Test
    @DisplayName("findByName: пошук нечутливий до регістру")
    void givenSupplier_whenFindByNameLowerCase_thenReturnsIt() {
        // Arrange
        repository.save(buildSupplier("Старий Лев"));

        // Act
        List<Supplier> found = repository.findByName("старий");

        // Assert
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCompanyName()).isEqualTo("Старий Лев");
    }

    @Test
    @DisplayName("count: повертає правильну кількість постачальників")
    void givenTwoSuppliers_whenCount_thenReturnsTwo() {
        // Arrange
        repository.save(buildSupplier("Перший"));
        repository.save(buildSupplier("Другий"));

        // Act & Assert
        assertThat(repository.count()).isEqualTo(2);
    }
}