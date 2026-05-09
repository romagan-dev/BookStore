package com.bookstore.application.impl;

import com.bookstore.application.contract.ClientService;
import com.bookstore.domain.entity.Client;
import com.bookstore.infrastructure.persistence.impl.JdbcClientRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Реалізація сервісу для роботи з клієнтами.
 *
 * <p>Патерни: Dependency Injection, Facade. Інкапсулює бізнес-логіку: валідацію,
 * пошук, перевірку унікальності.
 */
public class ClientServiceImpl implements ClientService {

    private final JdbcClientRepository clientRepository;

    /**
     * Конструктор з Dependency Injection
     *
     * @param clientRepository репозиторій клієнтів
     */
    public ClientServiceImpl(JdbcClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }
    @Override
    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    @Override
    public Optional<Client> findById(UUID id) {
        return clientRepository.findById(id);
    }

    /**
     * Пошук клієнтів за іменем або прізвищем (нечутливий до регістру).
     *
     * @param query рядок пошуку
     * @return відфільтрований список клієнтів
     */
    @Override
    public List<Client> search(String query) {
        if (query == null || query.isBlank()) {
            return clientRepository.findAll();
        }
        String lowerQuery = query.toLowerCase().trim();
        return clientRepository.findAll().stream()
              .filter(c ->
                    c.getFirstName().toLowerCase().contains(lowerQuery)
                          || c.getLastName().toLowerCase().contains(lowerQuery)
                          || (c.getPhone() != null && c.getPhone().contains(lowerQuery))
                          || (c.getEmail() != null
                          && c.getEmail().toLowerCase().contains(lowerQuery)))
              .collect(Collectors.toList());
    }

    /**
     * Створює нового клієнта з валідацією.
     *
     * @param firstName ім'я
     * @param lastName прізвище
     * @param phone телефон (може бути null)
     * @param email email (може бути null)
     * @return створений клієнт
     */
    @Override
    public Client create(String firstName, String lastName, String email, String phone) {
        validateName(firstName, "Ім'я");
        validateName(lastName, "Прізвище");
        validatePhoneUnique(phone, null);
        validateEmailUnique(email, null);

        Client client = Client.builder()
              .id(UUID.randomUUID())
              .firstName(firstName.trim())
              .lastName(lastName.trim())
              .phone(phone != null ? phone.trim() : null)
              .email(email != null ? email.trim() : null)
              .build();

        clientRepository.save(client);
        return client;
    }

    /**
     * Оновлює дані клієнта
     *
     * @param id UUID клієнта
     * @param firstName нове ім'я
     * @param lastName нове прізвище
     * @param phone новий телефон
     * @param email новий email
     * @return оновлений клієнт
     */
    @Override
    public Client update(UUID id, String firstName, String lastName,
          String phone, String email) {
        Client client = clientRepository.findById(id)
              .orElseThrow(() -> new IllegalArgumentException(
                    "Клієнта не знайдено: " + id));

        validateName(firstName, "Ім'я");
        validateName(lastName, "Прізвище");
        validatePhoneUnique(phone, id);
        validateEmailUnique(email, id);

        client.setFirstName(firstName.trim());
        client.setLastName(lastName.trim());
        client.setPhone(phone != null ? phone.trim() : null);
        client.setEmail(email != null ? email.trim() : null);

        clientRepository.update(client);
        return client;
    }

    /** Видаляє клієнта. */
    @Override
    public void delete(UUID id) {
        if (!clientRepository.existsById(id)) {
            throw new IllegalArgumentException("Клієнта не знайдено: " + id);
        }
        clientRepository.deleteById(id);
    }

    /** Перевіряє чи існує клієнт з таким телефоном. */
    @Override
    public boolean existsByPhone(String phone) {
        if (phone == null || phone.isBlank()) return false;
        return clientRepository.findByPhone(phone).isPresent();
    }
    // =========================================================
    // Приватні методи валідації
    // =========================================================
    private void validateName(String name, String fieldName) {
         if (name == null || name.isBlank()) {
              throw new IllegalArgumentException(fieldName + " не може бути порожнім");
          }
         if (name.trim().length() < 2) {
             throw new IllegalArgumentException(fieldName + " має містити мінімум 2 символи");
        }
         if (name.trim().length() > 50) {
            throw new IllegalArgumentException(fieldName + " не може бути довшим за 50 символів");
        }
    }
    private void validatePhoneUnique(String phone, UUID excludeId) {
        if (phone == null || phone.isBlank()) return;
        clientRepository.findByPhone(phone.trim()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException(
                      "Клієнт з телефоном '" + phone + "' вже існує");
            }
        });
    }
    private void validateEmailUnique(String email, UUID excludeId) {
        if (email == null || email.isBlank()) return;
        clientRepository.findByEmail(email.trim()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException(
                      "Клієнт з email '" + email + "' вже існує");
            }
        });
    }

}
