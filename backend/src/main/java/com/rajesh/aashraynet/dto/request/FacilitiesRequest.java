package com.rajesh.aashraynet.dto.request;

import jakarta.validation.constraints.NotNull;

public record FacilitiesRequest(@NotNull Boolean hasMedical, @NotNull Boolean hasRamp, @NotNull Boolean hasGenerator) { }
