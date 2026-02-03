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
 * Application Blueprint Entity
 * 
 * Defines the approved configuration for EC2 instances including
 * AMI, Launch Template settings, and governance metadata.
 * All EC2 instances must be launched from approved Blueprints.
 */
@Entity
@Table(name = "blueprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blueprint {

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
     * Blueprint name
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Blueprint description
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Version number (incremented on updates)
     */
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * Amazon Machine Image ID
     */
    @Column(name = "ami_id", nullable = false, length = 30)
    private String amiId;

    /**
     * AWS Region where AMI exists
     */
    @Column(name = "ami_region", nullable = false, length = 20)
    private String amiRegion;

    /**
     * EC2 Instance Type
     */
    @Column(name = "instance_type", nullable = false, length = 30)
    private String instanceType;

    /**
     * IAM Instance Profile ARN
     */
    @Column(name = "instance_profile_arn", length = 255)
    private String instanceProfileArn;

    /**
     * Security Group IDs (comma-separated)
     */
    @Column(name = "security_group_ids", length = 500)
    private String securityGroupIds;

    /**
     * Subnet IDs for placement (comma-separated)
     */
    @Column(name = "subnet_ids", length = 500)
    private String subnetIds;

    /**
     * Key pair name for SSH access (optional)
     */
    @Column(name = "key_name", length = 100)
    private String keyName;

    /**
     * User data script (base64 encoded)
     */
    @Column(name = "user_data", columnDefinition = "TEXT")
    private String userData;

    /**
     * Required tags as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_tags")
    private Map<String, String> requiredTags;

    /**
     * Launch Template configuration as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "launch_template_config")
    private Map<String, Object> launchTemplateConfig;

    /**
     * Health check path
     */
    @Column(name = "health_check_path", length = 255)
    @Builder.Default
    private String healthCheckPath = "/health";

    /**
     * Health check interval in seconds
     */
    @Column(name = "health_check_interval_seconds")
    @Builder.Default
    private Integer healthCheckIntervalSeconds = 30;

    /**
     * Connection drain time in seconds
     */
    @Column(name = "drain_time_seconds")
    @Builder.Default
    private Integer drainTimeSeconds = 300;

    /**
     * Startup/warmup time in seconds
     */
    @Column(name = "startup_time_seconds")
    @Builder.Default
    private Integer startupTimeSeconds = 120;

    /**
     * AWS Launch Template ID (created after blueprint approval)
     */
    @Column(name = "launch_template_id", length = 50)
    private String launchTemplateId;

    /**
     * Blueprint status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BlueprintStatus status = BlueprintStatus.DRAFT;

    /**
     * User who created this blueprint
     */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    /**
     * User who approved this blueprint
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
     * Blueprint status enum
     */
    public enum BlueprintStatus {
        DRAFT,
        PENDING_APPROVAL,
        ACTIVE,
        DEPRECATED,
        ARCHIVED
    }
}


