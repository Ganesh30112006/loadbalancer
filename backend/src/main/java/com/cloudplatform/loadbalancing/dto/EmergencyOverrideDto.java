package com.cloudplatform.loadbalancing.dto;

import com.cloudplatform.loadbalancing.entity.EmergencyOverride;
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
public class EmergencyOverrideDto {
    private UUID id;
    private UUID serviceId;
    private String serviceName;
    private EmergencyOverride.OverrideType overrideType;
    private EmergencyOverride.OverrideScope scope;
    private EmergencyOverride.OverrideStatus status;
    private String reason;
    private Integer overrideCapacity;
    private Integer previousCapacity;
    private Boolean previousAutomationState;
    private String initiatedBy;
    private String approvedBy;
    private Instant expiresAt;
    private Instant liftedAt;
    private String liftedBy;
    private String liftReason;
    private boolean isActive;
    private Instant createdAt;
    
    public static EmergencyOverrideDto fromEntity(EmergencyOverride override) {
        return EmergencyOverrideDto.builder()
                .id(override.getId())
                .serviceId(override.getService() != null ? override.getService().getId() : null)
                .serviceName(override.getService() != null ? override.getService().getServiceName() : null)
                .overrideType(override.getOverrideType())
                .scope(override.getScope())
                .status(override.getStatus())
                .reason(override.getReason())
                .overrideCapacity(override.getOverrideCapacity())
                .previousCapacity(override.getPreviousCapacity())
                .previousAutomationState(override.getPreviousAutomationState())
                .initiatedBy(override.getInitiatedBy())
                .approvedBy(override.getApprovedBy())
                .expiresAt(override.getExpiresAt())
                .liftedAt(override.getLiftedAt())
                .liftedBy(override.getLiftedBy())
                .liftReason(override.getLiftReason())
                .isActive(override.isActive())
                .createdAt(override.getCreatedAt())
                .build();
    }
}
