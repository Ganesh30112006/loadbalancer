package com.cloudplatform.loadbalancing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Log Entity
 * 
 * Records all control plane decisions and actions for compliance,
 * debugging, and operational visibility.
 */
@Entity
@Table(name = "audit_logs",
        indexes = {
            @Index(name = "idx_audit_service_time", columnList = "service_id, created_at DESC"),
            @Index(name = "idx_audit_action_type", columnList = "action_type, created_at DESC"),
            @Index(name = "idx_audit_cycle_id", columnList = "cycle_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to Service (optional, for service-level events)
     */
    @Column(name = "service_id")
    private UUID serviceId;

    /**
     * Reference to AWS Account
     */
    @Column(name = "aws_account_id")
    private UUID awsAccountId;

    /**
     * Control loop cycle ID
     */
    @Column(name = "cycle_id", length = 50)
    private String cycleId;

    /**
     * Action type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    /**
     * Action category
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_category", nullable = false, length = 30)
    private ActionCategory actionCategory;

    /**
     * Action description
     */
    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    /**
     * Action status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActionStatus status;

    /**
     * Observation data at time of decision
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "observation_data")
    private Map<String, Object> observationData;

    /**
     * Analysis result
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_result")
    private Map<String, Object> analysisResult;

    /**
     * Decision details
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "decision_details")
    private Map<String, Object> decisionDetails;

    /**
     * Execution details (AWS API calls, responses)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_details")
    private Map<String, Object> executionDetails;

    /**
     * Error details if action failed
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /**
     * User who triggered the action (null for automated actions)
     */
    @Column(name = "triggered_by")
    private UUID triggeredBy;

    /**
     * Whether action was manual override
     */
    @Column(name = "is_manual_override", nullable = false)
    @Builder.Default
    private Boolean isManualOverride = false;

    /**
     * Platform instance ID that executed the action
     */
    @Column(name = "platform_instance_id", length = 100)
    private String platformInstanceId;

    /**
     * Duration of action in milliseconds
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Action type enum
     */
    public enum ActionType {
        // Scaling Actions
        SCALE_OUT,
        SCALE_IN,
        CAPACITY_UPDATE,
        
        // Deployment Actions
        DEPLOYMENT_START,
        DEPLOYMENT_PROGRESS,
        DEPLOYMENT_COMPLETE,
        DEPLOYMENT_ROLLBACK,
        
        // Traffic Actions
        TRAFFIC_SHIFT,
        FAILOVER_START,
        FAILOVER_COMPLETE,
        
        // Health Actions
        HEALTH_CHECK,
        HEALTH_DEGRADATION_DETECTED,
        HEALTH_RECOVERED,
        
        // Configuration Actions
        POLICY_APPLIED,
        BLUEPRINT_APPLIED,
        OVERRIDE_ENABLED,
        OVERRIDE_DISABLED,
        
        // Account Actions
        ACCOUNT_ONBOARDED,
        ACCOUNT_VALIDATED,
        ACCOUNT_SUSPENDED,
        
        // Control Loop Actions
        CONTROL_LOOP_CYCLE,
        LEADER_ELECTED,
        LEADER_LOST
    }

    /**
     * Action category enum
     */
    public enum ActionCategory {
        SCALING,
        DEPLOYMENT,
        TRAFFIC,
        HEALTH,
        CONFIGURATION,
        ACCOUNT,
        CONTROL_LOOP
    }

    /**
     * Action status enum
     */
    public enum ActionStatus {
        INITIATED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        SKIPPED,
        ROLLED_BACK
    }
}
