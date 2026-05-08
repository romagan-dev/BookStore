package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.domain.entity.Book;
import com.bookstore.domain.entity.Category;
import com.bookstore.domain.entity.Supply;
import com.bookstore.domain.entity.SupplyItem;
import com.bookstore.infrastructure.persistence.util.ConnectionManager;
import com.bookstore.infrastructure.persistence.util.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC-реалізація репозиторію для сутності {@link SupplyItem}.
 *
 * <p>Читаючі операції використовують JOIN для завантаження Book з Category. Стандартні CRUD
 * успадковані від {@link AbstractJdbcRepository}.
 */
public class JdbcSupplyItemRepository extends AbstractJdbcRepository<SupplyItem, UUID> {

    private static final String SQL_BASE_SELECT =
          """
          SELECT si.supply_item_id, si.supply_id, si.quantity, si.unit_cost,
                 b.book_id, b.title, b.isbn, b.price, b.stock_quantity, b.description,
                 c.category_id, c.name AS category_name, c.description AS category_description
          FROM supply_items si
          JOIN books b ON si.book_id = b.book_id
          JOIN categories c ON b.category_id = c.category_id
          """;

    private static final String SQL_SELECT_BY_ID =
          SQL_BASE_SELECT + "WHERE si.supply_item_id = ?";

    private static final String SQL_SELECT_ALL =
          SQL_BASE_SELECT + "ORDER BY si.supply_item_id";

    private static final String SQL_INSERT =
          """
          INSERT INTO supply_items (supply_item_id, supply_id, book_id, quantity, unit_cost)
          VALUES (?, ?, ?, ?, ?)
          """;

    private static final String SQL_UPDATE =
          """
          UPDATE supply_items
          SET supply_id = ?, book_id = ?, quantity = ?, unit_cost = ?
          WHERE supply_item_id = ?
          """;

    private static final String SQL_FIND_BY_SUPPLY =
          SQL_BASE_SELECT + "WHERE si.supply_id = ? ORDER BY si.supply_item_id";

    public JdbcSupplyItemRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    protected SupplyItem mapRow(ResultSet rs) throws SQLException {
        Category category =
              Category.builder()
                    .id(UUID.fromString(rs.getString("category_id")))
                    .name(rs.getString("category_name"))
                    .description(rs.getString("category_description"))
                    .build();

        Book book =
              Book.builder()
                    .id(UUID.fromString(rs.getString("book_id")))
                    .title(rs.getString("title"))
                    .isbn(rs.getString("isbn"))
                    .price(rs.getBigDecimal("price"))
                    .stockQuantity(rs.getInt("stock_quantity"))
                    .description(rs.getString("description"))
                    .category(category)
                    .authors(new ArrayList<>())
                    .build();

        Supply supply =
              Supply.builder()
                    .id(UUID.fromString(rs.getString("supply_id")))
                    .build();

        return SupplyItem.builder()
              .id(UUID.fromString(rs.getString("supply_item_id")))
              .supply(supply)
              .book(book)
              .quantity(rs.getInt("quantity"))
              .unitCost(rs.getBigDecimal("unit_cost"))
              .build();
    }

    @Override
    protected String getTableName() {
        return "supply_items";
    }

    @Override
    protected String getIdColumnName() {
        return "supply_item_id";
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
    protected void setInsertParams(PreparedStatement stmt, SupplyItem item) throws SQLException {
        stmt.setString(1, item.getId().toString());
        stmt.setString(2, item.getSupply().getId().toString());
        stmt.setString(3, item.getBook().getId().toString());
        stmt.setInt(4, item.getQuantity());
        stmt.setBigDecimal(5, item.getUnitCost());
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, SupplyItem item) throws SQLException {
        stmt.setString(1, item.getSupply().getId().toString());
        stmt.setString(2, item.getBook().getId().toString());
        stmt.setInt(3, item.getQuantity());
        stmt.setBigDecimal(4, item.getUnitCost());
        stmt.setString(5, item.getId().toString());
    }

    @Override
    protected UUID getId(SupplyItem item) {
        return item.getId();
    }

    /**
     * Знаходить всі позиції конкретного постачання.
     *
     * @param supplyId UUID постачання
     * @return список позицій
     */
    public List<SupplyItem> findBySupplyId(UUID supplyId) {
        List<SupplyItem> list = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_SUPPLY)) {

            stmt.setString(1, supplyId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка findBySupplyId для: " + supplyId, e);
        }
        return list;
    }
}