package com.bookstore.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.bookstore.domain.entity.Author;
import com.bookstore.infrastructure.persistence.impl.JdbcAuthorRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Інтеграційні тести для {@link JdbcAuthorRepository}.
 */
@DisplayName("JdbcAuthorRepository — інтеграційні тести")
class JdbcAuthorRepositoryTest extends BaseRepositoryTest {

    private JdbcAuthorRepository repository;

    @Override
    protected void setUp() {
        repository = new JdbcAuthorRepository(connectionManager);
    }

    @Test
    @DisplayName("save: зберігає автора і повертає через findById")
    void givenValidAuthor_whenSave_thenCanBeFoundById() {
        // Arrange
        Author author =
              Author.builder()
                    .id(UUID.randomUUID())
                    .firstName("Іван")
                    .lastName("Франко")
                    .build();

        // Act
        repository.save(author);

        // Assert
        Optional<Author> found = repository.findById(author.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Іван");
        assertThat(found.get().getLastName()).isEqualTo("Франко");
    }

    @Test
    @DisplayName("findAll: повертає всіх авторів відсортованих за прізвищем")
    void givenThreeAuthors_whenFindAll_thenReturnsSortedByLastName() {
        // Arrange
        repository.save(Author.builder().id(UUID.randomUUID())
              .firstName("Сергій").lastName("Жадан").build());
        repository.save(Author.builder().id(UUID.randomUUID())
              .firstName("Ліна").lastName("Костенко").build());
        repository.save(Author.builder().id(UUID.randomUUID())
              .firstName("Оксана").lastName("Забужко").build());

        // Act
        List<Author> all = repository.findAll();

        // Assert
        assertThat(all).hasSize(3);
        assertThat(all).extracting(Author::getLastName)
              .containsExactly("Жадан", "Забужко", "Костенко");
    }

    @Test
    @DisplayName("update: оновлює ім'я та прізвище автора")
    void givenExistingAuthor_whenUpdate_thenDataIsChanged() {
        // Arrange
        Author author =
              Author.builder()
                    .id(UUID.randomUUID())
                    .firstName("Старе")
                    .lastName("Ім'я")
                    .build();
        repository.save(author);

        // Act
        author.setFirstName("Нове");
        author.setLastName("Прізвище");
        repository.update(author);

        // Assert
        Author updated = repository.findById(author.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Нове");
        assertThat(updated.getLastName()).isEqualTo("Прізвище");
    }

    @Test
    @DisplayName("deleteById: видаляє автора")
    void givenExistingAuthor_whenDeleteById_thenNotFound() {
        // Arrange
        Author author =
              Author.builder()
                    .id(UUID.randomUUID())
                    .firstName("Видалити")
                    .lastName("Мене")
                    .build();
        repository.save(author);

        // Act
        boolean deleted = repository.deleteById(author.getId());

        // Assert
        assertThat(deleted).isTrue();
        assertThat(repository.findById(author.getId())).isEmpty();
    }

    @Test
    @DisplayName("findByLastName: знаходить за частиною прізвища")
    void givenAuthors_whenFindByLastName_thenReturnsMatching() {
        // Arrange
        repository.save(Author.builder().id(UUID.randomUUID())
              .firstName("Іван").lastName("Франко").build());
        repository.save(Author.builder().id(UUID.randomUUID())
              .firstName("Леся").lastName("Франківна").build());
        repository.save(Author.builder().id(UUID.randomUUID())
              .firstName("Тарас").lastName("Шевченко").build());

        // Act
        List<Author> found = repository.findByLastName("Франк");

        // Assert
        assertThat(found).hasSize(2);
        assertThat(found).extracting(Author::getLastName)
              .containsExactlyInAnyOrder("Франко", "Франківна");
    }

    @Test
    @DisplayName("findByFullName: знаходить автора за точним іменем та прізвищем")
    void givenAuthor_whenFindByFullName_thenReturnsIt() {
        // Arrange
        Author author = Author.builder()
              .id(UUID.randomUUID())
              .firstName("Василь")
              .lastName("Шкляр")
              .build();
        repository.save(author);

        // Act
        Optional<Author> found = repository.findByFullName("Шкляр", "Василь");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(author.getId());
    }

    @Test
    @DisplayName("getFullName: повертає правильне повне ім'я")
    void givenAuthor_whenGetFullName_thenReturnsCorrectString() {
        // Arrange
        Author author = Author.builder()
              .id(UUID.randomUUID())
              .firstName("Ліна")
              .lastName("Костенко")
              .build();

        // Act & Assert
        assertThat(author.getFullName()).isEqualTo("Ліна Костенко");
    }
}