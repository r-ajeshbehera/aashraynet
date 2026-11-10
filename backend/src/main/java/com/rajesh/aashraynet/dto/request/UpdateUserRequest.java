package com.rajesh.aashraynet.dto.request;

import com.rajesh.aashraynet.entity.Role;
import jakarta.validation.constraints.Size;

/** All fields optional: only non-null values are changed. */
public record UpdateUserRequest(@Size(max = 120) String fullName, Boolean active, Role role) { }
