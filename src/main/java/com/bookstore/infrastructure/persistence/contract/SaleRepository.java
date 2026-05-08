package com.bookstore.infrastructure.persistence.contract;

import com.bookstore.domain.entity.Sale;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends Repository<Sale> {
    List<Sale> findByPeriod(LocalDateTime start, LocalDateTime end);
    List<Sale> findByClientId(java.util.UUID clientId);
}