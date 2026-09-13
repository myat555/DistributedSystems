package com.dsp.payment.event;

/** Published to {@code payment.completed} when the charge succeeds. */
public record PaymentCompletedEvent(String orderId) {
}
