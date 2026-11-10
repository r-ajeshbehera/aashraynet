package com.rajesh.aashraynet.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ShelterRequest(
        @NotNull Long districtId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String village,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @NotNull @Min(1) @Max(100000) Integer capacity,
        boolean hasMedical, boolean hasRamp, boolean hasGenerator,
        Boolean active) { }
