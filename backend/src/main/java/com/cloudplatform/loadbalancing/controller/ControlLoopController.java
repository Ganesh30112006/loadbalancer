package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.controlloop.*;
import com.cloudplatform.loadbalancing.dto.ControlLoopDto;
import com.cloudplatform.loadbalancing.entity.AuditLog;
import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.repository.AuditLogRepository;
import com.cloudplatform.loadbalancing.service.ObservabilityService;
import com.cloudplatform.loadbalancing.service.ServiceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Control Loop Controller
 * 
 * Provides visibility into the control loop engine,
 * including current status, recent decisions, and metrics.
 */
@RestController
@RequestMapping("/v1/control-loop")
@RequiredArgsConstructor
@Tag(name = "Control Loop", description = "Control loop monitoring and management")
public class ControlLoopController {

    private final ServiceManagementService serviceManagementService;
    private final ObservabilityService observabilityService;
    private final LeaderElectionService leaderElectionService;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/status")
    @Operation(summary = "Get control loop status")
    public ResponseEntity<ControlLoopDto.ControlLoopStatus> getStatus() {
        LeaderElectionService.LeadershipStatus leaderStatus = leaderElectionService.getStatus();
        List<Service> activeServices = serviceManagementService.getServicesForControlLoop();

        return ResponseEntity.ok(ControlLoopDto.ControlLoopStatus.builder()
                .isLeader(leaderStatus.isLeader())
                .instanceId(leaderStatus.instanceId())
                .currentLeader(leaderStatus.currentLeader())
                .activeServicesCount(activeServices.size())
                .lastCheckedAt(leaderStatus.checkedAt())
                .build());
    }

    @GetMapping("/services/{serviceId}/metrics")
    @Operation(summary = "Get current metrics for a service")
    public ResponseEntity<ControlLoopDto.ObservationData> getServiceMetrics(
            @PathVariable UUID serviceId) {
        
        Service service = serviceManagementService.getServiceWithRegions(serviceId);
        Map<String, ObservabilityService.RegionMetrics> regionMetrics = 
                observabilityService.collectAllRegionMetrics(service);
        ObservabilityService.ServiceMetrics aggregated = 
                observabilityService.aggregateServiceMetrics(regionMetrics);

        return ResponseEntity.ok(ControlLoopDto.ObservationData.builder()
                .avgCpu(aggregated.getAvgCpu())
                .latencyP99(aggregated.getLatencyP99())
                .errorRate(aggregated.getErrorRate())
                .totalRequestsPerMinute(aggregated.getTotalRequestsPerMinute())
                .healthyInstances(aggregated.getTotalHealthyInstances())
                .unhealthyInstances(aggregated.getTotalUnhealthyInstances())
                .collectedAt(aggregated.getAggregatedAt())
                .build());
    }

    @GetMapping("/services/{serviceId}/decisions")
    @Operation(summary = "Get recent control loop decisions for a service")
    public ResponseEntity<List<ControlLoopDto.ExecutionResult>> getRecentDecisions(
            @PathVariable UUID serviceId,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<AuditLog> recentLogs = auditLogRepository.findRecentByServiceId(serviceId, limit);
        
        return ResponseEntity.ok(recentLogs.stream()
                .filter(log -> log.getActionType() == AuditLog.ActionType.CONTROL_LOOP_CYCLE ||
                        log.getActionCategory() == AuditLog.ActionCategory.SCALING)
                .map(this::mapToExecutionResult)
                .collect(Collectors.toList()));
    }

    @GetMapping("/services/{serviceId}/audit")
    @Operation(summary = "Get audit log for a service")
    public ResponseEntity<List<AuditLogEntry>> getAuditLog(
            @PathVariable UUID serviceId,
            @RequestParam(required = false) Instant since,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<AuditLog> logs;
        if (since != null) {
            logs = auditLogRepository.findByServiceIdAndCreatedAtAfter(serviceId, since);
        } else {
            logs = auditLogRepository.findRecentByServiceId(serviceId, limit);
        }

        return ResponseEntity.ok(logs.stream()
                .map(this::mapToAuditEntry)
                .collect(Collectors.toList()));
    }

    @GetMapping("/leadership")
    @Operation(summary = "Get leadership status")
    public ResponseEntity<LeaderElectionService.LeadershipStatus> getLeadershipStatus() {
        return ResponseEntity.ok(leaderElectionService.getStatus());
    }

    @PostMapping("/automation")
    @Operation(summary = "Toggle control loop automation")
    public ResponseEntity<Map<String, Object>> setAutomation(@RequestBody Map<String, Boolean> request) {
        boolean enabled = request.getOrDefault("enabled", true);
        // In a real implementation, this would toggle the control loop's automation mode
        // For now, we return the requested state
        return ResponseEntity.ok(Map.of(
                "automationEnabled", enabled,
                "message", enabled ? "Automation enabled" : "Automation disabled",
                "updatedAt", Instant.now()
        ));
    }

    private ControlLoopDto.ExecutionResult mapToExecutionResult(AuditLog log) {
        return ControlLoopDto.ExecutionResult.builder()
                .cycleId(log.getCycleId())
                .actionType(log.getActionType().name())
                .success(log.getStatus() == AuditLog.ActionStatus.COMPLETED)
                .description(log.getDescription())
                .observationData(log.getObservationData())
                .analysisResult(log.getAnalysisResult())
                .executionDetails(log.getExecutionDetails())
                .durationMs(log.getDurationMs())
                .executedAt(log.getCreatedAt())
                .build();
    }

    private AuditLogEntry mapToAuditEntry(AuditLog log) {
        return new AuditLogEntry(
                log.getId(),
                log.getActionType().name(),
                log.getActionCategory().name(),
                log.getDescription(),
                log.getStatus().name(),
                log.getIsManualOverride(),
                log.getTriggeredBy(),
                log.getDurationMs(),
                log.getCreatedAt()
        );
    }

    public record AuditLogEntry(
            UUID id,
            String actionType,
            String category,
            String description,
            String status,
            Boolean isManualOverride,
            UUID triggeredBy,
            Long durationMs,
            Instant createdAt
    ) {}
}

