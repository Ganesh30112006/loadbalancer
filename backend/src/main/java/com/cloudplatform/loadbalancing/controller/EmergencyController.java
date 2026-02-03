package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.dto.EmergencyOverrideDto;
import com.cloudplatform.loadbalancing.dto.ScalingEventDto;
import com.cloudplatform.loadbalancing.service.EmergencyControlService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/emergency")
@RequiredArgsConstructor
public class EmergencyController {

    private final EmergencyControlService emergencyService;

    @PostMapping("/services/{serviceId}/pause")
    public ResponseEntity<EmergencyOverrideDto> pauseServiceAutomation(
            @PathVariable UUID serviceId,
            @RequestBody PauseRequest request) {
        return ResponseEntity.ok(
                emergencyService.pauseServiceAutomation(serviceId, request.getReason(), request.getInitiatedBy())
        );
    }

    @PostMapping("/global/pause")
    public ResponseEntity<EmergencyOverrideDto> pauseGlobalAutomation(@RequestBody PauseRequest request) {
        return ResponseEntity.ok(
                emergencyService.pauseGlobalAutomation(request.getReason(), request.getInitiatedBy())
        );
    }

    @PostMapping("/services/{serviceId}/capacity-override")
    public ResponseEntity<EmergencyOverrideDto> setCapacityOverride(
            @PathVariable UUID serviceId,
            @RequestBody CapacityOverrideRequest request) {
        return ResponseEntity.ok(
                emergencyService.setCapacityOverride(
                        serviceId, 
                        request.getTargetCapacity(), 
                        request.getReason(), 
                        request.getInitiatedBy()
                )
        );
    }

    @PostMapping("/overrides/{overrideId}/lift")
    public ResponseEntity<EmergencyOverrideDto> liftOverride(
            @PathVariable UUID overrideId,
            @RequestBody LiftRequest request) {
        return ResponseEntity.ok(
                emergencyService.liftOverride(overrideId, request.getReason(), request.getLiftedBy())
        );
    }

    @PostMapping("/services/{serviceId}/resume")
    public ResponseEntity<Void> resumeServiceAutomation(
            @PathVariable UUID serviceId,
            @RequestBody Map<String, String> request) {
        emergencyService.resumeServiceAutomation(serviceId, request.get("resumedBy"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/overrides/active")
    public ResponseEntity<List<EmergencyOverrideDto>> getActiveOverrides() {
        return ResponseEntity.ok(emergencyService.getActiveOverrides());
    }

    @GetMapping("/services/{serviceId}/overrides")
    public ResponseEntity<List<EmergencyOverrideDto>> getServiceOverrides(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(emergencyService.getActiveOverridesForService(serviceId));
    }

    @GetMapping("/services/{serviceId}/blocked")
    public ResponseEntity<Map<String, Boolean>> isAutomationBlocked(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(Map.of("blocked", emergencyService.isAutomationBlocked(serviceId)));
    }

    @GetMapping("/services/{serviceId}/scaling-events")
    public ResponseEntity<List<ScalingEventDto>> getScalingEvents(
            @PathVariable UUID serviceId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(emergencyService.getScalingEvents(serviceId, limit));
    }

    @Data
    public static class PauseRequest {
        private String reason;
        private String initiatedBy;
    }

    @Data
    public static class CapacityOverrideRequest {
        private Integer targetCapacity;
        private String reason;
        private String initiatedBy;
    }

    @Data
    public static class LiftRequest {
        private String reason;
        private String liftedBy;
    }
}

