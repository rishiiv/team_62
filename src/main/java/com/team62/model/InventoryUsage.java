package com.team62.model;

/**
 * Simple DTO for inventory usage reporting.
 */
public class InventoryUsage {
    private final String itemName;
    private final int amountUsed;

    public InventoryUsage(String itemName, int amountUsed) {
        this.itemName = itemName;
        this.amountUsed = amountUsed;
    }

    public String getItemName() {
        return itemName;
    }

    public int getAmountUsed() {
        return amountUsed;
    }
}
