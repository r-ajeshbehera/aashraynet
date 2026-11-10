package com.rajesh.aashraynet.service;

import com.rajesh.aashraynet.dto.request.LoginRequest;
import com.rajesh.aashraynet.dto.response.LoginResponse;
import com.rajesh.aashraynet.dto.response.UserResponse;
import com.rajesh.aashraynet.entity.RevokedToken;
import com.rajesh.aashraynet.repository.RevokedTokenRepository;
import com.rajesh.aashraynet.repository.UserRepository;
import com.rajesh.aashraynet.security.AppUserPrincipal;
import com.rajesh.aashraynet.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository users;
    private final RevokedTokenRepository revokedTokens;
    private final AuditService audit;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository users,
                       RevokedTokenRepository revokedTokens, AuditService audit) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.users = users;
        this.revokedTokens = revokedTokens;
        this.audit = audit;
    }

    /** Throws BadCredentialsException (mapped to 401 with a generic message) on any failure. */
    @Transactional
    public LoginResponse login(LoginRequest req) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        JwtService.IssuedToken token = jwtService.issue(principal);
        audit.log(principal.id(), "LOGIN", "USER", principal.id(), null);
        UserResponse user = UserResponse.from(users.findById(principal.id()).orElseThrow());
        return new LoginResponse(token.token(), "Bearer", token.expiresAt(), user);
    }

    /** Stateless JWTs cannot be "deleted", so the token id is blacklisted until it would expire anyway. */
    @Transactional
    public void logout(String jti, Instant expiresAt, Long userId) {
        if (jti == null || revokedTokens.existsById(jti)) return;
        revokedTokens.save(new RevokedToken(jti, LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)));
        audit.log(userId, "LOGOUT", "USER", userId, null);
    }
}
