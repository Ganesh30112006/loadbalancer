package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.controlloop.InfrastructureExecutor;
import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.entity.ServiceRegion;
import com.cloudplatform.loadbalancing.service.ObservabilityService;
import com.cloudplatform.loadbalancing.service.ServiceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Metrics Controller
 * 
 * Provides real-time and historical metrics for services and regions.
 */
@RestController
@RequestMapping("/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Real-time metrics and observability")
public class MetricsController {

    private final ServiceManagementService serviceManagementService;
    private final ObservabilityService observabilityService;
    private final InfrastructureExecutor infrastructureExecutor;

    @GetMapping("/services/{serviceId}")
    @Operation(summary = "Get aggregated metrics for a service")
    public ResponseEntity<ServiceMetricsResponse> getServiceMetrics(
            @PathVariable UUID serviceId) {
        
        Service service = serviceManagementService.getServiceWithRegions(serviceId);
        Map<String, ObservabilityService.RegionMetrics> regionMetrics = 
                observabilityService.collectAllRegionMetrics(service);
        ObservabilityService.ServiceMetrics aggregated = 
                observabilityService.aggregateServiceMetrics(regionMetrics);
        ObservabilityService.HealthStatus health = 
                observabilityService.computeHealthStatus(service, aggregated);

        return ResponseEntity.ok(new ServiceMetricsResponse(
                serviceId,
                service.getServiceName(),
                aggregated.getAvgCpu(),
                aggregated.getLatencyP99(),
                aggregated.getErrorRate(),
                aggregated.getTotalRequestsPerMinute(),
                aggregated.getTotalHealthyInstances(),
                aggregated.getTotalUnhealthyInstances(),
                health.getStatus(),
                health.getSloViolations(),
                regionMetrics.entrySet().stream()
                        .map(e -> mapRegionMetrics(e.getKey(), e.getValue()))
                        .toList(),
                aggregated.getAggregatedAt()
        ));
    }

    @GetMapping("/services/{serviceId}/regions/{region}")
    @Operation(summary = "Get metrics for a specific region")
    public ResponseEntity<RegionMetricsResponse> getRegionMetrics(
            @PathVariable UUID serviceId,
            @PathVariable String region) {
        
        Service service = serviceManagementService.getServiceWithRegions(serviceId);
        ServiceRegion serviceRegion = serviceManagementService.getServiceRegion(serviceId, region);
        ObservabilityService.RegionMetrics metrics = 
                observabilityService.collectRegionMetrics(service, serviceRegion);

        return ResponseEntity.ok(mapRegionMetrics(region, metrics));
    }

    @GetMapping("/services/{serviceId}/regions/{region}/asg")
    @Operation(summary = "Get ASG state for a region")
    public ResponseEntity<InfrastructureExecutor.AsgState> getAsgState(
            @PathVariable UUID serviceId,
            @PathVariable String region) {
        
        Service service = serviceManagementService.getService(serviceId);
        return ResponseEntity.ok(infrastructureExecutor.getAsgState(service, region));
    }

    @GetMapping("/services/{serviceId}/dashboard")
    @Operation(summary = "Get dashboard data for a service")
    public ResponseEntity<DashboardData> getDashboardData(@PathVariable UUID serviceId) {
        Service service = serviceManagementService.getServiceWithRegions(serviceId);
        Map<String, ObservabilityService.RegionMetrics> regionMetrics = 
                observabilityService.collectAllRegionMetrics(service);
        ObservabilityService.ServiceMetrics aggregated = 
                observabilityService.aggregateServiceMetrics(regionMetrics);
        ObservabilityService.HealthStatus health = 
                observabilityService.computeHealthStatus(service, aggregated);

        // Build region summaries
        List<RegionSummary> regionSummaries = new ArrayList<>();
        for (ServiceRegion sr : service.getRegions()) {
            ObservabilityService.RegionMetrics rm = regionMetrics.get(sr.getRegion());
            InfrastructureExecutor.AsgState asgState = infrastructureExecutor.getAsgState(service, sr.getRegion());
            
            regionSummaries.add(new RegionSummary(
                    sr.getRegion(),
                    sr.getStatus().name(),
                    sr.getTrafficWeight(),
                    asgState.desiredCapacity(),
                    asgState.totalInstances(),
                    asgState.healthyInstances(),
                    rm != null ? rm.getAvgCpu() : BigDecimal.ZERO,
                    rm != null ? rm.getLatencyP99() : 0,
                    rm != null ? rm.getErrorRate() : BigDecimal.ZERO
            ));
        }

        return ResponseEntity.ok(new DashboardData(
                serviceId,
                service.getServiceName(),
                service.getStatus().name(),
                health.getStatus(),
                service.getAutomationEnabled(),
                service.getScalingEnabled(),
                new MetricsSummary(
                        aggregated.getAvgCpu(),
                        aggregated.getLatencyP99(),
                        aggregated.getErrorRate(),
                        aggregated.getTotalRequestsPerMinute()
                ),
                new CapacitySummary(
                        aggregated.getTotalHealthyInstances(),
                        aggregated.getTotalUnhealthyInstances(),
                        service.getRegions().size()
                ),
                regionSummaries,
                health.getSloViolations(),
                Instant.now()
        ));
    }

    private RegionMetricsResponse mapRegionMetrics(String region, ObservabilityService.RegionMetrics metrics) {
        return new RegionMetricsResponse(
                region,
                metrics.getAvgCpu(),
                metrics.getAvgMemory(),
                metrics.getNetworkIn(),
                metrics.getNetworkOut(),
                metrics.getRequestCount(),
                metrics.getLatencyP50(),
                metrics.getLatencyP95(),
                metrics.getLatencyP99(),
                metrics.getErrorRate(),
                metrics.getHealthyHostCount(),
                metrics.getUnhealthyHostCount(),
                metrics.getActiveConnections(),
                metrics.getError(),
                metrics.getCollectedAt()
        );
    }

    // Response records
    public record ServiceMetricsResponse(
            UUID serviceId,
            String serviceName,
            BigDecimal avgCpu,
            int latencyP99,
            BigDecimal errorRate,
            int requestsPerMinute,
            int healthyInstances,
            int unhealthyInstances,
            String healthStatus,
            List<String> sloViolations,
            List<RegionMetricsResponse> regionMetrics,
            Instant collectedAt
    ) {}

    public record RegionMetricsResponse(
            String region,
            BigDecimal avgCpu,
            BigDecimal avgMemory,
            BigDecimal networkIn,
            BigDecimal networkOut,
            int requestCount,
            int latencyP50,
            int latencyP95,
            int latencyP99,
            BigDecimal errorRate,
            int healthyHostCount,
            int unhealthyHostCount,
            int activeConnections,
            String error,
            Instant collectedAt
    ) {}

    public record DashboardData(
            UUID serviceId,
            String serviceName,
            String serviceStatus,
            String healthStatus,
            boolean automationEnabled,
            boolean scalingEnabled,
            MetricsSummary metrics,
            CapacitySummary capacity,
            List<RegionSummary> regions,
            List<String> sloViolations,
            Instant refreshedAt
    ) {}

    public record MetricsSummary(
            BigDecimal avgCpu,
            int latencyP99,
            BigDecimal errorRate,
            int requestsPerMinute
    ) {}

    public record CapacitySummary(
            int healthyInstances,
            int unhealthyInstances,
            int regionCount
    ) {}

    public record RegionSummary(
            String region,
            String status,
            int trafficWeight,
            int desiredCapacity,
            int totalInstances,
            int healthyInstances,
            BigDecimal avgCpu,
            int latencyP99,
            BigDecimal errorRate
    ) {}
}

