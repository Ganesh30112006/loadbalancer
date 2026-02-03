package com.cloudplatform.loadbalancing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Policy Entity
 * 
 * Defines scaling policies, SLO targets, cost limits, and safety guardrails
 * for a service. Policies are versioned and require approval.
 */
@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

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
     * Reference to AWS Account
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aws_account_id", nullable = false)
    private AwsAccount awsAccount;

    /**
     * Policy name
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Policy description
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Policy version
     */
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    // ============ SLO Configuration ============

    /**
     * Target availability percentage (e.g., 99.9)
     */
    @Column(name = "slo_availability", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal sloAvailability = new BigDecimal("99.9");

    /**
     * Maximum allowed latency P99 in milliseconds
     */
    @Column(name = "slo_latency_p99_ms")
    @Builder.Default
    private Integer sloLatencyP99Ms = 500;

    /**
     * Maximum allowed error rate percentage
     */
    @Column(name = "slo_error_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal sloErrorRate = new BigDecimal("1.0");

    // ============ Scaling Configuration ============

    /**
     * Minimum instance count
     */
    @Column(name = "min_instances", nullable = false)
    @Builder.Default
    private Integer minInstances = 2;

    /**
     * Maximum instance count
     */
    @Column(name = "max_instances", nullable = false)
    @Builder.Default
    private Integer maxInstances = 100;

    /**
     * Desired instance count (initial)
     */
    @Column(name = "desired_instances", nullable = false)
    @Builder.Default
    private Integer desiredInstances = 2;

    /**
     * CPU threshold for scale-out (percentage)
     */
    @Column(name = "scale_out_cpu_threshold")
    @Builder.Default
    private Integer scaleOutCpuThreshold = 70;

    /**
     * CPU threshold for scale-in (percentage)
     */
    @Column(name = "scale_in_cpu_threshold")
    @Builder.Default
    private Integer scaleInCpuThreshold = 30;

    /**
     * Scale-out cooldown in seconds
     */
    @Column(name = "scale_out_cooldown_seconds")
    @Builder.Default
    private Integer scaleOutCooldownSeconds = 300;

    /**
     * Scale-in cooldown in seconds
     */
    @Column(name = "scale_in_cooldown_seconds")
    @Builder.Default
    private Integer scaleInCooldownSeconds = 600;

    /**
     * Maximum instances to add per scaling event
     */
    @Column(name = "max_scale_out_step")
    @Builder.Default
    private Integer maxScaleOutStep = 5;

    /**
     * Maximum instances to remove per scaling event
     */
    @Column(name = "max_scale_in_step")
    @Builder.Default
    private Integer maxScaleInStep = 2;

    // ============ Cost Configuration ============

    /**
     * Minimum On-Demand instances (safety floor)
     */
    @Column(name = "min_on_demand_instances")
    @Builder.Default
    private Integer minOnDemandInstances = 2;

    /**
     * Maximum Spot instance ratio (0.0 - 1.0)
     */
    @Column(name = "max_spot_ratio", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal maxSpotRatio = new BigDecimal("0.70");

    /**
     * Maximum hourly cost limit in USD
     */
    @Column(name = "max_hourly_cost", precision = 10, scale = 2)
    private BigDecimal maxHourlyCost;

    // ============ Deployment Configuration ============

    /**
     * Deployment strategy
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "deployment_strategy", length = 20)
    @Builder.Default
    private DeploymentStrategy deploymentStrategy = DeploymentStrategy.BLUE_GREEN;

    /**
     * Canary traffic percentage steps (e.g., "5,25,50,100")
     */
    @Column(name = "canary_steps", length = 100)
    @Builder.Default
    private String canarySteps = "5,25,50,100";

    /**
     * Canary step interval in minutes
     */
    @Column(name = "canary_step_interval_minutes")
    @Builder.Default
    private Integer canaryStepIntervalMinutes = 10;

    /**
     * Enable automatic rollback on SLO violation
     */
    @Column(name = "auto_rollback_enabled")
    @Builder.Default
    private Boolean autoRollbackEnabled = true;

    // ============ Advanced Configuration ============

    /**
     * SLO configuration as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "slo_config")
    private Map<String, Object> sloConfig;

    /**
     * Scaling rules configuration as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scaling_rules")
    private Map<String, Object> scalingRules;

    /**
     * Cost configuration as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cost_config")
    private Map<String, Object> costConfig;

    /**
     * Deployment configuration as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deployment_config")
    private Map<String, Object> deploymentConfig;

    /**
     * Full policy configuration as JSON (legacy)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy_config")
    private Map<String, Object> policyConfig;

    // ============ Status & Audit ============

    /**
     * Policy status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.DRAFT;

    /**
     * Whether this is the active policy for the service
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    /**
     * User who created this policy
     */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    /**
     * User who approved this policy
     */
    @Column(name = "approved_by")
    private UUID approvedBy;

    /**
     * Approval timestamp
     */
    @Column(name = "approved_at")
    private Instant approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Policy status enum
     */
    public enum PolicyStatus {
        DRAFT,
        PENDING_APPROVAL,
        ACTIVE,
        DEPRECATED,
        SUPERSEDED,
        ARCHIVED
    }

    /**
     * Deployment strategy enum
     */
    public enum DeploymentStrategy {
        BLUE_GREEN,
        CANARY,
        ROLLING
    }
}


