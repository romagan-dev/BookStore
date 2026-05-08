package com.bookstore.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.bookstore.domain.entity.Supplier;
import com.bookstore.domain.entity.Supply;
import com.bookstore.infrastructure.persistence.impl.JdbcSupplierRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSupplyItemRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSupplyRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Інтеграційні тести для {@link JdbcSupplyRepository}.
 */
@DisplayName("JdbcSupplyRepository — інтеграційні тести")
class JdbcSupplyRepositoryTest extends BaseRepositoryTest {

    private JdbcSupplyRepository supplyRepository;
    private JdbcSupplierRepository supplierRepository;

    private Supplier testSupplier;

    @Override
    protected void setUp() {
        supplierRepository = new JdbcSupplierRepository(connectionManager);
        JdbcSupplyItemRepository supplyItemRepository =
              new JdbcSupplyItemRepository(connectionManager);
        supplyRepository = new JdbcSupplyRepository(connectionManager, supplyItemRepository);

        testSupplier = Supplier.builder()
              .id(UUID.randomUUID())
              .companyName("Тестовий постачальник")
              .phone("+380441234567")
              .email("test@supplier.ua")
              .address("вул. Тестова, 1")
              .build();
        supplierRepository.save(testSupplier);
    }

    private Supply buildSupply(BigDecimal totalCost) {
        return Supply.builder()
              .id(UUID.randomUUID())
              .supplier(testSupplier)
              .supplyDate(LocalDateTime.now())
              .totalCost(totalCost)
              .build();
    }

    @Test
    @DisplayName("save: зберігає постачання і можна знайти через findById")
    void givenValidSupply_whenSave_thenCanBeFoundById() {
        // Arrange
        Supply supply = buildSupply(new BigDecimal("10000.00"));

        // Act
        supplyRepository.save(supply);

        // Assert
        Optional<Supply> found = supplyRepository.findById(supply.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTotalCost()).isEqualByComparingTo("10000.00");
        assertThat(found.get().getSupplier().getCompanyName())
              .isEqualTo("Тестовий постачальник");
    }

    @Test
    @DisplayName("findAll: повертає всі постачання відсортовані за датою")
    void givenThreeSupplies_whenFindAll_thenReturnsAll() {
        // Arrange
        supplyRepository.save(buildSupply(new BigDecimal("1000.00")));
        supplyRepository.save(buildSupply(new BigDecimal("2000.00")));
        supplyRepository.save(buildSupply(new BigDecimal("3000.00")));

        // Act
        List<Supply> all = supplyRepository.findAll();

        // Assert
        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("update: оновлює суму постачання")
    void givenExistingSupply_whenUpdate_thenDataIsChanged() {
        // Arrange
        Supply supply = buildSupply(new BigDecimal("5000.00"));
        supplyRepository.save(supply);

        // Act
        supply.setTotalCost(new BigDecimal("7500.00"));
        supplyRepository.update(supply);

        // Assert
        supplyRepository.clearCache();
        Supply updated = supplyRepository.findById(supply.getId()).orElseThrow();
        assertThat(updated.getTotalCost()).isEqualByComparingTo("7500.00");
    }

    @Test
    @DisplayName("deleteById: видаляє постачання")
    void givenExistingSupply_whenDeleteById_thenNotFound() {
        // Arrange
        Supply supply = buildSupply(new BigDecimal("1000.00"));
        supplyRepository.save(supply);

        // Act
        boolean deleted = supplyRepository.deleteById(supply.getId());

        // Assert
        assertThat(deleted).isTrue();
        supplyRepository.clearCache();
        assertThat(supplyRepository.findById(supply.getId())).isEmpty();
    }

    @Test
    @DisplayName("findBySupplierId: повертає постачання від конкретного постачальника")
    void givenSuppliesForSupplier_whenFindBySupplierId_thenReturnsHisSupplies() {
        // Arrange
        supplyRepository.save(buildSupply(new BigDecimal("1000.00")));
        supplyRepository.save(buildSupply(new BigDecimal("2000.00")));

        // Другий постачальник
        Supplier other = Supplier.builder()
              .id(UUID.randomUUID())
              .companyName("Інший постачальник")
              .build();
        supplierRepository.save(other);

        Supply otherSupply = Supply.builder()
              .id(UUID.randomUUID())
              .supplier(other)
              .supplyDate(LocalDateTime.now())
              .totalCost(new BigDecimal("500.00"))
              .build();
        supplyRepository.save(otherSupply);

        // Act
        List<Supply> found = supplyRepository.findBySupplierId(testSupplier.getId());

        // Assert
        assertThat(found).hasSize(2);
        assertThat(found)
              .allMatch(s -> s.getSupplier().getId().equals(testSupplier.getId()));
    }

    @Test
    @DisplayName("count: повертає правильну кількість постачань")
    void givenTwoSupplies_whenCount_thenReturnsTwo() {
        // Arrange
        supplyRepository.save(buildSupply(new BigDecimal("1000.00")));
        supplyRepository.save(buildSupply(new BigDecimal("2000.00")));

        // Act & Assert
        assertThat(supplyRepository.count()).isEqualTo(2);
    }
}