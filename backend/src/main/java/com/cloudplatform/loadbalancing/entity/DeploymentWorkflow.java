package com.cloudplatform.loadbalancing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Deployment Workflow for Blue/Green and Canary deployments
 */
@Entity
@Table(name = "deployment_workflows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id", nullable = false)
    private Deployment deployment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentStrategy strategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WorkflowStatus status = WorkflowStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WorkflowPhase currentPhase = WorkflowPhase.INITIALIZING;

    // Traffic configuration
    @Column
    @Builder.Default
    private Integer blueTrafficPercent = 100;

    @Column
    @Builder.Default
    private Integer greenTrafficPercent = 0;

    @Column
    @Builder.Default
    private Integer canaryTrafficPercent = 0;

    // Health thresholds
    @Column
    @Builder.Default
    private Double healthyThreshold = 0.95;

    @Column
    @Builder.Default
    private Double errorRateThreshold = 0.01;

    @Column
    @Builder.Default
    private Double latencyThresholdMs = 500.0;

    // Canary specific
    @Column
    @Builder.Default
    private Integer canarySteps = 5;

    @Column
    @Builder.Default
    private Integer currentCanaryStep = 0;

    @Column
    @Builder.Default
    private Integer stepDurationMinutes = 5;

    // Rollback configuration
    @Column
    @Builder.Default
    private Boolean autoRollbackEnabled = true;

    @Column
    private Instant rollbackTriggeredAt;

    @Column
    private String rollbackReason;

    // Progress tracking
    @Column(columnDefinition = "TEXT")
    private String progressLog;

    @Column
    private String initiatedBy;

    @Column
    private Instant startedAt;

    @Column
    private Instant completedAt;

    @Column
    private Long durationMs;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public enum DeploymentStrategy {
        BLUE_GREEN,
        CANARY,
        ROLLING
    }

    public enum WorkflowStatus {
        PENDING,
        IN_PROGRESS,
        PAUSED,
        COMPLETED,
        FAILED,
        ROLLED_BACK
    }

    public enum WorkflowPhase {
        INITIALIZING,
        PROVISIONING,
        HEALTH_CHECK,
        TRAFFIC_SHIFTING,
        MONITORING,
        FINALIZING,
        ROLLING_BACK,
        COMPLETED
    }

    public void appendProgressLog(String message) {
        String timestamp = Instant.now().toString();
        String entry = "[" + timestamp + "] " + message + "\n";
        if (this.progressLog == null) {
            this.progressLog = entry;
        } else {
            this.progressLog += entry;
        }
    }
}
