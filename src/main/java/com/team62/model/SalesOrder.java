package com.team62.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a sales order
 */
public class SalesOrder {
    private long orderId;
    private Timestamp orderDatetime;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private List<SalesOrderItem> orderItems;
    
    public SalesOrder() {
        this.orderItems = new ArrayList<>();
    }
    
    public SalesOrder(long orderId, Timestamp orderDatetime, BigDecimal totalAmount, String paymentMethod) {
        this.orderId = orderId;
        this.orderDatetime = orderDatetime;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.orderItems = new ArrayList<>();
    }
    
    // Getters and Setters
    public long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }
    
    public Timestamp getOrderDatetime() {
        return orderDatetime;
    }
    
    public void setOrderDatetime(Timestamp orderDatetime) {
        this.orderDatetime = orderDatetime;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public List<SalesOrderItem> getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(List<SalesOrderItem> orderItems) {
        this.orderItems = orderItems;
    }
    
    public void addOrderItem(SalesOrderItem item) {
        this.orderItems.add(item);
    }
}