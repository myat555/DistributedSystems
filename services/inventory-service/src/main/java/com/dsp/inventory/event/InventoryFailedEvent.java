package com.dsp.inventory.event;

/** Published to {@code inventory.failed} when a reservation cannot be made. */
public record InventoryFailedEvent(String orderId, String reason) {
}
