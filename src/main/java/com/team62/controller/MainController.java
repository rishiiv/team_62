package com.team62.controller;

import com.team62.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller class - handles business logic and coordinates between Model and View
 * This is a base controller that can be extended for specific functionality
 */
public class MainController {
    // Model data (in a real app, this would come from a database/service layer)
    private List<MenuItem> menuItems;
    private List<InventoryItem> inventoryItems;
    private List<SalesOrder> salesOrders;
    
    public MainController() {
        initializeData();
    }
    
    private void initializeData() {
        // Initialize empty lists
        // In a real application, these would be loaded from a database
        menuItems = new ArrayList<>();
        inventoryItems = new ArrayList<>();
        salesOrders = new ArrayList<>();
    }
    
    // Business logic methods - these handle operations on models
    
    public List<MenuItem> getAllMenuItems() {
        return new ArrayList<>(menuItems);
    }
    
    public void addMenuItem(MenuItem item) {
        menuItems.add(item);
    }
    
    public List<InventoryItem> getAllInventoryItems() {
        return new ArrayList<>(inventoryItems);
    }
    
    public void addInventoryItem(InventoryItem item) {
        inventoryItems.add(item);
    }
    
    public List<SalesOrder> getAllSalesOrders() {
        return new ArrayList<>(salesOrders);
    }
    
    public void addSalesOrder(SalesOrder order) {
        salesOrders.add(order);
    }
    
    // Example business logic method
    public String processOrder(SalesOrder order) {
        // Business logic for processing an order
        if (order == null || order.getOrderItems().isEmpty()) {
            return "Error: Invalid order";
        }
        
        if (order.getTotalAmount().doubleValue() <= 0) {
            return "Error: Order total must be greater than zero";
        }
        
        // Add order to list
        addSalesOrder(order);
        return "Order processed successfully";
    }
}
