package com.team62.model;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * Model representing an inventory purchase
 */
public class InventoryPurchase {
    private long purchaseId;
    private int inventoryItemId;
    private Date purchaseDate;
    private int quantity;
    private BigDecimal unitCost;
    private BigDecimal lineCost;
    
    public InventoryPurchase() {
    }
    
    public InventoryPurchase(long purchaseId, int inventoryItemId, Date purchaseDate, int quantity, BigDecimal unitCost, BigDecimal lineCost) {
        this.purchaseId = purchaseId;
        this.inventoryItemId = inventoryItemId;
        this.purchaseDate = purchaseDate;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.lineCost = lineCost;
    }
    
    // Getters and Setters
    public long getPurchaseId() {
        return purchaseId;
    }
    
    public void setPurchaseId(long purchaseId) {
        this.purchaseId = purchaseId;
    }
    
    public int getInventoryItemId() {
        return inventoryItemId;
    }
    
    public void setInventoryItemId(int inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }
    
    public Date getPurchaseDate() {
        return purchaseDate;
    }
    
    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getUnitCost() {
        return unitCost;
    }
    
    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }
    
    public BigDecimal getLineCost() {
        return lineCost;
    }
    
    public void setLineCost(BigDecimal lineCost) {
        this.lineCost = lineCost;
    }
}
