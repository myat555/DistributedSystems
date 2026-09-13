package com.dsp.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Stock level for one SKU: units available to reserve, and units currently reserved by in-flight orders. */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    private String itemId;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    protected InventoryItem() {
        // for JPA
    }

    public InventoryItem(String itemId, int availableQuantity, int reservedQuantity) {
        this.itemId = itemId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    public String getItemId() {
        return itemId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
}
