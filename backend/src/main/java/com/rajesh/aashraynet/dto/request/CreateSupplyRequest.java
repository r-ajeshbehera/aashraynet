package com.rajesh.aashraynet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateSupplyRequest(
        @NotNull Long eventId,
        @Size(max = 255) String note,
        @NotEmpty @Size(max = 50) List<@Valid Line> lines) {

    public record Line(@NotNull Long itemId,
                       @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal quantity) { }
}
