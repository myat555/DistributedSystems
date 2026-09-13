package com.dsp.inventory.event;

import java.math.BigDecimal;

/**
 * Published to {@code inventory.reserved} once stock is reserved. {@code amount} is forwarded
 * unchanged from the order.created event - inventory-service doesn't know pricing, it's just
 * relaying the field payment-service needs, keeping the saga fully event-driven.
 */
public record InventoryReservedEvent(String orderId, String itemId, int quantity, BigDecimal amount) {
}
