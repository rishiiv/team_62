package com.team62.controller;

import com.team62.db.Database;
import com.team62.model.Employee;
import com.team62.model.InventoryItem;
import com.team62.model.MenuItem;
import com.team62.model.SalesOrder;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller class - handles business logic and coordinates between Model and View.
 * This implementation backs the UI with the shared Postgres database.
 */
public class MainController {

    // In-memory list keeps a lightweight history of orders for this session;
    // all persisted data lives in Postgres.
    private final List<SalesOrder> salesOrders = new ArrayList<>();
    
    public MainController() {}
    
    // ============================
    // Menu items ("Item" table)
    // ============================
    
    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        String sql = """
                SELECT item_id, name, category, price, is_active
                  FROM "Item"
                 ORDER BY name
                """;
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            
            int uiId = 1;
            while (rs.next()) {
                MenuItem mi = new MenuItem(
                        uiId++,
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getBigDecimal("price"),
                        rs.getBoolean("is_active"));
                Object dbId = rs.getObject("item_id");
                mi.setDbId(dbId != null ? dbId.toString() : null);
                items.add(mi);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
    
    public void addMenuItem(MenuItem item) {
        String sql = """
                INSERT INTO "Item" (item_id, name, category, price, is_active, milk, ice, sugar, toppings)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {
            
            UUID id = UUID.randomUUID();
            item.setDbId(id.toString());
            
            ps.setObject(1, id);
            ps.setString(2, item.getName());
            ps.setString(3, item.getCategory());
            ps.setBigDecimal(4, item.getBasePrice());
            ps.setBoolean(5, item.isActive());
            // Simple defaults for customization fields required by schema
            ps.setString(6, "whole");        // milk
            ps.setInt(7, 1);                 // ice (0,1,2)
            ps.setFloat(8, 1.0f);            // sugar (0.0-1.0)
            ps.setArray(9, conn.createArrayOf("text", new Object[]{})); // toppings
            
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateMenuItem(MenuItem item) {
        if (item.getDbId() == null) {
            return;
        }
        String sql = """
                UPDATE "Item"
                   SET name = ?, category = ?, price = ?, is_active = ?
                 WHERE item_id = ?
                """;
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, item.getName());
            ps.setString(2, item.getCategory());
            ps.setBigDecimal(3, item.getBasePrice());
            ps.setBoolean(4, item.isActive());
            ps.setObject(5, UUID.fromString(item.getDbId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return null on success, or an error message if delete failed (e.g. item is referenced by orders).
     */
    public String deleteMenuItem(MenuItem item) {
        if (item.getDbId() == null) {
            return "No database id.";
        }
        String sql = "DELETE FROM \"Item\" WHERE item_id = ?";
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(item.getDbId()));
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("fk_order_item_item")) {
                return "Cannot delete: this item appears in existing orders.";
            }
            e.printStackTrace();
            return e.getMessage() != null ? e.getMessage() : "Delete failed.";
        }
    }
    
    // ============================
    // Inventory (read from \"Inventory_Quantity\" / Item_Inventory / Item)
    // ============================
    
    public List<InventoryItem> getAllInventoryItems() {
        List<InventoryItem> items = new ArrayList<>();
        String sql = """
                SELECT iq.inventory_id,
                       i.name,
                       iq.quantity
                  FROM \"Inventory_Quantity\" iq
                  JOIN "Item_Inventory" ii ON ii.inventory_id = iq.inventory_id
                  JOIN "Item" i ON i.item_id = ii.item_id
                 ORDER BY i.name
                """;
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            
            int uiId = 1;
            while (rs.next()) {
                InventoryItem inv = new InventoryItem(
                        uiId++,
                        rs.getString("name"),
                        "", // unit not tracked in schema
                        rs.getInt("quantity"),
                        0,  // parLevel not tracked
                        0,  // reorderPoint not tracked
                        true);
                Object dbId = rs.getObject("inventory_id");
                inv.setDbId(dbId != null ? dbId.toString() : null);
                items.add(inv);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
    
    public void addInventoryItem(InventoryItem item) {
        // When adding inventory from the UI, require that the name matches an existing Item.
        String findItemSql = """
                SELECT item_id
                  FROM "Item"
                 WHERE name = ?
                 LIMIT 1
                """;
        String insertInvSql = """
                INSERT INTO \"Inventory_Quantity\" (inventory_id, quantity, last_restocked, last_quantity)
                VALUES (?, ?, CURRENT_DATE, CURRENT_DATE)
                """;
        String insertLinkSql = """
                INSERT INTO "Item_Inventory" (id, inventory_id, item_id)
                VALUES (?, ?, ?)
                """;
        try (var conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            
            UUID itemId;
            try (var ps = conn.prepareStatement(findItemSql)) {
                ps.setString(1, item.getName());
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("No existing Item found with name '" + item.getName() + "'");
                    }
                    itemId = (UUID) rs.getObject("item_id");
                }
            }
            
            UUID invId = UUID.randomUUID();
            UUID linkId = UUID.randomUUID();
            
            try (var ps = conn.prepareStatement(insertInvSql)) {
                ps.setObject(1, invId);
                ps.setInt(2, item.getCurrentQuantity());
                ps.executeUpdate();
            }
            
            try (var ps = conn.prepareStatement(insertLinkSql)) {
                ps.setObject(1, linkId);
                ps.setObject(2, invId);
                ps.setObject(3, itemId);
                ps.executeUpdate();
            }
            
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateInventoryItem(InventoryItem item) {
        if (item.getDbId() == null) {
            return;
        }
        String sql = """
                UPDATE \"Inventory_Quantity\"
                   SET quantity = ?, last_quantity = CURRENT_DATE
                 WHERE inventory_id = ?
                """;
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getCurrentQuantity());
            ps.setObject(2, UUID.fromString(item.getDbId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return null on success, or an error message if delete failed.
     */
    public String deleteInventoryItem(InventoryItem item) {
        if (item.getDbId() == null) {
            return "No database id.";
        }
        String sql = "DELETE FROM \"Inventory_Quantity\" WHERE inventory_id = ?";
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(item.getDbId()));
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return e.getMessage() != null ? e.getMessage() : "Delete failed.";
        }
    }
    
    // ============================
    // Employees ("Employee" table, role/active stored in work_history JSONB)
    // ============================
    
    public List<Employee> getAllEmployees() {
        List<Employee> list = new ArrayList<>();
        String sql = """
                SELECT employee_id,
                       name,
                       work_history->>'role' AS role,
                       COALESCE((work_history->>'active')::boolean, TRUE) AS active
                  FROM "Employee"
                 ORDER BY name
                """;
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            int uiId = 1;
            while (rs.next()) {
                Employee emp = new Employee(
                        uiId++,
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getBoolean("active"));
                Object dbId = rs.getObject("employee_id");
                emp.setDbId(dbId != null ? dbId.toString() : null);
                list.add(emp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public void addEmployee(Employee employee) {
        String sql = """
                INSERT INTO "Employee" (employee_id, name, start_date, work_history)
                VALUES (?, ?, CURRENT_DATE, ?::jsonb)
                """;
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {
            UUID id = UUID.randomUUID();
            employee.setDbId(id.toString());
            ps.setObject(1, id);
            ps.setString(2, employee.getName());
            String json = String.format("{\"role\":\"%s\",\"active\":%s}",
                    employee.getRole() == null ? "" : employee.getRole(),
                    employee.isActive());
            ps.setString(3, json);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateEmployee(Employee employee) {
        if (employee.getDbId() == null) {
            return;
        }
        String sql = """
                UPDATE "Employee"
                   SET name = ?, work_history = ?::jsonb
                 WHERE employee_id = ?
                """;
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, employee.getName());
            String json = String.format("{\"role\":\"%s\",\"active\":%s}",
                    employee.getRole() == null ? "" : employee.getRole(),
                    employee.isActive());
            ps.setString(2, json);
            ps.setObject(3, UUID.fromString(employee.getDbId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return null on success, or an error message if delete failed (e.g. employee is referenced by orders).
     */
    public String deleteEmployee(Employee employee) {
        if (employee.getDbId() == null) {
            return "No database id.";
        }
        String sql = "DELETE FROM \"Employee\" WHERE employee_id = ?";
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(employee.getDbId()));
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("fk_order_employee")) {
                return "Cannot delete: this employee appears in existing orders.";
            }
            e.printStackTrace();
            return e.getMessage() != null ? e.getMessage() : "Delete failed.";
        }
    }
    
    // ============================
    // Reports (Order / Item aggregates)
    // ============================
    
    public BigDecimal getTotalSalesForDate(LocalDate date) {
        String sql = """
                SELECT COALESCE(SUM(oi.quantity * oi.unit_price), 0) AS total
                  FROM "Order" o
                  JOIN "Order_Item" oi ON oi.order_id = o.order_id
                 WHERE o.date::date = ?
                """;
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, date);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total");
                    return total != null ? total : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }
    
    public long getOrderCountForDate(LocalDate date) {
        String sql = """
                SELECT COUNT(*) AS cnt
                  FROM "Order"
                 WHERE date::date = ?
                """;
        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, date);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("cnt");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }
    
    // ============================
    // Cashier order processing -> "Order" + "Order_Item"
    // ============================
    
    public String processOrder(SalesOrder order) {
        if (order == null || order.getOrderItems().isEmpty()) {
            return "Error: Invalid order";
        }
        
        if (order.getTotalAmount() == null || order.getTotalAmount().doubleValue() <= 0) {
            return "Error: Order total must be greater than zero";
        }
        
        String insertOrderSql = """
                INSERT INTO "Order" (order_id, employee_id, customer_id, date, total_price)
                VALUES (?, ?, ?, NOW(), ?)
                """;
        String insertOrderItemSql = """
                INSERT INTO "Order_Item" (id, order_id, item_id, quantity, unit_price)
                VALUES (?, ?, ?, ?, ?)
                """;
        
        try (var conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            
            UUID orderId = UUID.randomUUID();
            UUID employeeId = ensureDemoEmployee(conn);
            UUID customerId = ensureDemoCustomer(conn);
            
            // Build JSONB of item quantities: { "item_uuid": qty, ... }
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (var item : order.getOrderItems()) {
                if (item.getItemDbId() == null) {
                    continue;
                }
                if (!first) {
                    json.append(",");
                }
                first = false;
                json.append("\"")
                        .append(item.getItemDbId())
                        .append("\":")
                        .append(item.getQuantity());
            }
            json.append("}");
            
            try (var ps = conn.prepareStatement(insertOrderSql)) {
                ps.setObject(1, orderId);
                ps.setObject(2, employeeId);
                ps.setObject(3, customerId);
                ps.setObject(4, order.calculateTotal());
                ps.executeUpdate();
            }
            
            try (var ps = conn.prepareStatement(insertOrderItemSql)) {
                for (var item : order.getOrderItems()) {
                    if (item.getItemDbId() == null) {
                        continue;
                    }
                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, orderId);
                    ps.setObject(3, UUID.fromString(item.getItemDbId()));
                    ps.setInt(4, item.getQuantity());
                    ps.setBigDecimal(5, item.getUnitPrice());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            
            conn.commit();
            salesOrders.add(order);
            return "Order processed successfully";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    
    private UUID ensureDemoEmployee(java.sql.Connection conn) throws SQLException {
        String select = """
                SELECT employee_id
                  FROM "Employee"
                 ORDER BY start_date
                 LIMIT 1
                """;
        try (var ps = conn.prepareStatement(select);
             var rs = ps.executeQuery()) {
            if (rs.next()) {
                return (UUID) rs.getObject("employee_id");
            }
        }
        UUID id = UUID.randomUUID();
        String json = "{\"role\":\"Cashier\",\"active\":true}";
        String insert = """
                INSERT INTO "Employee" (employee_id, name, start_date, work_history)
                VALUES (?, ?, CURRENT_DATE, ?::jsonb)
                """;
        try (var ps = conn.prepareStatement(insert)) {
            ps.setObject(1, id);
            ps.setString(2, "Demo Employee");
            ps.setString(3, json);
            ps.executeUpdate();
        }
        return id;
    }
    
    private UUID ensureDemoCustomer(java.sql.Connection conn) throws SQLException {
        String select = """
                SELECT customer_id
                  FROM "Customer"
                 ORDER BY customer_id
                 LIMIT 1
                """;
        try (var ps = conn.prepareStatement(select);
             var rs = ps.executeQuery()) {
            if (rs.next()) {
                return (UUID) rs.getObject("customer_id");
            }
        }
        UUID id = UUID.randomUUID();
        String insert = """
                INSERT INTO "Customer" (customer_id, name, phone_number, email, points, purchase_history)
                VALUES (?, ?, NULL, NULL, 0, '{}'::uuid[])
                """;
        try (var ps = conn.prepareStatement(insert)) {
            ps.setObject(1, id);
            ps.setString(2, "Walk-up Customer");
            ps.executeUpdate();
        }
        return id;
    }
}


