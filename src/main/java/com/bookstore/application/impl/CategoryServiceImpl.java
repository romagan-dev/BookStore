package com.bookstore.application.impl;

import com.bookstore.application.contract.CategoryService;
import com.bookstore.domain.entity.Category;
import com.bookstore.infrastructure.persistence.impl.JdbcCategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Реалізація сервісу для роботи з категоріями.
 */
public class CategoryServiceImpl implements CategoryService {

    private final JdbcCategoryRepository categoryRepository;

    public CategoryServiceImpl(JdbcCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return categoryRepository.findById(id);
    }

    @Override
    public List<Category> search(String query) {
        if (query == null || query.isBlank()) {
            return categoryRepository.findAll();
        }
        String lowerQuery = query.toLowerCase().trim();
        return categoryRepository.findAll().stream()
              .filter(c -> c.getName().toLowerCase().contains(lowerQuery))
              .collect(Collectors.toList());
    }

    @Override
    public Optional<Category> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return categoryRepository.findByName(name.trim());
    }

    @Override
    public Category create(String name, String description) {
        validateName(name);

        if (categoryRepository.findByName(name.trim()).isPresent()) {
            throw new IllegalArgumentException(
                  "Категорія з назвою '" + name + "' вже існує");
        }

        Category category = Category.builder()
              .id(UUID.randomUUID())
              .name(name.trim())
              .description(description != null ? description.trim() : null)
              .build();

        categoryRepository.save(category);
        return category;
    }

    @Override
    public Category update(UUID id, String name, String description) {
        Category category = categoryRepository.findById(id)
              .orElseThrow(() -> new IllegalArgumentException(
                    "Категорію не знайдено: " + id));

        validateName(name);

        categoryRepository.findByName(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException(
                      "Категорія з назвою '" + name + "' вже існує");
            }
        });

        category.setName(name.trim());
        category.setDescription(description != null ? description.trim() : null);
        categoryRepository.update(category);
        return category;
    }

    @Override
    public void delete(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Категорію не знайдено: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Назва категорії не може бути порожньою");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException(
                  "Назва категорії не може бути довшою за 100 символів");
        }
    }
}