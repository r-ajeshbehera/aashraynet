package com.rajesh.aashraynet.dto.request;

import com.rajesh.aashraynet.entity.Role;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        Long districtId) { }
