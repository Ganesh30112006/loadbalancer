package com.cloudplatform.loadbalancing.dto;

import com.cloudplatform.loadbalancing.entity.DeploymentWorkflow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentWorkflowDto {
    private UUID id;
    private UUID serviceId;
    private String serviceName;
    private UUID deploymentId;
    private DeploymentWorkflow.DeploymentStrategy strategy;
    private DeploymentWorkflow.WorkflowStatus status;
    private DeploymentWorkflow.WorkflowPhase currentPhase;
    
    // Traffic
    private Integer blueTrafficPercent;
    private Integer greenTrafficPercent;
    private Integer canaryTrafficPercent;
    
    // Health thresholds
    private Double healthyThreshold;
    private Double errorRateThreshold;
    private Double latencyThresholdMs;
    
    // Canary
    private Integer canarySteps;
    private Integer currentCanaryStep;
    private Integer stepDurationMinutes;
    
    // Rollback
    private Boolean autoRollbackEnabled;
    private Instant rollbackTriggeredAt;
    private String rollbackReason;
    
    // Progress
    private String progressLog;
    private String initiatedBy;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private Instant createdAt;
    
    public static DeploymentWorkflowDto fromEntity(DeploymentWorkflow workflow) {
        return DeploymentWorkflowDto.builder()
                .id(workflow.getId())
                .serviceId(workflow.getService().getId())
                .serviceName(workflow.getService().getServiceName())
                .deploymentId(workflow.getDeployment().getId())
                .strategy(workflow.getStrategy())
                .status(workflow.getStatus())
                .currentPhase(workflow.getCurrentPhase())
                .blueTrafficPercent(workflow.getBlueTrafficPercent())
                .greenTrafficPercent(workflow.getGreenTrafficPercent())
                .canaryTrafficPercent(workflow.getCanaryTrafficPercent())
                .healthyThreshold(workflow.getHealthyThreshold())
                .errorRateThreshold(workflow.getErrorRateThreshold())
                .latencyThresholdMs(workflow.getLatencyThresholdMs())
                .canarySteps(workflow.getCanarySteps())
                .currentCanaryStep(workflow.getCurrentCanaryStep())
                .stepDurationMinutes(workflow.getStepDurationMinutes())
                .autoRollbackEnabled(workflow.getAutoRollbackEnabled())
                .rollbackTriggeredAt(workflow.getRollbackTriggeredAt())
                .rollbackReason(workflow.getRollbackReason())
                .progressLog(workflow.getProgressLog())
                .initiatedBy(workflow.getInitiatedBy())
                .startedAt(workflow.getStartedAt())
                .completedAt(workflow.getCompletedAt())
                .durationMs(workflow.getDurationMs())
                .createdAt(workflow.getCreatedAt())
                .build();
    }
}
