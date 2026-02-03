package com.cloudplatform.loadbalancing.dto;

import com.cloudplatform.loadbalancing.entity.DiscoveredResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveredResourceDto {
    private UUID id;
    private UUID awsAccountId;
    private DiscoveredResource.ResourceType resourceType;
    private String resourceId;
    private String resourceArn;
    private String resourceName;
    private String region;
    private DiscoveredResource.AdoptionStatus adoptionStatus;
    private String configurationJson;
    private String tagsJson;
    private Integer currentCapacity;
    private Integer minCapacity;
    private Integer maxCapacity;
    private Double cpuUtilization;
    private Double latencyP99;
    private Double errorRate;
    private UUID adoptedServiceId;
    private Instant lastSyncedAt;
    private Instant createdAt;
    
    public static DiscoveredResourceDto fromEntity(DiscoveredResource resource) {
        return DiscoveredResourceDto.builder()
                .id(resource.getId())
                .awsAccountId(resource.getAwsAccount().getId())
                .resourceType(resource.getResourceType())
                .resourceId(resource.getResourceId())
                .resourceArn(resource.getResourceArn())
                .resourceName(resource.getResourceName())
                .region(resource.getRegion())
                .adoptionStatus(resource.getAdoptionStatus())
                .configurationJson(resource.getConfigurationJson())
                .tagsJson(resource.getTagsJson())
                .currentCapacity(resource.getCurrentCapacity())
                .minCapacity(resource.getMinCapacity())
                .maxCapacity(resource.getMaxCapacity())
                .cpuUtilization(resource.getCpuUtilization())
                .latencyP99(resource.getLatencyP99())
                .errorRate(resource.getErrorRate())
                .adoptedServiceId(resource.getAdoptedService() != null ? resource.getAdoptedService().getId() : null)
                .lastSyncedAt(resource.getLastSyncedAt())
                .createdAt(resource.getCreatedAt())
                .build();
    }
}
