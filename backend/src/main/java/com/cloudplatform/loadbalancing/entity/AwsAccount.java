package com.cloudplatform.loadbalancing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * AWS Account Entity
 * 
 * Represents an onboarded AWS account with IAM Role configuration
 * for STS AssumeRole access. External ID is platform-generated
 * for confused-deputy attack prevention.
 */
@Entity
@Table(name = "aws_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AwsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * AWS Account ID (12-digit number)
     */
    @Column(name = "account_id", nullable = false, unique = true, length = 12)
    private String accountId;

    /**
     * Friendly name for the account
     */
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    /**
     * IAM Role ARN to assume for access
     * Format: arn:aws:iam::123456789012:role/RoleName
     */
    @Column(name = "role_arn", nullable = false, length = 255)
    private String roleArn;

    /**
     * Platform-generated External ID (UUID)
     * Used in STS AssumeRole for security
     */
    @Column(name = "external_id", nullable = false, unique = true, length = 64)
    private String externalId;

    /**
     * Account status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.PENDING_VALIDATION;

    /**
     * AWS Regions enabled for this account
     * Stored as comma-separated values
     */
    @Column(name = "enabled_regions", length = 500)
    private String enabledRegions;

    /**
     * Default AWS region for this account
     */
    @Column(name = "default_region", length = 20)
    private String defaultRegion;

    /**
     * Last successful credential validation
     */
    @Column(name = "last_validated_at")
    private Instant lastValidatedAt;

    /**
     * Validation error message (if any)
     */
    @Column(name = "validation_error", length = 1000)
    private String validationError;

    /**
     * User who created this account
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
     * Account status enum
     */
    public enum AccountStatus {
        PENDING_VALIDATION,
        ACTIVE,
        VALIDATION_FAILED,
        SUSPENDED,
        DELETED
    }
}
