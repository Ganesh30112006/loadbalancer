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
 * Service Region Entity
 * 
 * Tracks regional deployment state including ASGs, ALBs,
 * and current capacity for multi-region services.
 */
@Entity
@Table(name = "service_regions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"service_id", "region"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRegion {

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
     * AWS Region
     */
    @Column(name = "region", nullable = false, length = 20)
    private String region;

    /**
     * Whether this is the primary region
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Region status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RegionStatus status = RegionStatus.PROVISIONING;

    // ============ ASG Configuration ============

    /**
     * Primary Auto Scaling Group name
     */
    @Column(name = "asg_name", length = 100)
    private String asgName;

    /**
     * Secondary ASG name (for blue/green)
     */
    @Column(name = "secondary_asg_name", length = 100)
    private String secondaryAsgName;

    /**
     * Current desired capacity
     */
    @Column(name = "desired_capacity")
    @Builder.Default
    private Integer desiredCapacity = 0;

    /**
     * Current running instance count
     */
    @Column(name = "running_instances")
    @Builder.Default
    private Integer runningInstances = 0;

    /**
     * Current healthy instance count
     */
    @Column(name = "healthy_instances")
    @Builder.Default
    private Integer healthyInstances = 0;

    // ============ ALB Configuration ============

    /**
     * Application Load Balancer ARN
     */
    @Column(name = "alb_arn", length = 255)
    private String albArn;

    /**
     * ALB DNS name
     */
    @Column(name = "alb_dns_name", length = 255)
    private String albDnsName;

    /**
     * Primary target group ARN
     */
    @Column(name = "target_group_arn", length = 255)
    private String targetGroupArn;

    /**
     * Secondary target group ARN (for blue/green)
     */
    @Column(name = "secondary_target_group_arn", length = 255)
    private String secondaryTargetGroupArn;

    // ============ Traffic Configuration ============

    /**
     * Traffic weight (0-100, for Route 53)
     */
    @Column(name = "traffic_weight")
    @Builder.Default
    private Integer trafficWeight = 100;

    /**
     * Route 53 health check ID
     */
    @Column(name = "health_check_id", length = 50)
    private String healthCheckId;

    // ============ Health & Metrics ============

    /**
     * Overall health score (0.0 - 1.0)
     */
    @Column(name = "health_score", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal healthScore = BigDecimal.ONE;

    /**
     * Average CPU utilization (percentage)
     */
    @Column(name = "avg_cpu", precision = 5, scale = 2)
    private BigDecimal avgCpu;

    /**
     * Average latency P99 in ms
     */
    @Column(name = "avg_latency_p99")
    private Integer avgLatencyP99;

    /**
     * Error rate (percentage)
     */
    @Column(name = "error_rate", precision = 5, scale = 2)
    private BigDecimal errorRate;

    /**
     * Requests per second
     */
    @Column(name = "requests_per_second")
    private Integer requestsPerSecond;

    /**
     * Last metrics update
     */
    @Column(name = "metrics_updated_at")
    private Instant metricsUpdatedAt;

    /**
     * Detailed AWS resources
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aws_resources")
    private Map<String, Object> awsResources;

    /**
     * Last scaling action time
     */
    @Column(name = "last_scale_action_at")
    private Instant lastScaleActionAt;

    /**
     * Last scale action type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_scale_action_type", length = 20)
    private ScaleActionType lastScaleActionType;

    /**
     * Last metrics collection timestamp
     */
    @Column(name = "last_metrics_at")
    private Instant lastMetricsAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Region status enum
     */
    public enum RegionStatus {
        PROVISIONING,
        ACTIVE,
        DEGRADED,
        DRAINING,
        FAILING_OVER,
        FAILED,
        SUSPENDED
    }

    /**
     * Scale action type enum
     */
    public enum ScaleActionType {
        SCALE_OUT,
        SCALE_IN,
        NO_CHANGE
    }
}


