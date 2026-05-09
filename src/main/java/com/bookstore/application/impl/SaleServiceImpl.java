package com.bookstore.application.impl;

import com.bookstore.application.contract.SaleService;
import com.bookstore.domain.entity.Client;
import com.bookstore.domain.entity.Sale;
import com.bookstore.domain.entity.User;
import com.bookstore.infrastructure.persistence.impl.JdbcClientRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSaleItemRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcSaleRepository;
import com.bookstore.infrastructure.persistence.impl.JdbcUserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Реалізація сервісу для роботи з продажами.
 */
public class SaleServiceImpl implements SaleService {

    private final JdbcSaleRepository saleRepository;
    private final JdbcUserRepository userRepository;
    private final JdbcClientRepository clientRepository;

    public SaleServiceImpl(
          JdbcSaleRepository saleRepository,
          JdbcUserRepository userRepository,
          JdbcClientRepository clientRepository) {
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public List<Sale> findAll() {
        return saleRepository.findAll();
    }

    @Override
    public Optional<Sale> findById(UUID id) {
        return saleRepository.findById(id);
    }

    @Override
    public List<Sale> findByClientId(UUID clientId) {
        return saleRepository.findByClientId(clientId);
    }

    @Override
    public List<Sale> findByUserId(UUID userId) {
        return saleRepository.findByUserId(userId);
    }

    @Override
    public Sale create(UUID userId, UUID clientId,
          BigDecimal totalAmount, Sale.PaymentMethod paymentMethod) {
        // Валідація
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Сума не може бути від'ємною");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Спосіб оплати не може бути порожнім");
        }

        User user = userRepository.findById(userId)
              .orElseThrow(() -> new IllegalArgumentException(
                    "Касира не знайдено: " + userId));

        Client client = null;
        if (clientId != null) {
            client = clientRepository.findById(clientId)
                  .orElseThrow(() -> new IllegalArgumentException(
                        "Клієнта не знайдено: " + clientId));
        }

        Sale sale = Sale.builder()
              .id(UUID.randomUUID())
              .user(user)
              .client(client)
              .saleDate(LocalDateTime.now())
              .totalAmount(totalAmount)
              .paymentMethod(paymentMethod)
              .build();

        saleRepository.save(sale);
        return sale;
    }

    @Override
    public void delete(UUID id) {
        if (!saleRepository.existsById(id)) {
            throw new IllegalArgumentException("Продаж не знайдено: " + id);
        }
        saleRepository.deleteById(id);
    }
}