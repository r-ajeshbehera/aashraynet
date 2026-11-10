package com.rajesh.aashraynet.dto.request;

import com.rajesh.aashraynet.entity.MovementType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** quantity is always positive; the type decides whether stock goes up or down. RECEIPT is not allowed here. */
public record StockAdjustmentRequest(
        @NotNull Long itemId,
        @NotNull MovementType type,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal quantity,
        Long eventId,
        @Size(max = 255) String reason) { }
