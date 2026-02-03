package com.cloudplatform.loadbalancing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_loop_executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlLoopExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExecutionPhase phase = ExecutionPhase.COLLECTING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.RUNNING;
    
    private String region;
    
    // Metrics captured during execution
    private Double cpuUtilization;
    private Double memoryUtilization;
    private Double latencyP99;
    private Double errorRate;
    private Double requestsPerSecond;
    private Integer currentCapacity;
    
    // Analysis results
    @Column(columnDefinition = "TEXT")
    private String analysisReason;
    
    @Enumerated(EnumType.STRING)
    private ScalingDecision scalingDecision;
    
    private Integer targetCapacity;
    
    // AI Advisory
    @Column(columnDefinition = "TEXT")
    private String aiAdvisory;
    
    private Double aiConfidenceScore;
    
    // Execution details
    @Column(columnDefinition = "TEXT")
    private String executionDetails;
    
    @Column(nullable = false)
    private Instant startedAt;
    
    private Instant completedAt;
    
    private Long durationMs;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    public enum ExecutionPhase {
        COLLECTING,     // Collecting metrics
        ANALYZING,      // Analyzing data
        DECIDING,       // Making scaling decision
        EXECUTING,      // Executing scaling action
        VALIDATING,     // Validating results
        COMPLETED       // Finished
    }
    
    public enum ExecutionStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        NO_ACTION_NEEDED,
        BLOCKED_BY_GUARDRAIL
    }
    
    public enum ScalingDecision {
        SCALE_OUT,
        SCALE_IN,
        MAINTAIN,
        EMERGENCY_SCALE
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (startedAt == null) {
            startedAt = createdAt;
        }
    }
}
