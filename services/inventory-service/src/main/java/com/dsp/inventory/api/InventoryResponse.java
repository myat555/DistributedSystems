package com.dsp.inventory.api;

import com.dsp.inventory.domain.InventoryItem;

public record InventoryResponse(String itemId, int availableQuantity, int reservedQuantity) {
    public static InventoryResponse from(InventoryItem item) {
        return new InventoryResponse(item.getItemId(), item.getAvailableQuantity(), item.getReservedQuantity());
    }
}
