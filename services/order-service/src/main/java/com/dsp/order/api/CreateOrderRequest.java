package com.dsp.order.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotBlank String itemId,
        @NotNull @Positive Integer quantity,
        @NotNull @Positive BigDecimal amount
) {
}
