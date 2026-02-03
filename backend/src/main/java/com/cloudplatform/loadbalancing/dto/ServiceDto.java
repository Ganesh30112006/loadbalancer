package com.cloudplatform.loadbalancing.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for Service operations
 */
public class ServiceDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotNull(message = "AWS Account ID is required")
        private UUID awsAccountId;
        
        @NotBlank(message = "Service name is required")
        @Size(max = 100, message = "Name must be less than 100 characters")
        private String serviceName;
        
        @Size(max = 100, message = "Display name must be less than 100 characters")
        private String displayName;
        
        @Size(max = 500, message = "Description must be less than 500 characters")
        private String description;
        
        @NotBlank(message = "Primary region is required")
        private String primaryRegion;
        
        private List<String> regions;
        
        @NotNull(message = "Blueprint ID is required")
        private UUID blueprintId;
        
        private UUID policyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @Size(max = 500, message = "Description must be less than 500 characters")
        private String description;
        
        private List<String> secondaryRegions;
        private UUID blueprintId;
        private UUID policyId;
        private Boolean automationEnabled;
        private String overrideReason;
        private Integer overrideExpiryMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID awsAccountId;
        private String accountName;
        private String serviceName;
        private String displayName;
        private String description;
        private String primaryRegion;
        private List<String> secondaryRegions;
        private UUID blueprintId;
        private String blueprintName;
        private UUID policyId;
        private String policyName;
        private String status;
        private Boolean automationEnabled;
        private Boolean scalingEnabled;
        private Boolean deploymentEnabled;
        private List<RegionHealthSummary> regions;
        private Instant lastHealthCheckAt;
        private Map<String, Object> healthStatusData;
        private String overrideReason;
        private Instant overrideExpiresAt;
        private Map<String, Object> awsResources;
        private HealthSummary healthStatus;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthSummary {
        private UUID serviceId;
        private String serviceName;
        private String overallStatus;
        private BigDecimal healthScore;
        private Integer totalInstances;
        private Integer healthyInstances;
        private Integer unhealthyInstances;
        private BigDecimal avgCpuUtilization;
        private Integer avgLatencyP99Ms;
        private BigDecimal errorRate;
        private Integer requestsPerSecond;
        private Instant lastCheckedAt;
        private List<String> sloViolations;
        private List<RegionHealthSummary> regionHealth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionHealthSummary {
        private String region;
        private Boolean isPrimary;
        private String status;
        private String asgName;
        private BigDecimal healthScore;
        private Integer desiredCapacity;
        private Integer runningInstances;
        private Integer healthyInstances;
        private Integer trafficWeight;
        private BigDecimal avgCpu;
        private Integer avgLatencyP99;
        private BigDecimal errorRate;
        private Instant lastMetricsAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverrideRequest {
        
        @NotBlank(message = "Override reason is required")
        @Size(max = 500, message = "Reason must be less than 500 characters")
        private String reason;
        
        @Min(value = 1, message = "Expiry must be at least 1 minute")
        @Max(value = 10080, message = "Expiry must be at most 7 days (10080 minutes)")
        private Integer overrideDurationMinutes;
        
        private Boolean disableAutomation;
        private Boolean disableScaling;
        private Boolean disableDeployment;
    }
}
