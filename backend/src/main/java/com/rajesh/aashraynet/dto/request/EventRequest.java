package com.rajesh.aashraynet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EventRequest(@NotBlank @Size(max = 120) String name, @NotNull LocalDate startDate, LocalDate endDate,
                           Boolean active) { }
