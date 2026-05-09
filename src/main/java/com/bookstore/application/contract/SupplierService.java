package com.bookstore.application.contract;

import com.bookstore.domain.entity.Supplier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт сервісу для роботи з постачальниками.
 */
public interface SupplierService {

    /**
     * Повертає всіх постачальників.
     *
     * @return список постачальників
     */
    List<Supplier> findAll();

    /**
     * Знаходить постачальника за UUID.
     *
     * @param id UUID постачальника
     * @return Optional з постачальником
     */
    Optional<Supplier> findById(UUID id);

    /**
     * Пошук постачальників за назвою компанії.
     *
     * @param name назва або частина назви компанії
     * @return список знайдених постачальників
     */
    List<Supplier> search(String name);

    /**
     * Створює нового постачальника.
     *
     * @param companyName назва компанії
     * @param phone телефон
     * @param email email
     * @param address адреса
     * @return створений постачальник
     * @throws IllegalArgumentException якщо назва порожня або дані невалідні
     */
    Supplier create(String companyName, String phone, String email, String address);

    /**
     * Оновлює дані постачальника.
     *
     * @param id UUID постачальника
     * @param companyName нова назва компанії
     * @param phone новий телефон
     * @param email новий email
     * @param address нова адреса
     * @return оновлений постачальник
     * @throws IllegalArgumentException якщо постачальник не знайдений або дані невалідні
     */
    Supplier update(UUID id, String companyName, String phone, String email, String address);

    /**
     * Видаляє постачальника.
     *
     * @param id UUID постачальника
     * @throws IllegalArgumentException якщо постачальник не знайдений
     * @throws IllegalStateException якщо існують активні поставки від цього постачальника
     */
    void delete(UUID id);
}