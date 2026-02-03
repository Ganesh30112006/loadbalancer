package com.cloudplatform.loadbalancing.dto;

import com.cloudplatform.loadbalancing.entity.ScalingEvent;
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
public class ScalingEventDto {
    private UUID id;
    private UUID serviceId;
    private String serviceName;
    private String region;
    private ScalingEvent.EventType eventType;
    private ScalingEvent.EventSource eventSource;
    private ScalingEvent.EventStatus status;
    private Integer previousCapacity;
    private Integer targetCapacity;
    private Integer actualCapacity;
    private String reason;
    private String triggerMetrics;
    private String triggeredBy;
    private Boolean isManualOverride;
    private String overrideReason;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private String errorMessage;
    private Instant createdAt;
    
    public static ScalingEventDto fromEntity(ScalingEvent event) {
        return ScalingEventDto.builder()
                .id(event.getId())
                .serviceId(event.getService().getId())
                .serviceName(event.getService().getServiceName())
                .region(event.getRegion())
                .eventType(event.getEventType())
                .eventSource(event.getEventSource())
                .status(event.getStatus())
                .previousCapacity(event.getPreviousCapacity())
                .targetCapacity(event.getTargetCapacity())
                .actualCapacity(event.getActualCapacity())
                .reason(event.getReason())
                .triggerMetrics(event.getTriggerMetrics())
                .triggeredBy(event.getTriggeredBy())
                .isManualOverride(event.getIsManualOverride())
                .overrideReason(event.getOverrideReason())
                .startedAt(event.getStartedAt())
                .completedAt(event.getCompletedAt())
                .durationMs(event.getDurationMs())
                .errorMessage(event.getErrorMessage())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
