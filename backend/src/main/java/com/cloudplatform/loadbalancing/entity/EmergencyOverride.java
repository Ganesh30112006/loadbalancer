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
 * Emergency Override record for tracking manual interventions
 */
@Entity
@Table(name = "emergency_overrides")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OverrideType overrideType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OverrideScope scope = OverrideScope.SERVICE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OverrideStatus status = OverrideStatus.ACTIVE;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    // For capacity override
    @Column
    private Integer overrideCapacity;

    @Column
    private Integer previousCapacity;

    // For automation pause
    @Column
    private Boolean previousAutomationState;

    // User who initiated
    @Column(nullable = false)
    private String initiatedBy;

    @Column
    private String approvedBy;

    // Duration
    @Column
    private Instant expiresAt;

    @Column
    private Instant liftedAt;

    @Column
    private String liftedBy;

    @Column
    private String liftReason;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public enum OverrideType {
        PAUSE_AUTOMATION,
        CAPACITY_OVERRIDE,
        FORCED_ROLLBACK,
        TRAFFIC_SHIFT,
        EMERGENCY_SCALE
    }

    public enum OverrideScope {
        GLOBAL,         // All services
        SERVICE,        // Single service
        REGION          // Specific region
    }

    public enum OverrideStatus {
        ACTIVE,
        EXPIRED,
        LIFTED,
        SUPERSEDED
    }

    public boolean isActive() {
        if (status != OverrideStatus.ACTIVE) {
            return false;
        }
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }
}
