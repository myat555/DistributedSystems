package com.dsp.order.event;

import java.math.BigDecimal;

/** Published to {@code order.created} once a new order is persisted as PENDING. */
public record OrderCreatedEvent(String orderId, String itemId, int quantity, BigDecimal amount) {
}
