package com.bookstore.application.contract;

import com.bookstore.domain.entity.Supply;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт сервісу для роботи з постачаннями.
 */
public interface SupplyService {

    List<Supply> findAll();

    Optional<Supply> findById(UUID id);

    List<Supply> findBySupplierId(UUID supplierId);

    /**
     * Створює нове постачання.
     *
     * @param supplierId UUID постачальника
     * @param totalCost загальна вартість
     * @return створене постачання
     */
    Supply create(UUID supplierId, BigDecimal totalCost);

    void delete(UUID id);
}