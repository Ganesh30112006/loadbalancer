package com.cloudplatform.loadbalancing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Column(nullable = false)
    private String email;
    
    private String firstName;
    
    private String lastName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    
    private String selectedModule;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_accessible_accounts", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "account_id")
    @Builder.Default
    private Set<UUID> accessibleAccounts = new HashSet<>();
    
    private Instant lastLoginAt;
    
    private String lastLoginIp;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    public enum Role {
        ADMIN,      // Full access
        OPERATOR,   // Can modify but not delete critical resources
        READONLY    // View only
    }
    
    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return username;
        }
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }
    
    public boolean canRead() {
        return true; // All roles can read
    }
    
    public boolean canOperate() {
        return role == Role.ADMIN || role == Role.OPERATOR;
    }
    
    public boolean canAdmin() {
        return role == Role.ADMIN;
    }
}
