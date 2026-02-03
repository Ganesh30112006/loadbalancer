package com.cloudplatform.loadbalancing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for Deployment operations
 */
public class DeploymentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotNull(message = "Service ID is required")
        private UUID serviceId;
        
        @NotNull(message = "Target Blueprint ID is required")
        private UUID targetBlueprintId;
        
        private String deploymentType; // BLUE_GREEN, CANARY, ROLLING
        
        private String canarySteps; // e.g., "5,25,50,100"
        
        private Integer canaryStepIntervalMinutes;
        
        private Boolean skipApproval;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID serviceId;
        private String serviceName;
        private UUID sourceBlueprintId;
        private String sourceBlueprintName;
        private Integer sourceBlueprintVersion;
        private UUID targetBlueprintId;
        private String targetBlueprintName;
        private Integer targetBlueprintVersion;
        private String deploymentType;
        private String status;
        private String currentPhase;
        private Integer canaryPercentage;
        private Integer canaryStep;
        private String canarySteps;
        private Map<String, Object> deploymentResources;
        private Map<String, Object> deploymentMetrics;
        private String rollbackReason;
        private Boolean isAutoRollback;
        private UUID initiatedBy;
        private UUID approvedBy;
        private Instant startedAt;
        private Instant completedAt;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressUpdate {
        private UUID deploymentId;
        private String status;
        private String currentPhase;
        private Integer canaryPercentage;
        private String message;
        private Map<String, Object> metrics;
        private Instant timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollbackRequest {
        private String reason;
    }
}
