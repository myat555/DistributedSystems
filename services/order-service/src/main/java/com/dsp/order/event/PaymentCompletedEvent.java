package com.dsp.order.event;

/** Consumed from {@code payment.completed}: the saga finished successfully. */
public record PaymentCompletedEvent(String orderId) {
}
