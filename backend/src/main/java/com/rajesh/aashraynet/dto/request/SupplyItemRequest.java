package com.rajesh.aashraynet.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SupplyItemRequest(@NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 20) String unit,
                                @NotNull @DecimalMin("0") @Digits(integer = 5, fraction = 3) BigDecimal perPersonPerDay) { }
