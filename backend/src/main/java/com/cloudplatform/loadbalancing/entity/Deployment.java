package com.cloudplatform.loadbalancing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Deployment Entity
 * 
 * Tracks deployments (blue/green, canary) with full state machine
 * for progress tracking and rollback support.
 */
@Entity
@Table(name = "deployments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to Service
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    /**
     * Source Blueprint (current version)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_blueprint_id")
    private Blueprint sourceBlueprint;

    /**
     * Target Blueprint (new version)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_blueprint_id", nullable = false)
    private Blueprint targetBlueprint;

    /**
     * Deployment type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "deployment_type", nullable = false, length = 20)
    private DeploymentType deploymentType;

    /**
     * Deployment status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private DeploymentStatus status = DeploymentStatus.PENDING;

    /**
     * Current deployment phase
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", length = 30)
    private DeploymentPhase currentPhase;

    /**
     * Canary traffic percentage (0-100)
     */
    @Column(name = "canary_percentage")
    @Builder.Default
    private Integer canaryPercentage = 0;

    /**
     * Current canary step (1-based)
     */
    @Column(name = "canary_step")
    @Builder.Default
    private Integer canaryStep = 0;

    /**
     * Canary steps configuration (e.g., "5,25,50,100")
     */
    @Column(name = "canary_steps", length = 100)
    private String canarySteps;

    /**
     * AWS resources created for deployment
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deployment_resources")
    private Map<String, Object> deploymentResources;

    /**
     * Deployment metrics and progress
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deployment_metrics")
    private Map<String, Object> deploymentMetrics;

    /**
     * Rollback reason (if rolled back)
     */
    @Column(name = "rollback_reason", length = 500)
    private String rollbackReason;

    /**
     * Whether rollback was automatic
     */
    @Column(name = "is_auto_rollback")
    private Boolean isAutoRollback;

    /**
     * User who initiated deployment
     */
    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    /**
     * User who approved deployment (if approval required)
     */
    @Column(name = "approved_by")
    private UUID approvedBy;

    /**
     * Deployment start time
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Deployment completion time
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Deployment type enum
     */
    public enum DeploymentType {
        BLUE_GREEN,
        CANARY,
        ROLLING
    }

    /**
     * Deployment status enum
     */
    public enum DeploymentStatus {
        PENDING,
        APPROVED,
        IN_PROGRESS,
        VERIFYING,
        COMPLETED,
        ROLLING_BACK,
        ROLLED_BACK,
        FAILED,
        CANCELLED
    }

    /**
     * Deployment phase enum
     */
    public enum DeploymentPhase {
        // Blue/Green phases
        CREATING_TARGET_ASG,
        LAUNCHING_INSTANCES,
        WARMING_UP,
        RUNNING_SMOKE_TESTS,
        SWITCHING_TRAFFIC,
        DRAINING_SOURCE,
        CLEANUP,
        
        // Canary phases
        LAUNCHING_CANARY,
        CANARY_TRAFFIC_SHIFT,
        CANARY_MONITORING,
        FULL_CUTOVER,
        
        // Common phases
        VERIFICATION,
        ROLLBACK_INITIATED,
        ROLLBACK_IN_PROGRESS
    }
}


