package com.cloudplatform.loadbalancing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scaling_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;
    
    private String region;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventSource eventSource;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.PENDING;
    
    private Integer previousCapacity;
    
    private Integer targetCapacity;
    
    private Integer actualCapacity;
    
    private String reason;
    
    @Column(columnDefinition = "TEXT")
    private String triggerMetrics;
    
    private String triggeredBy;
    
    @Builder.Default
    private Boolean isManualOverride = false;
    
    private String overrideReason;
    
    @Column(nullable = false)
    private Instant startedAt;
    
    private Instant completedAt;
    
    private Long durationMs;
    
    private String errorMessage;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    public enum EventType {
        SCALE_OUT,
        SCALE_IN,
        CAPACITY_OVERRIDE,
        EMERGENCY_SCALE,
        MANUAL_OVERRIDE
    }
    
    public enum EventSource {
        CONTROL_LOOP,
        MANUAL,
        EMERGENCY,
        SCHEDULE,
        POLICY,
        API
    }
    
    public enum EventStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        ROLLED_BACK,
        CANCELLED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (startedAt == null) {
            startedAt = createdAt;
        }
    }
}
