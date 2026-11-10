package com.rajesh.aashraynet.dto.response;

import com.rajesh.aashraynet.entity.Role;
import com.rajesh.aashraynet.entity.User;

public record UserResponse(Long id, String fullName, String email, Role role, Long districtId, String districtName,
                           boolean active) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getDistrictId(),
                u.getDistrict() == null ? null : u.getDistrict().getName(), u.isActive());
    }
}
