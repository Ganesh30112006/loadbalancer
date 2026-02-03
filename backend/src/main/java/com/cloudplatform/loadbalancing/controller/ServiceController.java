package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.dto.ServiceDto;
import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.entity.ServiceRegion;
import com.cloudplatform.loadbalancing.service.ServiceManagementService;
import com.cloudplatform.loadbalancing.service.ServiceManagementService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service Controller
 * 
 * Manages services under platform control, including
 * automation settings and multi-region configuration.
 */
@RestController
@RequestMapping("/v1/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Managed service operations")
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    @PostMapping
    @Operation(summary = "Create a new managed service")
    public ResponseEntity<ServiceDto.Response> createService(
            @Valid @RequestBody ServiceDto.CreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        CreateServiceRequest createRequest = new CreateServiceRequest(
                request.getServiceName(),
                request.getDisplayName(),
                request.getDescription(),
                request.getAwsAccountId(),
                request.getBlueprintId(),
                request.getPolicyId(),
                request.getPrimaryRegion(),
                request.getRegions()
        );

        Service service = serviceManagementService.createService(createRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(service));
    }

    @GetMapping
    @Operation(summary = "Get all services")
    public ResponseEntity<List<ServiceDto.Response>> getAllServices() {
        List<Service> services = serviceManagementService.getAllServices();
        return ResponseEntity.ok(services.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{serviceId}")
    @Operation(summary = "Get service by ID")
    public ResponseEntity<ServiceDto.Response> getService(@PathVariable UUID serviceId) {
        Service service = serviceManagementService.getServiceWithRegions(serviceId);
        return ResponseEntity.ok(mapToResponse(service));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get all services for an account")
    public ResponseEntity<List<ServiceDto.Response>> getServicesByAccount(
            @PathVariable UUID accountId) {
        List<Service> services = serviceManagementService.getServicesByAccount(accountId);
        return ResponseEntity.ok(services.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{serviceId}/health")
    @Operation(summary = "Get service health summary")
    public ResponseEntity<ServiceDto.HealthSummary> getServiceHealth(@PathVariable UUID serviceId) {
        Service service = serviceManagementService.getServiceWithRegions(serviceId);
        return ResponseEntity.ok(buildHealthSummary(service));
    }

    @PostMapping("/{serviceId}/automation")
    @Operation(summary = "Configure automation settings")
    public ResponseEntity<ServiceDto.Response> configureAutomation(
            @PathVariable UUID serviceId,
            @RequestBody AutomationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        AutomationSettings settings = new AutomationSettings(
                request.automationEnabled(),
                request.scalingEnabled(),
                request.deploymentEnabled()
        );

        Service service = serviceManagementService.enableAutomation(serviceId, settings, userId);
        return ResponseEntity.ok(mapToResponse(service));
    }

    @PostMapping("/{serviceId}/override")
    @Operation(summary = "Apply manual override")
    public ResponseEntity<ServiceDto.Response> applyOverride(
            @PathVariable UUID serviceId,
            @Valid @RequestBody ServiceDto.OverrideRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        OverrideRequest override = new OverrideRequest(
                request.getDisableScaling(),
                request.getDisableDeployment(),
                request.getDisableAutomation(),
                request.getOverrideDurationMinutes(),
                request.getReason()
        );

        Service service = serviceManagementService.applyOverride(serviceId, override, userId);
        return ResponseEntity.ok(mapToResponse(service));
    }

    @PutMapping("/{serviceId}/traffic-weights")
    @Operation(summary = "Update traffic weights across regions")
    public ResponseEntity<Void> updateTrafficWeights(
            @PathVariable UUID serviceId,
            @RequestBody Map<String, Integer> regionWeights,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        serviceManagementService.updateTrafficWeights(serviceId, regionWeights, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{serviceId}/regions")
    @Operation(summary = "Get all regions for a service")
    public ResponseEntity<List<ServiceDto.RegionHealthSummary>> getServiceRegions(
            @PathVariable UUID serviceId) {
        List<ServiceRegion> regions = serviceManagementService.getServiceRegions(serviceId);
        return ResponseEntity.ok(regions.stream()
                .map(this::mapRegionToSummary)
                .collect(Collectors.toList()));
    }

    private ServiceDto.Response mapToResponse(Service service) {
        return ServiceDto.Response.builder()
                .id(service.getId())
                .serviceName(service.getServiceName())
                .displayName(service.getDisplayName())
                .description(service.getDescription())
                .awsAccountId(service.getAwsAccount().getId())
                .blueprintId(service.getBlueprint().getId())
                .policyId(service.getPolicy().getId())
                .primaryRegion(service.getPrimaryRegion())
                .status(service.getStatus().name())
                .automationEnabled(service.getAutomationEnabled())
                .scalingEnabled(service.getScalingEnabled())
                .deploymentEnabled(service.getDeploymentEnabled())
                .healthStatusData(service.getHealthStatus())
                .lastHealthCheckAt(service.getLastHealthCheckAt())
                .regions(service.getRegions() != null ? 
                        service.getRegions().stream()
                                .map(this::mapRegionToSummary)
                                .collect(Collectors.toList()) : List.of())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }

    private ServiceDto.RegionHealthSummary mapRegionToSummary(ServiceRegion region) {
        return ServiceDto.RegionHealthSummary.builder()
                .region(region.getRegion())
                .status(region.getStatus().name())
                .asgName(region.getAsgName())
                .desiredCapacity(region.getDesiredCapacity())
                .runningInstances(region.getRunningInstances())
                .trafficWeight(region.getTrafficWeight())
                .lastMetricsAt(region.getLastMetricsAt())
                .build();
    }

    private ServiceDto.HealthSummary buildHealthSummary(Service service) {
        Map<String, Object> healthStatus = service.getHealthStatus();
        String status = healthStatus != null ? 
                (String) healthStatus.getOrDefault("status", "UNKNOWN") : "UNKNOWN";
        
        @SuppressWarnings("unchecked")
        List<String> violations = healthStatus != null ?
                (List<String>) healthStatus.get("violations") : List.of();

        int totalInstances = 0;
        int healthyInstances = 0;
        
        for (ServiceRegion region : service.getRegions()) {
            if (region.getRunningInstances() != null) {
                totalInstances += region.getRunningInstances();
            }
            if (region.getDesiredCapacity() != null) {
                healthyInstances += region.getDesiredCapacity();
            }
        }

        return ServiceDto.HealthSummary.builder()
                .serviceId(service.getId())
                .serviceName(service.getServiceName())
                .overallStatus(status)
                .sloViolations(violations)
                .totalInstances(totalInstances)
                .healthyInstances(healthyInstances)
                .regionHealth(service.getRegions().stream()
                        .map(this::mapRegionToSummary)
                        .collect(Collectors.toList()))
                .lastCheckedAt(service.getLastHealthCheckAt())
                .build();
    }

    // Request record
    public record AutomationRequest(
            boolean automationEnabled,
            boolean scalingEnabled,
            boolean deploymentEnabled
    ) {}
}

