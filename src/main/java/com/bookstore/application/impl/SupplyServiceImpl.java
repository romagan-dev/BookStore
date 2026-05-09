package com.bookstore.application.impl;

import com.bookstore.application.contract.SupplyService;
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

/**
 * Реалізація сервісу для роботи з постачаннями.
 */
public class SupplyServiceImpl implements SupplyService {

    private final JdbcSupplyRepository supplyRepository;
    private final JdbcSupplierRepository supplierRepository;

    public SupplyServiceImpl(
          JdbcSupplyRepository supplyRepository,
          JdbcSupplierRepository supplierRepository) {
        this.supplyRepository = supplyRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    public List<Supply> findAll() {
        return supplyRepository.findAll();
    }

    @Override
    public Optional<Supply> findById(UUID id) {
        return supplyRepository.findById(id);
    }

    @Override
    public List<Supply> findBySupplierId(UUID supplierId) {
        return supplyRepository.findBySupplierId(supplierId);
    }

    @Override
    public Supply create(UUID supplierId, BigDecimal totalCost) {
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Вартість постачання має бути більше 0");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
              .orElseThrow(() -> new IllegalArgumentException(
                    "Постачальника не знайдено: " + supplierId));

        Supply supply = Supply.builder()
              .id(UUID.randomUUID())
              .supplier(supplier)
              .supplyDate(LocalDateTime.now())
              .totalCost(totalCost)
              .build();

        supplyRepository.save(supply);
        return supply;
    }

    @Override
    public void delete(UUID id) {
        if (!supplyRepository.existsById(id)) {
            throw new IllegalArgumentException("Постачання не знайдено: " + id);
        }
        supplyRepository.deleteById(id);
    }
}