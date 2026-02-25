package com.team62.model;

/**
 * Model representing an inventory item
 */
public class InventoryItem {
    private int inventoryItemId;
    private String name;
    private String unit;
    private int currentQuantity;
    private int parLevel;
    private int reorderPoint;
    private boolean isActive;
    
    public InventoryItem() {
    }
    
    public InventoryItem(int inventoryItemId, String name, String unit, int currentQuantity, int parLevel, int reorderPoint, boolean isActive) {
        this.inventoryItemId = inventoryItemId;
        this.name = name;
        this.unit = unit;
        this.currentQuantity = currentQuantity;
        this.parLevel = parLevel;
        this.reorderPoint = reorderPoint;
        this.isActive = isActive;
    }
    
    // Getters and Setters
    public int getInventoryItemId() {
        return inventoryItemId;
    }
    
    public void setInventoryItemId(int inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public int getCurrentQuantity() {
        return currentQuantity;
    }
    
    public void setCurrentQuantity(int currentQuantity) {
        this.currentQuantity = currentQuantity;
    }
    
    public int getParLevel() {
        return parLevel;
    }
    
    public void setParLevel(int parLevel) {
        this.parLevel = parLevel;
    }
    
    public int getReorderPoint() {
        return reorderPoint;
    }
    
    public void setReorderPoint(int reorderPoint) {
        this.reorderPoint = reorderPoint;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    @Override
    public String toString() {
        return name + " (" + unit + ")";
    }
}
