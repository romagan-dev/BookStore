package com.bookstore.application.impl;

import com.bookstore.application.contract.SupplierService;
import com.bookstore.domain.entity.Supplier;
import com.bookstore.infrastructure.persistence.impl.JdbcSupplierRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Реалізація сервісу для роботи з постачальниками.
 */
public class SupplierServiceImpl implements SupplierService {

    private final JdbcSupplierRepository supplierRepository;

    public SupplierServiceImpl(JdbcSupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public List<Supplier> findAll() {
        return supplierRepository.findAll();
    }

    @Override
    public Optional<Supplier> findById(UUID id) {
        return supplierRepository.findById(id);
    }

    @Override
    public List<Supplier> search(String name) {
        if (name == null || name.isBlank()) {
            return supplierRepository.findAll();
        }
        return supplierRepository.findByName(name.trim());
    }

    @Override
    public Supplier create(String companyName, String phone,
          String email, String address) {
        validateCompanyName(companyName);

        Supplier supplier = Supplier.builder()
              .id(UUID.randomUUID())
              .companyName(companyName.trim())
              .phone(phone != null ? phone.trim() : null)
              .email(email != null ? email.trim() : null)
              .address(address != null ? address.trim() : null)
              .build();

        supplierRepository.save(supplier);
        return supplier;
    }

    @Override
    public Supplier update(UUID id, String companyName, String phone,
          String email, String address) {
        Supplier supplier = supplierRepository.findById(id)
              .orElseThrow(() -> new IllegalArgumentException(
                    "Постачальника не знайдено: " + id));

        validateCompanyName(companyName);

        supplier.setCompanyName(companyName.trim());
        supplier.setPhone(phone != null ? phone.trim() : null);
        supplier.setEmail(email != null ? email.trim() : null);
        supplier.setAddress(address != null ? address.trim() : null);

        supplierRepository.update(supplier);
        return supplier;
    }

    @Override
    public void delete(UUID id) {
        if (!supplierRepository.existsById(id)) {
            throw new IllegalArgumentException("Постачальника не знайдено: " + id);
        }
        supplierRepository.deleteById(id);
    }

    private void validateCompanyName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                  "Назва компанії не може бути порожньою");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException(
                  "Назва компанії не може бути довшою за 100 символів");
        }
    }
}