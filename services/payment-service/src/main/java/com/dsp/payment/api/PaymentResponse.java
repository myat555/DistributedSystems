package com.dsp.payment.api;

import com.dsp.payment.domain.PaymentRecord;
import com.dsp.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(String id, String orderId, BigDecimal amount, PaymentStatus status, Instant createdAt) {
    public static PaymentResponse from(PaymentRecord record) {
        return new PaymentResponse(record.getId(), record.getOrderId(), record.getAmount(),
                record.getStatus(), record.getCreatedAt());
    }
}
