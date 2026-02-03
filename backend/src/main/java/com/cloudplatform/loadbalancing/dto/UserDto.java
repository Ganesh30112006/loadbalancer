package com.cloudplatform.loadbalancing.dto;

import com.cloudplatform.loadbalancing.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private User.Role role;
    private User.UserStatus status;
    private String selectedModule;
    private Set<UUID> accessibleAccounts;
    private Instant lastLoginAt;
    private Instant createdAt;
    
    // Permission flags for frontend
    private boolean canRead;
    private boolean canOperate;
    private boolean canAdmin;
    
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .selectedModule(user.getSelectedModule())
                .accessibleAccounts(user.getAccessibleAccounts())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .canRead(user.canRead())
                .canOperate(user.canOperate())
                .canAdmin(user.canAdmin())
                .build();
    }
}
