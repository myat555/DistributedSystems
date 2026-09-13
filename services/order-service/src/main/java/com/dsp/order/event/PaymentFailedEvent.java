package com.dsp.order.event;

/** Consumed from {@code payment.failed}: charge did not succeed after inventory was reserved. */
public record PaymentFailedEvent(String orderId, String reason) {
}
