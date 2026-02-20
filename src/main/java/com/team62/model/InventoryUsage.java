package com.team62.model;

import java.sql.Date;

/**
 * Model representing inventory usage
 */
public class InventoryUsage {
    private long usageId;
    private int inventoryItemId;
    private Date usageDate;
    private int quantity;
    
    public InventoryUsage() {
    }
    
    public InventoryUsage(long usageId, int inventoryItemId, Date usageDate, int quantity) {
        this.usageId = usageId;
        this.inventoryItemId = inventoryItemId;
        this.usageDate = usageDate;
        this.quantity = quantity;
    }
    
    // Getters and Setters
    public long getUsageId() {
        return usageId;
    }
    
    public void setUsageId(long usageId) {
        this.usageId = usageId;
    }
    
    public int getInventoryItemId() {
        return inventoryItemId;
    }
    
    public void setInventoryItemId(int inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }
    
    public Date getUsageDate() {
        return usageDate;
    }
    
    public void setUsageDate(Date usageDate) {
        this.usageDate = usageDate;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
