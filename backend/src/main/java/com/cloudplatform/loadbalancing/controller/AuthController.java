package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.dto.UserDto;
import com.cloudplatform.loadbalancing.entity.User;
import com.cloudplatform.loadbalancing.service.AuthenticationService;
import com.cloudplatform.loadbalancing.service.JwtService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.authenticateUserAndGetEntity(request.getUsername(), request.getPassword(), 
                        httpRequest.getRemoteAddr())
                .map(user -> {
                    String token = jwtService.generateToken(user);
                    return ResponseEntity.ok(Map.of(
                            "user", UserDto.fromEntity(user),
                            "token", token
                    ));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest request) {
        UserDto user = authService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                User.Role.READONLY
        );
        return ResponseEntity.ok(user);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable UUID userId) {
        return authService.getUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody UpdateRoleRequest request) {
        UserDto user = authService.updateUserRole(userId, request.getRole(), request.getAdminUsername());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{userId}/module")
    public ResponseEntity<UserDto> updateSelectedModule(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> request) {
        UserDto user = authService.updateSelectedModule(userId, request.get("module"));
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/{userId}/accounts/{accountId}/grant")
    public ResponseEntity<UserDto> grantAccountAccess(
            @PathVariable UUID userId,
            @PathVariable UUID accountId) {
        UserDto user = authService.grantAccountAccess(userId, accountId, "admin");
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/{userId}/accounts/{accountId}/revoke")
    public ResponseEntity<UserDto> revokeAccountAccess(
            @PathVariable UUID userId,
            @PathVariable UUID accountId) {
        UserDto user = authService.revokeAccountAccess(userId, accountId, "admin");
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable UUID userId) {
        authService.suspendUser(userId, "admin");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable UUID userId) {
        authService.activateUser(userId, "admin");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/profile")
    public ResponseEntity<UserDto> updateProfile(
            @PathVariable UUID userId,
            @RequestBody UpdateProfileRequest request) {
        UserDto user = authService.updateProfile(userId, request.getFirstName(), request.getLastName(), request.getEmail());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/{userId}/change-password")
    public ResponseEntity<?> changePassword(
            @PathVariable UUID userId,
            @RequestBody ChangePasswordRequest request) {
        boolean success = authService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));
        }
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
    }

    @Data
    public static class UpdateRoleRequest {
        private User.Role role;
        private String adminUsername;
    }

    @Data
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;
        private String email;
    }

    @Data
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }
}

