package com.cloudplatform.loadbalancing.dto;

import com.cloudplatform.loadbalancing.entity.ControlLoopExecution;
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
public class ControlLoopExecutionDto {
    private UUID id;
    private UUID serviceId;
    private String serviceName;
    private ControlLoopExecution.ExecutionPhase phase;
    private ControlLoopExecution.ExecutionStatus status;
    private String region;
    
    // Metrics
    private Double cpuUtilization;
    private Double memoryUtilization;
    private Double latencyP99;
    private Double errorRate;
    private Double requestsPerSecond;
    private Integer currentCapacity;
    
    // Analysis
    private String analysisReason;
    private ControlLoopExecution.ScalingDecision scalingDecision;
    private Integer targetCapacity;
    
    // AI Advisory
    private String aiAdvisory;
    private Double aiConfidenceScore;
    
    // Execution
    private String executionDetails;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private Instant createdAt;
    
    public static ControlLoopExecutionDto fromEntity(ControlLoopExecution execution) {
        return ControlLoopExecutionDto.builder()
                .id(execution.getId())
                .serviceId(execution.getService().getId())
                .serviceName(execution.getService().getServiceName())
                .phase(execution.getPhase())
                .status(execution.getStatus())
                .region(execution.getRegion())
                .cpuUtilization(execution.getCpuUtilization())
                .memoryUtilization(execution.getMemoryUtilization())
                .latencyP99(execution.getLatencyP99())
                .errorRate(execution.getErrorRate())
                .requestsPerSecond(execution.getRequestsPerSecond())
                .currentCapacity(execution.getCurrentCapacity())
                .analysisReason(execution.getAnalysisReason())
                .scalingDecision(execution.getScalingDecision())
                .targetCapacity(execution.getTargetCapacity())
                .aiAdvisory(execution.getAiAdvisory())
                .aiConfidenceScore(execution.getAiConfidenceScore())
                .executionDetails(execution.getExecutionDetails())
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .durationMs(execution.getDurationMs())
                .createdAt(execution.getCreatedAt())
                .build();
    }
}
