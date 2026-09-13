package com.dsp.inventory.event;

/** Consumed from {@code inventory.compensate}: release a previously reserved quantity back to stock. */
public record InventoryCompensateEvent(String orderId, String itemId, int quantity) {
}
