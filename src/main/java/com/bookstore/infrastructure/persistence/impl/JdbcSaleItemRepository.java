package com.bookstore.infrastructure.persistence.impl;

import com.bookstore.domain.entity.Book;
import com.bookstore.domain.entity.Category;
import com.bookstore.domain.entity.Sale;
import com.bookstore.domain.entity.SaleItem;
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
 * JDBC-реалізація репозиторію для сутності {@link SaleItem}.
 *
 * <p>Читаючі операції використовують JOIN для завантаження Book з Category. Стандартні CRUD
 * успадковані від {@link AbstractJdbcRepository}.
 */
public class JdbcSaleItemRepository extends AbstractJdbcRepository<SaleItem, UUID> {

    private static final String SQL_BASE_SELECT =
          """
          SELECT si.sale_item_id, si.sale_id, si.quantity, si.unit_price,
                 b.book_id, b.title, b.isbn, b.price, b.stock_quantity, b.description,
                 c.category_id, c.name AS category_name, c.description AS category_description
          FROM sale_items si
          JOIN books b ON si.book_id = b.book_id
          JOIN categories c ON b.category_id = c.category_id
          """;

    private static final String SQL_SELECT_BY_ID =
          SQL_BASE_SELECT + "WHERE si.sale_item_id = ?";

    private static final String SQL_SELECT_ALL =
          SQL_BASE_SELECT + "ORDER BY si.sale_item_id";

    private static final String SQL_INSERT =
          """
          INSERT INTO sale_items (sale_item_id, sale_id, book_id, quantity, unit_price)
          VALUES (?, ?, ?, ?, ?)
          """;

    private static final String SQL_UPDATE =
          """
          UPDATE sale_items
          SET sale_id = ?, book_id = ?, quantity = ?, unit_price = ?
          WHERE sale_item_id = ?
          """;

    private static final String SQL_FIND_BY_SALE =
          SQL_BASE_SELECT + "WHERE si.sale_id = ? ORDER BY si.sale_item_id";

    public JdbcSaleItemRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    protected SaleItem mapRow(ResultSet rs) throws SQLException {
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

        // Sale завантажується лише з ID (уникаємо циклічних залежностей)
        Sale sale =
              Sale.builder()
                    .id(UUID.fromString(rs.getString("sale_id")))
                    .build();

        return SaleItem.builder()
              .id(UUID.fromString(rs.getString("sale_item_id")))
              .sale(sale)
              .book(book)
              .quantity(rs.getInt("quantity"))
              .unitPrice(rs.getBigDecimal("unit_price"))
              .build();
    }

    @Override
    protected String getTableName() {
        return "sale_items";
    }

    @Override
    protected String getIdColumnName() {
        return "sale_item_id";
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
    protected void setInsertParams(PreparedStatement stmt, SaleItem item) throws SQLException {
        stmt.setString(1, item.getId().toString());
        stmt.setString(2, item.getSale().getId().toString());
        stmt.setString(3, item.getBook().getId().toString());
        stmt.setInt(4, item.getQuantity());
        stmt.setBigDecimal(5, item.getUnitPrice());
    }

    @Override
    protected void setUpdateParams(PreparedStatement stmt, SaleItem item) throws SQLException {
        stmt.setString(1, item.getSale().getId().toString());
        stmt.setString(2, item.getBook().getId().toString());
        stmt.setInt(3, item.getQuantity());
        stmt.setBigDecimal(4, item.getUnitPrice());
        stmt.setString(5, item.getId().toString());
    }

    @Override
    protected UUID getId(SaleItem item) {
        return item.getId();
    }

    /**
     * Знаходить всі позиції конкретної покупки.
     *
     * @param saleId UUID покупки
     * @return список позицій
     */
    public List<SaleItem> findBySaleId(UUID saleId) {
        List<SaleItem> list = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
              PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_SALE)) {

            stmt.setString(1, saleId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Помилка findBySaleId для: " + saleId, e);
        }
        return list;
    }
}