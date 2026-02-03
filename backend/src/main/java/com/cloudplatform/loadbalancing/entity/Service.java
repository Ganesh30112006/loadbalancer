package com.cloudplatform.loadbalancing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service Entity
 * 
 * Represents a managed service (application workload) that the
 * control plane monitors and scales. A service consists of
 * one or more ASGs, target groups, and observability configuration.
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to AWS Account
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aws_account_id", nullable = false)
    private AwsAccount awsAccount;

    /**
     * Service name (unique identifier)
     */
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    /**
     * Display name (human-readable)
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * Service description
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Primary AWS region
     */
    @Column(name = "primary_region", nullable = false, length = 20)
    private String primaryRegion;

    /**
     * Secondary regions (comma-separated)
     */
    @Column(name = "secondary_regions", length = 200)
    private String secondaryRegions;

    /**
     * Current active Blueprint
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id")
    private Blueprint blueprint;

    /**
     * Current active Policy
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    /**
     * Service status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.CREATING;

    /**
     * Whether automation is enabled
     */
    @Column(name = "automation_enabled", nullable = false)
    @Builder.Default
    private Boolean automationEnabled = true;

    /**
     * Whether scaling is enabled
     */
    @Column(name = "scaling_enabled", nullable = false)
    @Builder.Default
    private Boolean scalingEnabled = false;

    /**
     * Whether deployment is enabled
     */
    @Column(name = "deployment_enabled", nullable = false)
    @Builder.Default
    private Boolean deploymentEnabled = false;

    /**
     * Service regions (one-to-many relationship)
     */
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ServiceRegion> regions = new ArrayList<>();

    /**
     * Manual override reason (if automation disabled)
     */
    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    /**
     * Override expiry time
     */
    @Column(name = "override_expires_at")
    private Instant overrideExpiresAt;

    /**
     * Current desired capacity (for scaling)
     */
    @Column(name = "current_desired_capacity")
    private Integer currentDesiredCapacity;

    /**
     * AWS resource identifiers as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aws_resources")
    private Map<String, Object> awsResources;

    /**
     * Health status summary as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "health_status")
    private Map<String, Object> healthStatus;

    /**
     * Last health check timestamp
     */
    @Column(name = "last_health_check_at")
    private Instant lastHealthCheckAt;

    /**
     * User who created this service
     */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Service status enum
     */
    public enum ServiceStatus {
        PROVISIONING,
        CREATING,
        ACTIVE,
        DEGRADED,
        SCALING,
        DEPLOYING,
        MAINTENANCE,
        SUSPENDED,
        DELETED
    }
}


