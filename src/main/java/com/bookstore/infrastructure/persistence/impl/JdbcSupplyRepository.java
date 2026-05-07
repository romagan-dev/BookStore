package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.domain.entity.Supply;
import com.bookstore.domain.entity.Supplier;
import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import com.bookstore.infrastructure.persistence.util.LazyList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC-реалізація репозиторію для сутності {@link Supply}.
 *
 * <p>Позиції постачання ({@code items}) завантажуються через {@link LazyList} — відкладено при
 * першому зверненні до {@code supply.getItems()}.
 */
public class JdbcSupplyRepository extends AbstractJdbcRepository<Supply, UUID> {

    /** Репозиторій позицій — потрібен для LazyList завантаження. */
    private final JdbcSupplyItemRepository supplyItemRepository;

    private static final String SQL_BASE_SELECT =
          """
          SELECT s.supply_id, s.supply_date, s.total_cost,
                 sp.supplier_id, sp.company_name, sp.phone,
                 sp.email, sp.address
          FROM supplies s
          JOIN suppliers sp ON s.supplier_id = sp.supplier_id
          """;

    private static final String SQL_SELECT_BY_ID =
          SQL_BASE_SELECT + "WHERE s.supply_id = ?";

    private static final String SQL_SELECT_ALL =
          SQL_BASE_SELECT + "ORDER BY s.supply_date DESC";

    private static final String SQL_INSERT =
          """
          INSERT INTO supplies (supply_id, supplier_id, supply_date, total_cost)
          VALUES (?, ?, ?, ?)
          """;

    private static final String SQL_UPDATE =
          """
          UPDATE supplies
          SET supplier_id = ?, supply_date = ?, total_cost = ?
          WHERE supply_id = ?
          """;

    private static final String SQL_FIND_BY_SUPPLIER =
          SQL_BASE_SELECT + "WHERE s.supplier_id = ? ORDER BY s.supply_date DESC";

    /**
     * Конструктор з ін'єкцією залежностей.
     *
     * @param connectionManager менеджер пулу з'єднань
     * @param supplyItemRepository репозиторій позицій для Lazy Loading
     */
    public JdbcSupplyRepository(
          ConnectionManager connectionManager,
          JdbcSupplyItemRepository supplyItemRepository) {
        super(connectionManager);
        this.supplyItemRepository = supplyItemRepository;
    }

    /**
     * Data Mapper: ResultSet → Supply з вкладеним Supplier та LazyList позицій.
     *
     * <p>Позиції завантажуються лише при першому виклику {@code supply.getItems()}.
     */
    @Override
    protected Supply mapRow(ResultSet rs) throws SQLException {
        UUID supplyId = UUID.fromString(rs.getString("supply_id"));

        Supplier supplier =
              Supplier.builder()
                    .id(UUID.fromString(rs.getString("supplier_id")))
                    .companyName(rs.getString("company_name"))
                    .phone(rs.getString("phone"))
                    .email(rs.getString("email"))
                    .address(rs.getString("address"))
                    .build();

        return Supply.builder()
              .id(supplyId)
              .supplier(supplier)
              .supplyDate(rs.getObject("supply_date", LocalDateTime.class))
              .totalCost(rs.getBigDecimal("total_cost"))
              // LazyList: позиції завантажуються лише при supply.getItems()
              .items(new LazyList<>(() -> supplyItemRepository.findBySupplyId(supplyId)))
              .build();
    }

    @Override
    protected String getTableName() {
        return "supplies";
    }

    @Override
    protected String getIdColumnName() {
        return "supply_id";
    }

    @Override
    protected String getSelectByIdSql() {
        return SQL_SELECT_BY_ID;
    }

    @Override
    protected String getSelectAllSql() {
        return SQL_SELECT_ALL;
    }

    @Override
    protected String getInsertSql() {
        return SQL_INSERT;
    }

    @Override
    protected String getUpdateSql() {
        return SQL_UPDATE;
    }

    @Override
    protected void setInsertParams(PreparedStatement stmt, Supply supply) throws SQLException {
        stmt.setString(1, supply.getId().toString());
        stmt.setString(2, supply.getSupplier().getId().toString());
        stmt.setObject(3, supply.getSupplyDate());
        stmt.setBigDecimal(4, supply.getTotalCost());
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, Supply supply) throws SQLException {
        stmt.setString(1, supply.getSupplier().getId().toString());
        stmt.setObject(2, supply.getSupplyDate());
        stmt.setBigDecimal(3, supply.getTotalCost());
        stmt.setString(4, supply.getId().toString());
    }

    @Override
    protected UUID getId(Supply supply) {
        return supply.getId();
    }

    /**
     * Знаходить всі постачання від постачальника.
     *
     * @param supplierId UUID постачальника
     * @return список постачань
     */
    public List<Supply> findBySupplierId(UUID supplierId) {
        List<Supply> list = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_SUPPLIER)) {
            stmt.setString(1, supplierId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Помилка findBySupplierId: " + supplierId, e);
        }
        return list;
    }
}