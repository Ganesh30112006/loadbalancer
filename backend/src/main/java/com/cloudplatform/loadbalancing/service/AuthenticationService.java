package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.dto.UserDto;
import com.cloudplatform.loadbalancing.entity.User;
import com.cloudplatform.loadbalancing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto registerUser(String username, String email, String password, 
                                String firstName, String lastName, User.Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("Registered new user: {} with role: {}", username, role);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public Optional<UserDto> authenticateUser(String username, String password, String ipAddress) {
        return authenticateUserAndGetEntity(username, password, ipAddress)
                .map(UserDto::fromEntity);
    }

    @Transactional
    public Optional<User> authenticateUserAndGetEntity(String usernameOrEmail, String password, String ipAddress) {
        // Try to find user by username first, then by email
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        
        if (userOpt.isEmpty()) {
            // Try finding by email
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }
        
        if (userOpt.isEmpty()) {
            log.warn("Authentication failed - user not found: {}", usernameOrEmail);
            return Optional.empty();
        }

        User user = userOpt.get();
        
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Authentication failed - user not active: {}", usernameOrEmail);
            return Optional.empty();
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Authentication failed - invalid password for user: {}", usernameOrEmail);
            return Optional.empty();
        }

        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);
        
        log.info("User authenticated: {}", usernameOrEmail);
        return Optional.of(user);
    }

    public Optional<UserDto> getUserById(UUID userId) {
        return userRepository.findById(userId).map(UserDto::fromEntity);
    }

    public Optional<UserDto> getUserByUsername(String username) {
        return userRepository.findByUsername(username).map(UserDto::fromEntity);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateUserRole(UUID userId, User.Role newRole, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        User.Role oldRole = user.getRole();
        user.setRole(newRole);
        user = userRepository.save(user);
        
        log.info("User {} role changed from {} to {} by {}", 
                user.getUsername(), oldRole, newRole, adminUsername);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateSelectedModule(UUID userId, String module) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setSelectedModule(module);
        user = userRepository.save(user);
        
        log.info("User {} selected module: {}", user.getUsername(), module);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto grantAccountAccess(UUID userId, UUID accountId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.getAccessibleAccounts().add(accountId);
        user = userRepository.save(user);
        
        log.info("Account {} access granted to user {} by {}", 
                accountId, user.getUsername(), adminUsername);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto revokeAccountAccess(UUID userId, UUID accountId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.getAccessibleAccounts().remove(accountId);
        user = userRepository.save(user);
        
        log.info("Account {} access revoked from user {} by {}", 
                accountId, user.getUsername(), adminUsername);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public void suspendUser(UUID userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setStatus(User.UserStatus.SUSPENDED);
        userRepository.save(user);
        
        log.info("User {} suspended by {}", user.getUsername(), adminUsername);
    }

    @Transactional
    public void activateUser(UUID userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        
        log.info("User {} activated by {}", user.getUsername(), adminUsername);
    }

    @Transactional
    public UserDto updateProfile(UUID userId, String firstName, String lastName, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check if email is being changed and is not already taken
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user = userRepository.save(user);
        
        log.info("User {} profile updated", user.getUsername());
        return UserDto.fromEntity(user);
    }

    @Transactional
    public boolean changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.warn("Password change failed - incorrect current password for user: {}", user.getUsername());
            return false;
        }
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password changed for user: {}", user.getUsername());
        return true;
    }

    // Initialize default admin user if none exists
    @Transactional
    public void initializeDefaultAdmin() {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@loadbalancing.local")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .firstName("System")
                    .lastName("Administrator")
                    .role(User.Role.ADMIN)
                    .status(User.UserStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
            log.info("Created default admin user");
        }
    }
}
