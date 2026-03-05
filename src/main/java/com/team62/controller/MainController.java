package com.team62.controller;

import com.team62.db.Database;
import com.team62.model.Employee;
import com.team62.model.InventoryItem;
import com.team62.model.MenuItem;
import com.team62.model.SalesOrder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller class - handles business logic and coordinates between Model and View.
 * This implementation backs the UI with the shared Postgres database.
 */
public class MainController {

    private final List<SalesOrder> salesOrders = new ArrayList<>();

    public MainController() {
        bootstrapPosExtensions();
    }

    private void bootstrapPosExtensions() {
        String[] ddl = new String[] {
                """
                CREATE TABLE IF NOT EXISTS pos_inventory_meta (
                    inventory_id UUID PRIMARY KEY REFERENCES \"Inventory_Quantity\"(inventory_id) ON DELETE CASCADE,
                    display_name TEXT NOT NULL,
                    unit TEXT NOT NULL DEFAULT '',
                    min_quantity INTEGER NOT NULL DEFAULT 0 CHECK (min_quantity >= 0)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS pos_menu_inventory (
                    menu_item_id UUID NOT NULL REFERENCES \"Item\"(item_id) ON DELETE CASCADE,
                    inventory_id UUID NOT NULL REFERENCES \"Inventory_Quantity\"(inventory_id) ON DELETE CASCADE,
                    quantity_used INTEGER NOT NULL DEFAULT 1 CHECK (quantity_used > 0),
                    PRIMARY KEY (menu_item_id, inventory_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS pos_sales_activity (
                    activity_id UUID PRIMARY KEY,
                    business_date DATE NOT NULL,
                    event_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    activity_type TEXT NOT NULL,
                    order_id UUID,
                    amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                    tax_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                    payment_method TEXT NOT NULL DEFAULT 'Cash',
                    item_count INTEGER NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS pos_inventory_usage (
                    usage_id UUID PRIMARY KEY,
                    usage_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    business_date DATE NOT NULL,
                    order_id UUID,
                    menu_item_id UUID,
                    inventory_id UUID NOT NULL REFERENCES \"Inventory_Quantity\"(inventory_id) ON DELETE CASCADE,
                    quantity_used INTEGER NOT NULL CHECK (quantity_used > 0)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS pos_z_report (
                    report_date DATE PRIMARY KEY,
                    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    report_text TEXT NOT NULL
                )
                """
        };

        try (var conn = Database.getConnection()) {
            for (String sql : ddl) {
                try (var ps = conn.prepareStatement(sql)) {
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ============================
    // Menu items
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
            ps.setString(6, "whole");
            ps.setInt(7, 1);
            ps.setFloat(8, 1.0f);
            ps.setArray(9, conn.createArrayOf("text", new Object[] {}));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String addSeasonalMenuItem(String name, String category, BigDecimal price,
            String ingredientCsv, int quantityUsedPerSale, int startingInventory, int minInventory) {
        if (name == null || name.isBlank()) {
            return "Seasonal item name is required.";
        }
        try (var conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            UUID menuItemId = UUID.randomUUID();
            try (var ps = conn.prepareStatement("""
                    INSERT INTO "Item" (item_id, name, category, price, is_active, milk, ice, sugar, toppings)
                    VALUES (?, ?, ?, ?, TRUE, 'whole', 1, 1.0, '{}'::text[])
                    """)) {
                ps.setObject(1, menuItemId);
                ps.setString(2, name.trim());
                ps.setString(3, category == null || category.isBlank() ? "Seasonal" : category.trim());
                ps.setBigDecimal(4, price);
                ps.executeUpdate();
            }

            String[] parts = ingredientCsv == null ? new String[0] : ingredientCsv.split(",");
            for (String raw : parts) {
                String ingredientName = raw.trim();
                if (ingredientName.isEmpty()) {
                    continue;
                }
                UUID inventoryId = findOrCreateInventory(conn, ingredientName, "units", startingInventory, minInventory);
                try (var ps = conn.prepareStatement("""
                        INSERT INTO pos_menu_inventory (menu_item_id, inventory_id, quantity_used)
                        VALUES (?, ?, ?)
                        ON CONFLICT (menu_item_id, inventory_id)
                        DO UPDATE SET quantity_used = EXCLUDED.quantity_used
                        """)) {
                    ps.setObject(1, menuItemId);
                    ps.setObject(2, inventoryId);
                    ps.setInt(3, quantityUsedPerSale);
                    ps.executeUpdate();
                }
            }
            conn.commit();
            return "Seasonal item added successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to add seasonal item: " + e.getMessage();
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
    // Inventory
    // ============================

    public List<InventoryItem> getAllInventoryItems() {
        List<InventoryItem> items = new ArrayList<>();
        String sql = """
                SELECT iq.inventory_id,
                       COALESCE(meta.display_name, i.name, 'Inventory Item') AS name,
                       COALESCE(meta.unit, '') AS unit,
                       iq.quantity,
                       COALESCE(meta.min_quantity, 0) AS min_quantity
                  FROM "Inventory_Quantity" iq
             LEFT JOIN pos_inventory_meta meta ON meta.inventory_id = iq.inventory_id
             LEFT JOIN "Item_Inventory" ii ON ii.inventory_id = iq.inventory_id
             LEFT JOIN "Item" i ON i.item_id = ii.item_id
                 ORDER BY name
                """;
        try (var conn = Database.getConnection();
                var ps = conn.prepareStatement(sql);
                var rs = ps.executeQuery()) {
            int uiId = 1;
            while (rs.next()) {
                InventoryItem inv = new InventoryItem(
                        uiId++,
                        rs.getString("name"),
                        rs.getString("unit"),
                        rs.getInt("quantity"),
                        rs.getInt("min_quantity"),
                        rs.getInt("min_quantity"),
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
        try (var conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            UUID inventoryId = UUID.randomUUID();
            try (var ps = conn.prepareStatement("""
                    INSERT INTO "Inventory_Quantity" (inventory_id, quantity, last_restocked, last_quantity)
                    VALUES (?, ?, CURRENT_DATE, CURRENT_DATE)
                    """)) {
                ps.setObject(1, inventoryId);
                ps.setInt(2, item.getCurrentQuantity());
                ps.executeUpdate();
            }
            try (var ps = conn.prepareStatement("""
                    INSERT INTO pos_inventory_meta (inventory_id, display_name, unit, min_quantity)
                    VALUES (?, ?, ?, ?)
                    """)) {
                ps.setObject(1, inventoryId);
                ps.setString(2, item.getName());
                ps.setString(3, item.getUnit() == null ? "" : item.getUnit());
                ps.setInt(4, item.getParLevel());
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
        try (var conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try (var ps = conn.prepareStatement("""
                    UPDATE "Inventory_Quantity"
                       SET quantity = ?, last_quantity = CURRENT_DATE
                     WHERE inventory_id = ?
                    """)) {
                ps.setInt(1, item.getCurrentQuantity());
                ps.setObject(2, UUID.fromString(item.getDbId()));
                ps.executeUpdate();
            }
            try (var ps = conn.prepareStatement("""
                    INSERT INTO pos_inventory_meta (inventory_id, display_name, unit, min_quantity)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (inventory_id)
                    DO UPDATE SET display_name = EXCLUDED.display_name,
                                  unit = EXCLUDED.unit,
                                  min_quantity = EXCLUDED.min_quantity
                    """)) {
                ps.setObject(1, UUID.fromString(item.getDbId()));
                ps.setString(2, item.getName());
                ps.setString(3, item.getUnit() == null ? "" : item.getUnit());
                ps.setInt(4, item.getParLevel());
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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

    private UUID findOrCreateInventory(java.sql.Connection conn, String name, String unit, int quantity, int minQty)
            throws SQLException {
        try (var ps = conn.prepareStatement("""
                SELECT inventory_id
                  FROM pos_inventory_meta
                 WHERE LOWER(display_name) = LOWER(?)
                 LIMIT 1
                """)) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return (UUID) rs.getObject("inventory_id");
                }
            }
        }
        UUID inventoryId = UUID.randomUUID();
        try (var ps = conn.prepareStatement("""
                INSERT INTO "Inventory_Quantity" (inventory_id, quantity, last_restocked, last_quantity)
                VALUES (?, ?, CURRENT_DATE, CURRENT_DATE)
                """)) {
            ps.setObject(1, inventoryId);
            ps.setInt(2, quantity);
            ps.executeUpdate();
        }
        try (var ps = conn.prepareStatement("""
                INSERT INTO pos_inventory_meta (inventory_id, display_name, unit, min_quantity)
                VALUES (?, ?, ?, ?)
                """)) {
            ps.setObject(1, inventoryId);
            ps.setString(2, name);
            ps.setString(3, unit == null ? "" : unit);
            ps.setInt(4, minQty);
            ps.executeUpdate();
        }
        return inventoryId;
    }

    // ============================
    // Employees
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
    // Reports + order processing
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

    public String getInventoryUsageChart(LocalDate start, LocalDate end) {
        String sql = """
                SELECT COALESCE(meta.display_name, 'Inventory Item') AS item_name,
                       SUM(u.quantity_used) AS used_total
                  FROM pos_inventory_usage u
             LEFT JOIN pos_inventory_meta meta ON meta.inventory_id = u.inventory_id
                 WHERE u.business_date BETWEEN ? AND ?
              GROUP BY item_name
              ORDER BY used_total DESC, item_name
                """;
        StringBuilder sb = new StringBuilder();
        sb.append("PRODUCT USAGE CHART\n")
                .append(start).append(" to ").append(end).append("\n\n")
                .append(String.format("%-24s %8s  %s\n", "Inventory Item", "Used", "Chart"));
        try (var conn = Database.getConnection();
                var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, start);
            ps.setObject(2, end);
            try (var rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    int used = rs.getInt("used_total");
                    sb.append(String.format("%-24s %8d  %s\n",
                            rs.getString("item_name"),
                            used,
                            bar(used)));
                }
                if (!any) {
                    sb.append("No inventory usage logged in this time window.\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sb.append("Failed to build usage chart: ").append(e.getMessage());
        }
        return sb.toString();
    }

    public String getXReport(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("X REPORT\n")
                .append("Business date: ").append(date).append("\n\n");

        String hourlySql = """
                SELECT EXTRACT(HOUR FROM event_time) AS hr,
                       COUNT(*) AS sales_count,
                       COALESCE(SUM(amount), 0) AS sales_total,
                       COALESCE(SUM(tax_amount), 0) AS tax_total
                  FROM pos_sales_activity
                 WHERE business_date = ? AND activity_type = 'SALE'
              GROUP BY hr
              ORDER BY hr
                """;
        String totalsSql = """
                SELECT COUNT(*) AS sales_count,
                       COALESCE(SUM(amount), 0) AS sales_total,
                       COALESCE(SUM(tax_amount), 0) AS tax_total,
                       COALESCE(SUM(CASE WHEN LOWER(payment_method) = 'cash' THEN amount ELSE 0 END), 0) AS cash_total,
                       COALESCE(SUM(CASE WHEN LOWER(payment_method) <> 'cash' THEN amount ELSE 0 END), 0) AS non_cash_total,
                       COALESCE(SUM(item_count), 0) AS item_count
                  FROM pos_sales_activity
                 WHERE business_date = ? AND activity_type = 'SALE'
                """;

        sb.append(String.format("%-6s %-8s %-12s %-10s\n", "Hour", "Sales", "Revenue", "Tax"));
        try (var conn = Database.getConnection()) {
            try (var ps = conn.prepareStatement(hourlySql)) {
                ps.setObject(1, date);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        sb.append(String.format("%02d:00  %-8d $%-11s $%-9s\n",
                                rs.getInt("hr"),
                                rs.getInt("sales_count"),
                                money(rs.getBigDecimal("sales_total")),
                                money(rs.getBigDecimal("tax_total"))));
                    }
                }
            }
            try (var ps = conn.prepareStatement(totalsSql)) {
                ps.setObject(1, date);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        sb.append("\nTotals\n");
                        sb.append("Sales: ").append(rs.getInt("sales_count")).append("\n");
                        sb.append("Items sold: ").append(rs.getInt("item_count")).append("\n");
                        sb.append("Revenue: $").append(money(rs.getBigDecimal("sales_total"))).append("\n");
                        sb.append("Tax: $").append(money(rs.getBigDecimal("tax_total"))).append("\n");
                        sb.append("Returns: 0\nVoids: 0\nDiscards: 0\n");
                        sb.append("Cash payments: $").append(money(rs.getBigDecimal("cash_total"))).append("\n");
                        sb.append("Other payments: $").append(money(rs.getBigDecimal("non_cash_total"))).append("\n");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sb.append("Failed to build X-report: ").append(e.getMessage());
        }
        return sb.toString();
    }

    public String runZReport(LocalDate date) {
        String checkSql = "SELECT report_date FROM pos_z_report WHERE report_date = ?";
        try (var conn = Database.getConnection()) {
            try (var check = conn.prepareStatement(checkSql)) {
                check.setObject(1, date);
                try (var rs = check.executeQuery()) {
                    if (rs.next()) {
                        return "Z REPORT\nBusiness date: " + date + "\n\nThis Z-report has already been generated for today.\n";
                    }
                }
            }
            String reportText = buildZReportText(conn, date);
            conn.setAutoCommit(false);
            try (var insert = conn.prepareStatement("""
                    INSERT INTO pos_z_report (report_date, report_text)
                    VALUES (?, ?)
                    """)) {
                insert.setObject(1, date);
                insert.setString(2, reportText);
                insert.executeUpdate();
            }
            try (var delete = conn.prepareStatement("DELETE FROM pos_sales_activity WHERE business_date = ?")) {
                delete.setObject(1, date);
                delete.executeUpdate();
            }
            conn.commit();
            return reportText + "\nX/Z counters for this business date were reset to zero after close.\n";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Failed to run Z-report: " + e.getMessage();
        }
    }

    private String buildZReportText(java.sql.Connection conn, LocalDate date) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS sales_count,
                       COALESCE(SUM(amount), 0) AS sales_total,
                       COALESCE(SUM(tax_amount), 0) AS tax_total,
                       COALESCE(SUM(CASE WHEN LOWER(payment_method) = 'cash' THEN amount ELSE 0 END), 0) AS cash_total,
                       COALESCE(SUM(CASE WHEN LOWER(payment_method) <> 'cash' THEN amount ELSE 0 END), 0) AS non_cash_total,
                       COALESCE(SUM(item_count), 0) AS item_count
                  FROM pos_sales_activity
                 WHERE business_date = ? AND activity_type = 'SALE'
                """;
        StringBuilder sb = new StringBuilder();
        sb.append("Z REPORT\nBusiness date: ").append(date).append("\n\n");
        try (var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, date);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("Sales count: ").append(rs.getInt("sales_count")).append("\n");
                    sb.append("Items sold: ").append(rs.getInt("item_count")).append("\n");
                    sb.append("Gross sales: $").append(money(rs.getBigDecimal("sales_total"))).append("\n");
                    sb.append("Tax information: $").append(money(rs.getBigDecimal("tax_total"))).append("\n");
                    sb.append("Cash total: $").append(money(rs.getBigDecimal("cash_total"))).append("\n");
                    sb.append("Other payment methods: $").append(money(rs.getBigDecimal("non_cash_total"))).append("\n");
                    sb.append("Discounts: $0.00\nVoids: 0\nService charges: $0.00\n");
                    sb.append("Employee signatures: __________________________\n");
                }
            }
        }
        return sb.toString();
    }

    public String getSalesReport(LocalDate start, LocalDate end) {
        String sql = """
                SELECT i.name,
                       SUM(oi.quantity) AS qty,
                       SUM(oi.quantity * oi.unit_price) AS revenue
                  FROM "Order" o
                  JOIN "Order_Item" oi ON oi.order_id = o.order_id
                  JOIN "Item" i ON i.item_id = oi.item_id
                 WHERE o.date::date BETWEEN ? AND ?
              GROUP BY i.name
              ORDER BY revenue DESC, qty DESC, i.name
                """;
        StringBuilder sb = new StringBuilder();
        sb.append("SALES REPORT\n")
                .append(start).append(" to ").append(end).append("\n\n")
                .append(String.format("%-24s %8s %12s\n", "Item", "Qty", "Revenue"));
        try (var conn = Database.getConnection();
                var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, start);
            ps.setObject(2, end);
            try (var rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    sb.append(String.format("%-24s %8d $%11s\n",
                            rs.getString("name"),
                            rs.getInt("qty"),
                            money(rs.getBigDecimal("revenue"))));
                }
                if (!any) {
                    sb.append("No sales found for this time window.\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sb.append("Failed to build sales report: ").append(e.getMessage());
        }
        return sb.toString();
    }

    public String getRestockReport() {
        String sql = """
                SELECT COALESCE(meta.display_name, 'Inventory Item') AS item_name,
                       iq.quantity,
                       COALESCE(meta.min_quantity, 0) AS min_qty,
                       COALESCE(meta.unit, '') AS unit
                  FROM "Inventory_Quantity" iq
                  JOIN pos_inventory_meta meta ON meta.inventory_id = iq.inventory_id
                 WHERE iq.quantity < COALESCE(meta.min_quantity, 0)
              ORDER BY (COALESCE(meta.min_quantity, 0) - iq.quantity) DESC, item_name
                """;
        StringBuilder sb = new StringBuilder();
        sb.append("RESTOCK REPORT\n\n")
                .append(String.format("%-24s %10s %10s %10s\n", "Item", "Current", "Minimum", "Unit"));
        try (var conn = Database.getConnection();
                var ps = conn.prepareStatement(sql);
                var rs = ps.executeQuery()) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                sb.append(String.format("%-24s %10d %10d %10s\n",
                        rs.getString("item_name"),
                        rs.getInt("quantity"),
                        rs.getInt("min_qty"),
                        rs.getString("unit")));
            }
            if (!any) {
                sb.append("Nothing is currently below its minimum stock level.\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sb.append("Failed to build restock report: ").append(e.getMessage());
        }
        return sb.toString();
    }

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
            BigDecimal subtotal = BigDecimal.ZERO;
            int itemCount = 0;
            for (var item : order.getOrderItems()) {
                subtotal = subtotal.add(item.getLineTotal());
                itemCount += item.getQuantity();
            }
            BigDecimal tax = order.getTotalAmount().subtract(subtotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            try (var ps = conn.prepareStatement(insertOrderSql)) {
                ps.setObject(1, orderId);
                ps.setObject(2, employeeId);
                ps.setObject(3, customerId);
                ps.setBigDecimal(4, order.getTotalAmount());
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

            for (var item : order.getOrderItems()) {
                if (item.getItemDbId() == null) {
                    continue;
                }
                applyInventoryUsage(conn, orderId, UUID.fromString(item.getItemDbId()), item.getQuantity());
            }

            try (var ps = conn.prepareStatement("""
                    INSERT INTO pos_sales_activity
                    (activity_id, business_date, event_time, activity_type, order_id, amount, tax_amount, payment_method, item_count)
                    VALUES (?, CURRENT_DATE, NOW(), 'SALE', ?, ?, ?, ?, ?)
                    """)) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, orderId);
                ps.setBigDecimal(3, order.getTotalAmount());
                ps.setBigDecimal(4, tax);
                ps.setString(5, order.getPaymentMethod() == null ? "Cash" : order.getPaymentMethod());
                ps.setInt(6, itemCount);
                ps.executeUpdate();
            }

            conn.commit();
            salesOrders.add(order);
            return "Order processed successfully";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private void applyInventoryUsage(java.sql.Connection conn, UUID orderId, UUID menuItemId, int quantitySold)
            throws SQLException {
        try (var ps = conn.prepareStatement("""
                SELECT inventory_id, quantity_used
                  FROM pos_menu_inventory
                 WHERE menu_item_id = ?
                """)) {
            ps.setObject(1, menuItemId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID inventoryId = (UUID) rs.getObject("inventory_id");
                    int used = rs.getInt("quantity_used") * quantitySold;
                    try (var update = conn.prepareStatement("""
                            UPDATE "Inventory_Quantity"
                               SET quantity = GREATEST(quantity - ?, 0)
                             WHERE inventory_id = ?
                            """)) {
                        update.setInt(1, used);
                        update.setObject(2, inventoryId);
                        update.executeUpdate();
                    }
                    try (var insert = conn.prepareStatement("""
                            INSERT INTO pos_inventory_usage
                            (usage_id, usage_time, business_date, order_id, menu_item_id, inventory_id, quantity_used)
                            VALUES (?, NOW(), CURRENT_DATE, ?, ?, ?, ?)
                            """)) {
                        insert.setObject(1, UUID.randomUUID());
                        insert.setObject(2, orderId);
                        insert.setObject(3, menuItemId);
                        insert.setObject(4, inventoryId);
                        insert.setInt(5, used);
                        insert.executeUpdate();
                    }
                }
            }
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
        try (var ps = conn.prepareStatement("""
                INSERT INTO "Employee" (employee_id, name, start_date, work_history)
                VALUES (?, ?, CURRENT_DATE, ?::jsonb)
                """)) {
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
        try (var ps = conn.prepareStatement("""
                INSERT INTO "Customer" (customer_id, name, phone_number, email, points, purchase_history)
                VALUES (?, ?, NULL, NULL, 0, '{}'::uuid[])
                """)) {
            ps.setObject(1, id);
            ps.setString(2, "Walk-up Customer");
            ps.executeUpdate();
        }
        return id;
    }

    private String bar(int value) {
        int len = Math.max(1, Math.min(40, value));
        return "#".repeat(len);
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toString();
    }
}
