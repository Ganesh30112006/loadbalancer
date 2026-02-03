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
 * Discovered AWS Resource from account scanning
 */
@Entity
@Table(name = "discovered_resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveredResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aws_account_id", nullable = false)
    private AwsAccount awsAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    @Column(nullable = false)
    private String resourceId;

    @Column(nullable = false)
    private String resourceArn;

    @Column(nullable = false)
    private String resourceName;

    @Column(nullable = false)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AdoptionStatus adoptionStatus = AdoptionStatus.DISCOVERED;

    @Column(columnDefinition = "TEXT")
    private String configurationJson;

    @Column(columnDefinition = "TEXT")
    private String tagsJson;

    @Column
    private Integer currentCapacity;

    @Column
    private Integer minCapacity;

    @Column
    private Integer maxCapacity;

    @Column
    private Double cpuUtilization;

    @Column
    private Double latencyP99;

    @Column
    private Double errorRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adopted_service_id")
    private Service adoptedService;

    @Column
    private Instant lastSyncedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public enum ResourceType {
        AUTO_SCALING_GROUP,
        LAUNCH_TEMPLATE,
        LAUNCH_CONFIGURATION,
        APPLICATION_LOAD_BALANCER,
        TARGET_GROUP,
        EC2_INSTANCE
    }

    public enum AdoptionStatus {
        DISCOVERED,      // Just found, not managed
        OBSERVED,        // Monitoring but not controlling
        ADOPTED,         // Linked to a service, controlled
        IGNORED          // User chose to ignore
    }
}
