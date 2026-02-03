package com.cloudplatform.loadbalancing.dto;

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
 * DTOs for Control Loop operations
 */
public class ControlLoopDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObservationData {
        private UUID serviceId;
        private String serviceName;
        private Instant timestamp;
        private Instant collectedAt;
        private String cycleId;
        
        // ASG State
        private Integer desiredCapacity;
        private Integer runningInstances;
        private Integer healthyInstances;
        private Integer unhealthyInstances;
        
        // Metrics
        private BigDecimal avgCpu;
        private BigDecimal avgMemory;
        private BigDecimal avgNetworkIn;
        private BigDecimal avgNetworkOut;
        
        // Latency
        private Integer latencyP50;
        private Integer latencyP95;
        private Integer latencyP99;
        
        // Error Rates
        private BigDecimal errorRate;
        private Integer error4xxCount;
        private Integer error5xxCount;
        
        // Request Metrics
        private Integer requestsPerSecond;
        private Integer totalRequestsPerMinute;
        private Integer activeConnections;
        
        // SLO Status
        private BigDecimal sloBurnRate;
        private Boolean sloViolation;
        
        // Per-region observations
        private List<RegionObservation> regions;
        
        // AI/ML Advisory (optional)
        private LoadPrediction loadPrediction;
        private AnomalyDetection anomalyDetection;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionObservation {
        private String region;
        private Boolean isPrimary;
        private String status;
        private Integer desiredCapacity;
        private Integer runningInstances;
        private Integer healthyInstances;
        private BigDecimal avgCpu;
        private Integer latencyP99;
        private BigDecimal errorRate;
        private Integer trafficWeight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoadPrediction {
        private Boolean available;
        private BigDecimal predictedCpu5Min;
        private BigDecimal predictedCpu15Min;
        private BigDecimal predictedRps5Min;
        private BigDecimal predictedRps15Min;
        private BigDecimal confidence;
        private String trend; // INCREASING, DECREASING, STABLE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalyDetection {
        private Boolean available;
        private BigDecimal anomalyScore;
        private Boolean isAnomaly;
        private String anomalyType;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResult {
        private String cycleId;
        private String overallStatus; // HEALTHY, OVERLOADED, DEGRADED, CRITICAL
        private String recommendation; // SCALE_OUT, SCALE_IN, NO_CHANGE, FAILOVER
        
        // Scaling Analysis
        private Boolean scaleOutNeeded;
        private Boolean scaleInAllowed;
        private Integer suggestedCapacity;
        private String scalingReason;
        
        // Health Analysis
        private List<String> healthIssues;
        private List<String> warnings;
        
        // Guardrail Status
        private Boolean guardrailsPassed;
        private List<String> guardrailViolations;
        
        // Cooldown Status
        private Boolean inScaleOutCooldown;
        private Boolean inScaleInCooldown;
        private Instant cooldownExpiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionPlan {
        private String cycleId;
        private List<PlannedAction> actions;
        private Boolean requiresApproval;
        private String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlannedAction {
        private String actionType;
        private String targetResource;
        private String region;
        private Map<String, Object> parameters;
        private String description;
        private Integer priority;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionResult {
        private String cycleId;
        private String actionType;
        private String description;
        private Boolean success;
        private Map<String, Object> observationData;
        private Map<String, Object> analysisResult;
        private Map<String, Object> executionDetails;
        private List<ActionResult> actionResults;
        private Long durationMs;
        private Instant executedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionResult {
        private String actionType;
        private Boolean success;
        private String awsRequestId;
        private String errorMessage;
        private Long durationMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ControlLoopStatus {
        private Boolean enabled;
        private Boolean isLeader;
        private String instanceId;
        private String currentLeader;
        private String leaderInstanceId;
        private Instant lastCycleAt;
        private Instant lastCheckedAt;
        private Long lastCycleDurationMs;
        private Integer activeServicesCount;
        private Integer cyclesCompleted;
        private Integer cyclesFailed;
        private Map<String, ServiceLoopStatus> serviceStatuses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceLoopStatus {
        private UUID serviceId;
        private String serviceName;
        private Instant lastObservationAt;
        private String lastDecision;
        private Integer consecutiveFailures;
        private Boolean automationEnabled;
    }
}
