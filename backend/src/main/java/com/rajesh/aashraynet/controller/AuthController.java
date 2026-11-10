package com.rajesh.aashraynet.controller;

import com.rajesh.aashraynet.dto.request.ChangePasswordRequest;
import com.rajesh.aashraynet.dto.request.LoginRequest;
import com.rajesh.aashraynet.dto.response.LoginResponse;
import com.rajesh.aashraynet.dto.response.UserResponse;
import com.rajesh.aashraynet.security.CurrentUser;
import com.rajesh.aashraynet.security.JwtAuthenticationFilter;
import com.rajesh.aashraynet.service.AuthService;
import com.rajesh.aashraynet.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Log in and receive a JWT (rate-limited)")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "Revoke the current token")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout((String) request.getAttribute(JwtAuthenticationFilter.JTI_ATTRIBUTE),
                (Instant) request.getAttribute(JwtAuthenticationFilter.EXP_ATTRIBUTE), CurrentUser.get().id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/me")
    public UserResponse me() {
        return userService.me(CurrentUser.get());
    }

    @PatchMapping("/users/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(req, CurrentUser.get());
        return ResponseEntity.noContent().build();
    }
}
