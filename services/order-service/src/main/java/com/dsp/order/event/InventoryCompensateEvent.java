package com.dsp.order.event;

/**
 * Published to {@code inventory.compensate} when payment fails after inventory was already
 * reserved for this order - the compensating transaction that undoes step 2 of the saga.
 */
public record InventoryCompensateEvent(String orderId, String itemId, int quantity) {
}
