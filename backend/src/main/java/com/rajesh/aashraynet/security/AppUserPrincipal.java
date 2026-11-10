package com.rajesh.aashraynet.security;

import com.rajesh.aashraynet.entity.Role;
import com.rajesh.aashraynet.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** The logged-in user as Spring Security sees it. Carries the district so every service can scope queries. */
public record AppUserPrincipal(Long id, String email, String passwordHash, Role role, Long districtId,
                               boolean active) implements UserDetails {

    public static AppUserPrincipal from(User u) {
        return new AppUserPrincipal(u.getId(), u.getEmail(), u.getPasswordHash(), u.getRole(), u.getDistrictId(), u.isActive());
    }

    public boolean isStateAdmin() { return role == Role.STATE_ADMIN; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return active; }
}
