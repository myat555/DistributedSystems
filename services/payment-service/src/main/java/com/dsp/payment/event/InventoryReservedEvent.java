package com.dsp.payment.event;

import java.math.BigDecimal;

/** Consumed from {@code inventory.reserved}: attempt to charge {@code amount} for this order. */
public record InventoryReservedEvent(String orderId, String itemId, int quantity, BigDecimal amount) {
}
