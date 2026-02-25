package com.team62.model;

import java.math.BigDecimal;

/**
 * Model representing a menu item.
 *
 * {@code menuItemId} is a simple UI-friendly integer id used only inside the JavaFX
 * application. {@code dbId} holds the real UUID primary key from the database.
 */
public class MenuItem {
    private int menuItemId;
    private String dbId; // backing UUID from "Item".item_id (as String)
    private String name;
    private String category;
    private BigDecimal basePrice;
    private boolean isActive;
    
    public MenuItem() {
    }
    
    public MenuItem(int menuItemId, String name, String category, BigDecimal basePrice, boolean isActive) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
        this.isActive = isActive;
    }
    
    // Getters and Setters
    public int getMenuItemId() {
        return menuItemId;
    }
    
    public void setMenuItemId(int menuItemId) {
        this.menuItemId = menuItemId;
    }
    
    public String getDbId() {
        return dbId;
    }
    
    public void setDbId(String dbId) {
        this.dbId = dbId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public BigDecimal getBasePrice() {
        return basePrice;
    }
    
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    @Override
    public String toString() {
        return name + " - $" + basePrice;
    }
}
