package com.bookstore.application.contract;

import com.bookstore.domain.entity.Sale;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт сервісу для роботи з продажами.
 */
public interface SaleService {

    /**
     * Повертає список усіх продажів.
     *
     * @return список усіх продажів
     */
    List<Sale> findAll();

    /**
     * Знаходить продаж за UUID.
     *
     * @param id UUID продажу
     * @return Optional з продажем
     */
    Optional<Sale> findById(UUID id);

    /**
     * Знаходить усі продажі конкретного клієнта.
     *
     * @param clientId UUID клієнта
     * @return список продажів клієнта
     */
    List<Sale> findByClientId(UUID clientId);

    /**
     * Знаходить усі продажі, оформлені конкретним користувачем (касиром).
     *
     * @param userId UUID користувача
     * @return список продажів користувача
     */
    List<Sale> findByUserId(UUID userId);

    /**
     * Створює нову покупку (чек).
     *
     * @param userId UUID касира/користувача, що оформив продаж
     * @param clientId UUID клієнта (null — анонімна покупка)
     * @param totalAmount загальна сума продажу
     * @param paymentMethod спосіб оплати (CASH, CARD, ONLINE)
     * @return створена покупка
     * @throws IllegalArgumentException якщо дані не валідні (наприклад, сума <= 0)
     * @throws IllegalStateException якщо виникла помилка при оновленні залишків книг
     */
    Sale create(UUID userId, UUID clientId,
          BigDecimal totalAmount, Sale.PaymentMethod paymentMethod);

    /**
     * Видаляє запис про продаж.
     *
     * @param id UUID продажу
     * @throws IllegalArgumentException якщо продаж не знайдено
     */
    void delete(UUID id);
}