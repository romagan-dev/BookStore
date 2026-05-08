package com.bookstore.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.bookstore.domain.entity.Client;
import com.bookstore.infrastructure.persistence.impl.JdbcClientRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Інтеграційні тести для {@link JdbcClientRepository}.
 */
@DisplayName("JdbcClientRepository — інтеграційні тести")
class JdbcClientRepositoryTest extends BaseRepositoryTest {

    private JdbcClientRepository repository;

    @Override
    protected void setUp() {
        repository = new JdbcClientRepository(connectionManager);
    }

    private Client buildClient(String firstName, String lastName,
          String phone, String email) {
        return Client.builder()
              .id(UUID.randomUUID())
              .firstName(firstName)
              .lastName(lastName)
              .phone(phone)
              .email(email)
              .build();
    }

    @Test
    @DisplayName("save: зберігає клієнта і можна знайти через findById")
    void givenValidClient_whenSave_thenCanBeFoundById() {
        // Arrange
        Client client = buildClient("Іван", "Петренко",
              "+380671234567", "ivan@test.com");

        // Act
        repository.save(client);

        // Assert
        Optional<Client> found = repository.findById(client.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Іван");
        assertThat(found.get().getLastName()).isEqualTo("Петренко");
    }

    @Test
    @DisplayName("save: зберігає клієнта без телефону та email")
    void givenClientWithoutContacts_whenSave_thenSavedSuccessfully() {
        // Arrange
        Client client = Client.builder()
              .id(UUID.randomUUID())
              .firstName("Анонім")
              .lastName("Анонімний")
              .phone(null)
              .email(null)
              .build();

        // Act
        repository.save(client);

        // Assert
        Optional<Client> found = repository.findById(client.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPhone()).isNull();
        assertThat(found.get().getEmail()).isNull();
    }

    @Test
    @DisplayName("save: кидає виняток при дублюванні телефону (UNIQUE constraint)")
    void givenDuplicatePhone_whenSave_thenThrowsException() {
        // Arrange
        Client first = buildClient("Перший", "Клієнт",
              "+380671111111", "first@test.com");
        Client second = buildClient("Другий", "Клієнт",
              "+380671111111", "second@test.com");
        repository.save(first);

        // Act & Assert
        assertThatThrownBy(() -> repository.save(second))
              .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("findAll: повертає всіх клієнтів")
    void givenThreeClients_whenFindAll_thenReturnsAll() {
        // Arrange
        repository.save(buildClient("А", "А", "+1", "a@a.com"));
        repository.save(buildClient("Б", "Б", "+2", "b@b.com"));
        repository.save(buildClient("В", "В", "+3", "c@c.com"));

        // Act
        List<Client> all = repository.findAll();

        // Assert
        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("update: оновлює дані клієнта")
    void givenExistingClient_whenUpdate_thenDataIsChanged() {
        // Arrange
        Client client = buildClient("Старе", "Ім'я",
              "+380670000000", "old@test.com");
        repository.save(client);

        // Act
        client.setFirstName("Нове");
        client.setLastName("Прізвище");
        repository.update(client);

        // Assert
        repository.clearCache();
        Client updated = repository.findById(client.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Нове");
        assertThat(updated.getLastName()).isEqualTo("Прізвище");
    }

    @Test
    @DisplayName("deleteById: видаляє клієнта")
    void givenExistingClient_whenDeleteById_thenNotFound() {
        // Arrange
        Client client = buildClient("Видалити", "Мене",
              "+380679999999", "del@test.com");
        repository.save(client);

        // Act
        boolean deleted = repository.deleteById(client.getId());

        // Assert
        assertThat(deleted).isTrue();
        repository.clearCache();
        assertThat(repository.findById(client.getId())).isEmpty();
    }

    @Test
    @DisplayName("findByPhone: знаходить клієнта за телефоном")
    void givenExistingClient_whenFindByPhone_thenReturnsIt() {
        // Arrange
        Client client = buildClient("Марія", "Іваненко",
              "+380631234567", "maria@test.com");
        repository.save(client);

        // Act
        Optional<Client> found = repository.findByPhone("+380631234567");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Марія");
    }

    @Test
    @DisplayName("findByEmail: знаходить клієнта за email")
    void givenExistingClient_whenFindByEmail_thenReturnsIt() {
        // Arrange
        Client client = buildClient("Олег", "Бондар",
              "+380501234567", "oleg@test.com");
        repository.save(client);

        // Act
        Optional<Client> found = repository.findByEmail("oleg@test.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getLastName()).isEqualTo("Бондар");
    }

    @Test
    @DisplayName("getFullName: повертає правильне повне ім'я")
    void givenClient_whenGetFullName_thenReturnsCorrectString() {
        // Arrange
        Client client = buildClient("Тарас", "Шевченко", null, null);

        // Act & Assert
        assertThat(client.getFullName()).isEqualTo("Тарас Шевченко");
    }

    @Test
    @DisplayName("existsById: повертає false для неіснуючого клієнта")
    void givenNonExistentId_whenExistsById_thenReturnsFalse() {
        // Act & Assert
        assertThat(repository.existsById(UUID.randomUUID())).isFalse();
    }
}