package com.rajesh.aashraynet.dto.request;

import com.rajesh.aashraynet.entity.Gender;
import com.rajesh.aashraynet.entity.SpecialNeed;
import jakarta.validation.constraints.*;

public record CheckInRequest(
        @NotNull Long eventId,
        @NotBlank @Size(max = 120) String fullName,
        @NotNull @Min(0) @Max(120) Integer age,
        @NotNull Gender gender,
        @NotBlank @Size(max = 120) String village,
        SpecialNeed specialNeed) { }
