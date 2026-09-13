package com.dsp.payment.event;

/** Published to {@code payment.failed} when the charge does not succeed. */
public record PaymentFailedEvent(String orderId, String reason) {
}
