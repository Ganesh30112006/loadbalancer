package com.cloudplatform.loadbalancing.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for Policy operations
 */
public class PolicyDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotNull(message = "AWS Account ID is required")
        private UUID awsAccountId;
        
        @NotNull(message = "Service ID is required")
        private UUID serviceId;
        
        @NotBlank(message = "Policy name is required")
        @Size(max = 100, message = "Name must be less than 100 characters")
        private String name;
        
        @Size(max = 500, message = "Description must be less than 500 characters")
        private String description;
        
        // Configuration Maps
        private Map<String, Object> sloConfig;
        private Map<String, Object> scalingRules;
        private Map<String, Object> costConfig;
        private Map<String, Object> deploymentConfig;
        
        // SLO Configuration
        @DecimalMin(value = "90.0", message = "SLO availability must be at least 90%")
        @DecimalMax(value = "100.0", message = "SLO availability cannot exceed 100%")
        private BigDecimal sloAvailability;
        
        @Min(value = 10, message = "SLO latency must be at least 10ms")
        @Max(value = 60000, message = "SLO latency must be at most 60 seconds")
        private Integer sloLatencyP99Ms;
        
        @DecimalMin(value = "0.0", message = "SLO error rate cannot be negative")
        @DecimalMax(value = "100.0", message = "SLO error rate cannot exceed 100%")
        private BigDecimal sloErrorRate;
        
        // Scaling Configuration
        @Min(value = 1, message = "Min instances must be at least 1")
        private Integer minInstances;
        
        @Min(value = 1, message = "Max instances must be at least 1")
        private Integer maxInstances;
        
        @Min(value = 1, message = "Desired instances must be at least 1")
        private Integer desiredInstances;
        
        @Min(value = 1, message = "Scale out CPU threshold must be at least 1%")
        @Max(value = 100, message = "Scale out CPU threshold cannot exceed 100%")
        private Integer scaleOutCpuThreshold;
        
        @Min(value = 1, message = "Scale in CPU threshold must be at least 1%")
        @Max(value = 100, message = "Scale in CPU threshold cannot exceed 100%")
        private Integer scaleInCpuThreshold;
        
        @Min(value = 60, message = "Scale out cooldown must be at least 60 seconds")
        private Integer scaleOutCooldownSeconds;
        
        @Min(value = 60, message = "Scale in cooldown must be at least 60 seconds")
        private Integer scaleInCooldownSeconds;
        
        @Min(value = 1, message = "Max scale out step must be at least 1")
        private Integer maxScaleOutStep;
        
        @Min(value = 1, message = "Max scale in step must be at least 1")
        private Integer maxScaleInStep;
        
        // Cost Configuration
        @Min(value = 1, message = "Min On-Demand instances must be at least 1")
        private Integer minOnDemandInstances;
        
        @DecimalMin(value = "0.0", message = "Max Spot ratio cannot be negative")
        @DecimalMax(value = "1.0", message = "Max Spot ratio cannot exceed 1.0")
        private BigDecimal maxSpotRatio;
        
        @DecimalMin(value = "0.0", message = "Max hourly cost cannot be negative")
        private BigDecimal maxHourlyCost;
        
        // Deployment Configuration
        private String deploymentStrategy;
        private String canarySteps;
        private Integer canaryStepIntervalMinutes;
        private Boolean autoRollbackEnabled;
        
        // Advanced Configuration
        private Map<String, Object> policyConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        // Same fields as CreateRequest (except serviceId)
        private String name;
        private String description;
        
        // Configuration Maps
        private Map<String, Object> sloConfig;
        private Map<String, Object> scalingRules;
        private Map<String, Object> costConfig;
        private Map<String, Object> deploymentConfig;
        
        private BigDecimal sloAvailability;
        private Integer sloLatencyP99Ms;
        private BigDecimal sloErrorRate;
        private Integer minInstances;
        private Integer maxInstances;
        private Integer desiredInstances;
        private Integer scaleOutCpuThreshold;
        private Integer scaleInCpuThreshold;
        private Integer scaleOutCooldownSeconds;
        private Integer scaleInCooldownSeconds;
        private Integer maxScaleOutStep;
        private Integer maxScaleInStep;
        private Integer minOnDemandInstances;
        private BigDecimal maxSpotRatio;
        private BigDecimal maxHourlyCost;
        private String deploymentStrategy;
        private String canarySteps;
        private Integer canaryStepIntervalMinutes;
        private Boolean autoRollbackEnabled;
        private Map<String, Object> policyConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID serviceId;
        private String serviceName;
        private String name;
        private String description;
        private Integer version;
        
        // SLO
        private BigDecimal sloAvailability;
        private Integer sloLatencyP99Ms;
        private BigDecimal sloErrorRate;
        
        // Scaling
        private Integer minInstances;
        private Integer maxInstances;
        private Integer desiredInstances;
        private Integer scaleOutCpuThreshold;
        private Integer scaleInCpuThreshold;
        private Integer scaleOutCooldownSeconds;
        private Integer scaleInCooldownSeconds;
        private Integer maxScaleOutStep;
        private Integer maxScaleInStep;
        
        // Cost
        private Integer minOnDemandInstances;
        private BigDecimal maxSpotRatio;
        private BigDecimal maxHourlyCost;
        
        // Deployment
        private String deploymentStrategy;
        private String canarySteps;
        private Integer canaryStepIntervalMinutes;
        private Boolean autoRollbackEnabled;
        
        // Advanced
        private Map<String, Object> policyConfig;
        
        // Status
        private String status;
        private Boolean isActive;
        private UUID createdBy;
        private UUID approvedBy;
        private Instant approvedAt;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalRequest {
        private String approvalComments;
    }
}
