package com.dsp.order.api;

import com.dsp.order.domain.Order;
import com.dsp.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String id,
        String customerId,
        String itemId,
        int quantity,
        BigDecimal amount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getItemId(), order.getQuantity(),
                order.getAmount(), order.getStatus(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
