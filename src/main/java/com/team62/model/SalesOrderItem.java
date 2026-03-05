package com.team62.model;

import java.math.BigDecimal;

/**
 * Model representing a sales order item.
 *
 * {@code menuItemId} is the UI id; {@code itemDbId} stores the real UUID
 * primary key from the {@code "Item"} table as a String.
 */
public class SalesOrderItem {
    private long orderItemId;
    private long orderId;
    private int menuItemId;
    private String itemDbId;
    private int quantity;
    private BigDecimal unitPrice;
    
    public SalesOrderItem() {
    }
    
    public SalesOrderItem(long orderItemId, long orderId, int menuItemId, int quantity, BigDecimal unitPrice) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
    
    // Getters and Setters
    public long getOrderItemId() {
        return orderItemId;
    }
    
    public void setOrderItemId(long orderItemId) {
        this.orderItemId = orderItemId;
    }
    
    public long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }
    
    public int getMenuItemId() {
        return menuItemId;
    }
    //comment 
    public void setMenuItemId(int menuItemId) {
        this.menuItemId = menuItemId;
    }
    
    public String getItemDbId() {
        return itemDbId;
    }
    
    public void setItemDbId(String itemDbId) {
        this.itemDbId = itemDbId;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
