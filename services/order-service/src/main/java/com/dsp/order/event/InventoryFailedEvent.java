package com.dsp.order.event;

/** Consumed from {@code inventory.failed}: reservation could not be made. */
public record InventoryFailedEvent(String orderId, String reason) {
}
