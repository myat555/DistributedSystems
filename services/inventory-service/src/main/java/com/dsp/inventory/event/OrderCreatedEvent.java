package com.dsp.inventory.event;

import java.math.BigDecimal;

/** Consumed from {@code order.created}: try to reserve stock for this order. */
public record OrderCreatedEvent(String orderId, String itemId, int quantity, BigDecimal amount) {
}
