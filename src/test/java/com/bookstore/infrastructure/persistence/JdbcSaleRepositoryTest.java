package com.bookstore.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.bookstore.domain.entity.Client;
import com.bookstore.domain.entity.Sale;
import com.bookstore.domain.entity.User;
import com.bookstore.infrastructure.persistence.impl.JdbcClientRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSaleItemRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSaleRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcUserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Інтеграційні тести для {@link JdbcSaleRepository}.
 */
@DisplayName("JdbcSaleRepository — інтеграційні тести")
class JdbcSaleRepositoryTest extends BaseRepositoryTest {

    private JdbcSaleRepository saleRepository;
    private JdbcUserRepository userRepository;
    private JdbcClientRepository clientRepository;

    private User testUser;
    private Client testClient;

    @Override
    protected void setUp() {
        userRepository = new JdbcUserRepository(connectionManager);
        clientRepository = new JdbcClientRepository(connectionManager);
        JdbcSaleItemRepository saleItemRepository =
              new JdbcSaleItemRepository(connectionManager);
        saleRepository = new JdbcSaleRepository(connectionManager, saleItemRepository);

        // Спільні тестові дані
        testUser = User.builder()
              .id(UUID.randomUUID())
              .username("cashier_test")
              .passwordHash("hash")
              .email("cashier@test.com")
              .role(User.Role.cashier)
              .build();
        userRepository.save(testUser);

        testClient = Client.builder()
              .id(UUID.randomUUID())
              .firstName("Тест")
              .lastName("Клієнт")
              .phone("+380671111111")
              .email("client@test.com")
              .build();
        clientRepository.save(testClient);
    }

    private Sale buildSale(Client client, Sale.PaymentMethod method) {
        return Sale.builder()
              .id(UUID.randomUUID())
              .user(testUser)
              .client(client)
              .saleDate(LocalDateTime.now())
              .totalAmount(new BigDecimal("500.00"))
              .paymentMethod(method)
              .build();
    }

    @Test
    @DisplayName("save: зберігає покупку з клієнтом")
    void givenSaleWithClient_whenSave_thenCanBeFoundById() {
        // Arrange
        Sale sale = buildSale(testClient, Sale.PaymentMethod.card);

        // Act
        saleRepository.save(sale);

        // Assert
        Optional<Sale> found = saleRepository.findById(sale.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPaymentMethod()).isEqualTo(Sale.PaymentMethod.card);
        assertThat(found.get().getClient()).isNotNull();
        assertThat(found.get().getClient().getFirstName()).isEqualTo("Тест");
    }

    @Test
    @DisplayName("save: зберігає анонімну покупку (client = null)")
    void givenAnonymousSale_whenSave_thenClientIsNull() {
        // Arrange
        Sale sale = buildSale(null, Sale.PaymentMethod.cash);

        // Act
        saleRepository.save(sale);

        // Assert
        saleRepository.clearCache();
        Optional<Sale> found = saleRepository.findById(sale.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getClient()).isNull();
        assertThat(found.get().isAnonymous()).isTrue();
    }

    @Test
    @DisplayName("findAll: повертає всі покупки відсортовані за датою")
    void givenThreeSales_whenFindAll_thenReturnsAll() {
        // Arrange
        saleRepository.save(buildSale(testClient, Sale.PaymentMethod.cash));
        saleRepository.save(buildSale(null, Sale.PaymentMethod.card));
        saleRepository.save(buildSale(testClient, Sale.PaymentMethod.online));

        // Act
        List<Sale> all = saleRepository.findAll();

        // Assert
        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("findByClientId: повертає покупки конкретного клієнта")
    void givenSalesForClient_whenFindByClientId_thenReturnsHisSales() {
        // Arrange
        saleRepository.save(buildSale(testClient, Sale.PaymentMethod.card));
        saleRepository.save(buildSale(testClient, Sale.PaymentMethod.cash));
        saleRepository.save(buildSale(null, Sale.PaymentMethod.online));

        // Act
        List<Sale> clientSales = saleRepository.findByClientId(testClient.getId());

        // Assert
        assertThat(clientSales).hasSize(2);
        assertThat(clientSales)
              .allMatch(s -> s.getClient() != null
                    && s.getClient().getId().equals(testClient.getId()));
    }

    @Test
    @DisplayName("findByUserId: повертає покупки оформлені касиром")
    void givenSalesForUser_whenFindByUserId_thenReturnsHisSales() {
        // Arrange
        saleRepository.save(buildSale(testClient, Sale.PaymentMethod.card));
        saleRepository.save(buildSale(null, Sale.PaymentMethod.cash));

        // Act
        List<Sale> userSales = saleRepository.findByUserId(testUser.getId());

        // Assert
        assertThat(userSales).hasSize(2);
        assertThat(userSales)
              .allMatch(s -> s.getUser().getId().equals(testUser.getId()));
    }

    @Test
    @DisplayName("deleteById: видаляє покупку")
    void givenExistingSale_whenDeleteById_thenNotFound() {
        // Arrange
        Sale sale = buildSale(testClient, Sale.PaymentMethod.card);
        saleRepository.save(sale);

        // Act
        boolean deleted = saleRepository.deleteById(sale.getId());

        // Assert
        assertThat(deleted).isTrue();
        saleRepository.clearCache();
        assertThat(saleRepository.findById(sale.getId())).isEmpty();
    }

    @Test
    @DisplayName("isAnonymous: повертає true для анонімної покупки")
    void givenAnonymousSale_whenIsAnonymous_thenReturnsTrue() {
        // Arrange
        Sale sale = buildSale(null, Sale.PaymentMethod.cash);

        // Act & Assert
        assertThat(sale.isAnonymous()).isTrue();
    }

    @Test
    @DisplayName("isAnonymous: повертає false якщо клієнт вказаний")
    void givenSaleWithClient_whenIsAnonymous_thenReturnsFalse() {
        // Arrange
        Sale sale = buildSale(testClient, Sale.PaymentMethod.card);

        // Act & Assert
        assertThat(sale.isAnonymous()).isFalse();
    }
}