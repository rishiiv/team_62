package com.team62.controller;

import com.team62.model.Employee;
import com.team62.model.InventoryItem;
import com.team62.model.MenuItem;
import com.team62.model.SalesOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Controller class - handles business logic and coordinates between Model and View.
 * For this demo implementation, all data is kept in-memory.
 */
public class MainController {
    // Model data (in a real app, this would come from a database/service layer)
    private List<MenuItem> menuItems;
    private List<InventoryItem> inventoryItems;
    private List<SalesOrder> salesOrders;
    private List<Employee> employees;
    
    public MainController() {
        initializeData();
    }
    
    private void initializeData() {
        // Initialize lists
        menuItems = new ArrayList<>();
        inventoryItems = new ArrayList<>();
        salesOrders = new ArrayList<>();
        employees = new ArrayList<>();
        
        // Seed starter demo data so the UI is not empty
        menuItems.add(new MenuItem(1, "Classic Milk Tea", "Milk Tea", new BigDecimal("5.50"), true));
        menuItems.add(new MenuItem(2, "Taro Milk Tea", "Milk Tea", new BigDecimal("5.75"), true));
        menuItems.add(new MenuItem(3, "Matcha Latte", "Specialty", new BigDecimal("6.00"), true));
        menuItems.add(new MenuItem(4, "Brown Sugar Boba", "Signature", new BigDecimal("6.25"), true));
        
        inventoryItems.add(new InventoryItem(1, "Black Tea Base", "gallon", 12, 5, 2, true));
        inventoryItems.add(new InventoryItem(2, "Tapioca Pearls", "bag", 6, 8, 3, true));
        inventoryItems.add(new InventoryItem(3, "Milk", "gallon", 7, 10, 4, true));
        
        employees.add(new Employee(1, "Alex Chen", "Cashier", true));
        employees.add(new Employee(2, "Jordan Smith", "Shift Lead", true));
        employees.add(new Employee(3, "Morgan Lee", "Manager", true));
    }
    
    // Business logic methods - these handle operations on models
    
    public List<MenuItem> getAllMenuItems() {
        return new ArrayList<>(menuItems);
    }
    
    public void addMenuItem(MenuItem item) {
        menuItems.add(item);
    }
    
    public void updateMenuItem(MenuItem item) {
        for (int i = 0; i < menuItems.size(); i++) {
            if (menuItems.get(i).getMenuItemId() == item.getMenuItemId()) {
                menuItems.set(i, item);
                return;
            }
        }
    }
    
    public List<InventoryItem> getAllInventoryItems() {
        return new ArrayList<>(inventoryItems);
    }
    
    public void addInventoryItem(InventoryItem item) {
        inventoryItems.add(item);
    }
    
    public void updateInventoryItem(InventoryItem item) {
        for (int i = 0; i < inventoryItems.size(); i++) {
            if (inventoryItems.get(i).getInventoryItemId() == item.getInventoryItemId()) {
                inventoryItems.set(i, item);
                return;
            }
        }
    }
    
    public List<SalesOrder> getAllSalesOrders() {
        return new ArrayList<>(salesOrders);
    }
    
    public void addSalesOrder(SalesOrder order) {
        salesOrders.add(order);
    }
    
    public List<Employee> getAllEmployees() {
        return new ArrayList<>(employees);
    }
    
    public void addEmployee(Employee employee) {
        employees.add(employee);
    }
    
    public void updateEmployee(Employee employee) {
        for (int i = 0; i < employees.size(); i++) {
            if (employees.get(i).getEmployeeId() == employee.getEmployeeId()) {
                employees.set(i, employee);
                return;
            }
        }
    }
    
    /**
     * Simple helpers for day-to-day reporting needs.
     */
    public BigDecimal getTotalSalesForDate(LocalDate date) {
        return salesOrders.stream()
                .filter(order -> order.getOrderDatetime() != null
                        && order.getOrderDatetime().toLocalDateTime().toLocalDate().equals(date))
                .map(SalesOrder::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public long getOrderCountForDate(LocalDate date) {
        return salesOrders.stream()
                .filter(order -> order.getOrderDatetime() != null
                        && order.getOrderDatetime().toLocalDateTime().toLocalDate().equals(date))
                .count();
    }
    
    // Example business logic method
    public String processOrder(SalesOrder order) {
        if (order == null || order.getOrderItems().isEmpty()) {
            return "Error: Invalid order";
        }
        
        if (order.getTotalAmount() == null || order.getTotalAmount().doubleValue() <= 0) {
            return "Error: Order total must be greater than zero";
        }
        
        addSalesOrder(order);
        return "Order processed successfully";
    }
}

