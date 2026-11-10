package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.request.ChangePasswordRequest;
import com.rajesh.aashraynet.dto.request.CreateUserRequest;
import com.rajesh.aashraynet.dto.request.UpdateUserRequest;
import com.rajesh.aashraynet.dto.response.UserResponse;
import com.rajesh.aashraynet.entity.District;
import com.rajesh.aashraynet.entity.Role;
import com.rajesh.aashraynet.entity.User;
import com.rajesh.aashraynet.exception.BusinessRuleException;
import com.rajesh.aashraynet.exception.ResourceNotFoundException;
import com.rajesh.aashraynet.repository.DistrictRepository;
import com.rajesh.aashraynet.repository.UserRepository;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

/**
 * Accounts are only ever created by administrators (no public sign-up):
 * STATE_ADMIN can create any role; DISTRICT_ADMIN can create VOLUNTEER / SHELTER_INCHARGE in their own district.
 */
@Service
public class UserService {
    private static final Set<Role> DISTRICT_ADMIN_CAN_MANAGE = EnumSet.of(Role.VOLUNTEER, Role.SHELTER_INCHARGE);

    private final UserRepository users;
    private final DistrictRepository districts;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserService(UserRepository users, DistrictRepository districts, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.districts = districts;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional
    public UserResponse create(CreateUserRequest req, AppUserPrincipal actor) {
        Long districtId = req.districtId();
        if (actor.role() == Role.DISTRICT_ADMIN) {
            if (!DISTRICT_ADMIN_CAN_MANAGE.contains(req.role())) {
                throw new AccessDeniedException("District admins can create volunteers and shelter in-charges only");
            }
            if (districtId == null) districtId = actor.districtId();
            if (!districtId.equals(actor.districtId())) throw new ResourceNotFoundException("District", districtId);
        }
        if (req.role() == Role.STATE_ADMIN && districtId != null) {
            throw new BusinessRuleException("INVALID_DISTRICT", "A state admin is not linked to a district");
        }
        if (req.role() != Role.STATE_ADMIN && districtId == null) {
            throw new BusinessRuleException("DISTRICT_REQUIRED", "districtId is required for this role");
        }
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("EMAIL_TAKEN", "An account with this email already exists");
        }
        District district = districtId == null ? null : districts.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", req.districtId()));
        User saved = users.save(new User(req.fullName().trim(), email, encoder.encode(req.password()), req.role(), district));
        audit.log(actor.id(), "USER_CREATED", "USER", saved.getId(), "role=" + saved.getRole());
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Role role, Long districtId, Pageable pageable, AppUserPrincipal actor) {
        Long scope = actor.isStateAdmin() ? districtId : actor.districtId();
        return users.search(scope, role, pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req, AppUserPrincipal actor) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (actor.role() == Role.DISTRICT_ADMIN) {
            boolean sameDistrict = actor.districtId().equals(user.getDistrictId());
            if (!sameDistrict) throw new ResourceNotFoundException("User", id);
            if (!DISTRICT_ADMIN_CAN_MANAGE.contains(user.getRole())
                    || (req.role() != null && !DISTRICT_ADMIN_CAN_MANAGE.contains(req.role()))) {
                throw new AccessDeniedException("District admins manage volunteers and shelter in-charges only");
            }
        }
        if (req.role() != null && (req.role() == Role.STATE_ADMIN) != (user.getRole() == Role.STATE_ADMIN)) {
            throw new BusinessRuleException("ROLE_CHANGE_NOT_ALLOWED", "Cannot convert between state-level and district-level roles");
        }
        if (Boolean.FALSE.equals(req.active()) && user.getId().equals(actor.id())) {
            throw new BusinessRuleException("SELF_DEACTIVATION", "You cannot deactivate your own account");
        }
        if (req.fullName() != null) user.setFullName(req.fullName().trim());
        if (req.active() != null) user.setActive(req.active());
        if (req.role() != null) user.setRole(req.role());
        audit.log(actor.id(), "USER_UPDATED", "USER", id, null);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(AppUserPrincipal actor) {
        return UserResponse.from(users.findById(actor.id()).orElseThrow(() -> new ResourceNotFoundException("User", actor.id())));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req, AppUserPrincipal actor) {
        User user = users.findById(actor.id()).orElseThrow(() -> new ResourceNotFoundException("User", actor.id()));
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("WRONG_PASSWORD", "Current password is incorrect");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        audit.log(actor.id(), "PASSWORD_CHANGED", "USER", actor.id(), null);
    }
}
