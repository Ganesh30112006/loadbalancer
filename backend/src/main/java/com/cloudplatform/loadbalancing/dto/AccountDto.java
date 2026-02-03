package com.cloudplatform.loadbalancing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for AWS Account operations
 */
public class AccountDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "Account ID is required")
        @Pattern(regexp = "^\\d{12}$", message = "Account ID must be 12 digits")
        private String accountId;
        
        @NotBlank(message = "Account name is required")
        @Size(max = 100, message = "Account name must be less than 100 characters")
        private String accountName;
        
        @NotBlank(message = "Role ARN is required")
        @Pattern(regexp = "^arn:aws:iam::\\d{12}:role/.+$", message = "Invalid IAM Role ARN format")
        private String roleArn;
        
        private List<String> enabledRegions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @Size(max = 100, message = "Account name must be less than 100 characters")
        private String accountName;
        
        @Pattern(regexp = "^arn:aws:iam::\\d{12}:role/.+$", message = "Invalid IAM Role ARN format")
        private String roleArn;
        
        private List<String> enabledRegions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String accountId;
        private String accountName;
        private String roleArn;
        private String externalId;
        private String status;
        private List<String> enabledRegions;
        private Instant lastValidatedAt;
        private String validationError;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingResponse {
        private UUID id;
        private String accountId;
        private String accountName;
        private String externalId;
        private String trustPolicyTemplate;
        private String iamRoleSetupInstructions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private String accountId;
        private String assumedRoleArn;
        private String errorMessage;
        private Instant validatedAt;
    }
}
