package com.bookstore.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.bookstore.domain.entity.User;
import com.bookstore.infrastructure.persistence.impl.JdbcUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Інтеграційні тести для {@link JdbcUserRepository}.
 */
@DisplayName("JdbcUserRepository — інтеграційні тести")
class JdbcUserRepositoryTest extends BaseRepositoryTest {

    private JdbcUserRepository repository;

    @Override
    protected void setUp() {
        repository = new JdbcUserRepository(connectionManager);
    }

    private User buildUser(String username, String email, User.Role role) {
        return User.builder()
              .id(UUID.randomUUID())
              .username(username)
              .passwordHash("$2a$10$hashedpassword")
              .email(email)
              .role(role)
              .build();
    }

    @Test
    @DisplayName("save: зберігає користувача і можна знайти через findById")
    void givenValidUser_whenSave_thenCanBeFoundById() {
        // Arrange
        User user = buildUser("admin_test", "admin@test.com", User.Role.admin);

        // Act
        repository.save(user);

        // Assert
        Optional<User> found = repository.findById(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("admin_test");
        assertThat(found.get().getRole()).isEqualTo(User.Role.admin);
    }

    @Test
    @DisplayName("save: кидає виняток при дублюванні username (UNIQUE constraint)")
    void givenDuplicateUsername_whenSave_thenThrowsException() {
        // Arrange
        User first = buildUser("duplicate", "first@test.com", User.Role.cashier);
        User second = buildUser("duplicate", "second@test.com", User.Role.cashier);
        repository.save(first);

        // Act & Assert
        assertThatThrownBy(() -> repository.save(second))
              .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("save: кидає виняток при невалідній ролі (CHECK constraint)")
    void givenInvalidRole_whenSave_thenThrowsException() throws Exception {
        // Arrange — вставляємо напряму з невалідною роллю
        UUID id = UUID.randomUUID();

        // Act & Assert
        assertThatThrownBy(() -> executeSql(
              String.format(
                    "INSERT INTO users VALUES ('%s','bad','hash','e@e.com','superuser')",
                    id)))
              .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("findAll: повертає всіх користувачів")
    void givenThreeUsers_whenFindAll_thenReturnsAll() {
        // Arrange
        repository.save(buildUser("admin1", "a1@test.com", User.Role.admin));
        repository.save(buildUser("cashier1", "c1@test.com", User.Role.cashier));
        repository.save(buildUser("manager1", "m1@test.com", User.Role.manager));

        // Act
        List<User> all = repository.findAll();

        // Assert
        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("update: оновлює email та роль користувача")
    void givenExistingUser_whenUpdate_thenDataIsChanged() {
        // Arrange
        User user = buildUser("update_me", "old@test.com", User.Role.cashier);
        repository.save(user);

        // Act
        user.setEmail("new@test.com");
        user.setRole(User.Role.manager);
        repository.update(user);

        // Assert
        repository.clearCache();
        User updated = repository.findById(user.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("new@test.com");
        assertThat(updated.getRole()).isEqualTo(User.Role.manager);
    }

    @Test
    @DisplayName("deleteById: видаляє користувача")
    void givenExistingUser_whenDeleteById_thenNotFound() {
        // Arrange
        User user = buildUser("delete_me", "del@test.com", User.Role.cashier);
        repository.save(user);

        // Act
        boolean deleted = repository.deleteById(user.getId());

        // Assert
        assertThat(deleted).isTrue();
        repository.clearCache();
        assertThat(repository.findById(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("findByUsername: знаходить користувача за логіном")
    void givenExistingUser_whenFindByUsername_thenReturnsIt() {
        // Arrange
        User user = buildUser("find_me", "find@test.com", User.Role.admin);
        repository.save(user);

        // Act
        Optional<User> found = repository.findByUsername("find_me");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("findByUsername: повертає empty якщо логін не існує")
    void givenWrongUsername_whenFindByUsername_thenReturnsEmpty() {
        // Act
        Optional<User> found = repository.findByUsername("nonexistent");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByEmail: знаходить користувача за email")
    void givenExistingUser_whenFindByEmail_thenReturnsIt() {
        // Arrange
        User user = buildUser("email_test", "email@test.com", User.Role.manager);
        repository.save(user);

        // Act
        Optional<User> found = repository.findByEmail("email@test.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("email_test");
    }

    @Test
    @DisplayName("isAdmin: повертає true для адміністратора")
    void givenAdminUser_whenIsAdmin_thenReturnsTrue() {
        // Arrange
        User admin = buildUser("admin", "admin@test.com", User.Role.admin);

        // Act & Assert
        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.isCashier()).isFalse();
        assertThat(admin.isManager()).isFalse();
    }

    @Test
    @DisplayName("count: повертає правильну кількість користувачів")
    void givenTwoUsers_whenCount_thenReturnsTwo() {
        // Arrange
        repository.save(buildUser("u1", "u1@test.com", User.Role.admin));
        repository.save(buildUser("u2", "u2@test.com", User.Role.cashier));

        // Act & Assert
        assertThat(repository.count()).isEqualTo(2);
    }
}