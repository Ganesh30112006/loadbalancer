package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.aws.AwsClientProvider;
import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.entity.ServiceRegion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Observability Service
 * 
 * Collects and aggregates metrics from CloudWatch for services
 * under control. Provides the "Observe" phase of the OODA loop.
 */
@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ObservabilityService {

    private final AwsClientProvider awsClientProvider;
    // private final ServiceManagementService serviceManagementService; // Reserved for future use

    // Metric collection period
    private static final Duration METRIC_PERIOD = Duration.ofMinutes(1);
    private static final Duration LOOKBACK_DURATION = Duration.ofMinutes(5);

    /**
     * Collect metrics for a service region
     */
    public RegionMetrics collectRegionMetrics(Service service, ServiceRegion region) {
        try {
            CloudWatchClient cloudWatch = awsClientProvider.getCloudWatchClient(
                    service.getAwsAccount(), region.getRegion());

            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(LOOKBACK_DURATION);

            // Collect ASG metrics
            Map<String, BigDecimal> asgMetrics = collectAsgMetrics(
                    cloudWatch, region.getAsgName(), startTime, endTime);

            // Collect ALB metrics
            Map<String, BigDecimal> albMetrics = region.getAlbArn() != null ?
                    collectAlbMetrics(cloudWatch, region.getAlbArn(), region.getTargetGroupArn(), 
                            startTime, endTime) : Map.of();

            return RegionMetrics.builder()
                    .region(region.getRegion())
                    .collectedAt(endTime)
                    .avgCpu(asgMetrics.getOrDefault("CPUUtilization", BigDecimal.ZERO))
                    .avgMemory(asgMetrics.getOrDefault("MemoryUtilization", BigDecimal.ZERO))
                    .networkIn(asgMetrics.getOrDefault("NetworkIn", BigDecimal.ZERO))
                    .networkOut(asgMetrics.getOrDefault("NetworkOut", BigDecimal.ZERO))
                    .requestCount(albMetrics.getOrDefault("RequestCount", BigDecimal.ZERO).intValue())
                    .latencyP50(albMetrics.getOrDefault("TargetResponseTime_p50", BigDecimal.ZERO).intValue())
                    .latencyP95(albMetrics.getOrDefault("TargetResponseTime_p95", BigDecimal.ZERO).intValue())
                    .latencyP99(albMetrics.getOrDefault("TargetResponseTime_p99", BigDecimal.ZERO).intValue())
                    .errorRate(calculateErrorRate(albMetrics))
                    .healthyHostCount(albMetrics.getOrDefault("HealthyHostCount", BigDecimal.ZERO).intValue())
                    .unhealthyHostCount(albMetrics.getOrDefault("UnHealthyHostCount", BigDecimal.ZERO).intValue())
                    .activeConnections(albMetrics.getOrDefault("ActiveConnectionCount", BigDecimal.ZERO).intValue())
                    .build();

        } catch (Exception e) {
            log.error("Failed to collect metrics for region {}: {}", region.getRegion(), e.getMessage());
            return RegionMetrics.builder()
                    .region(region.getRegion())
                    .collectedAt(Instant.now())
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Collect metrics for all regions of a service
     */
    public Map<String, RegionMetrics> collectAllRegionMetrics(Service service) {
        Map<String, RegionMetrics> metrics = new HashMap<>();
        
        for (ServiceRegion region : service.getRegions()) {
            if (region.getStatus() == ServiceRegion.RegionStatus.ACTIVE || 
                region.getStatus() == ServiceRegion.RegionStatus.DEGRADED) {
                metrics.put(region.getRegion(), collectRegionMetrics(service, region));
            }
        }
        
        return metrics;
    }

    /**
     * Calculate aggregate service-level metrics
     */
    public ServiceMetrics aggregateServiceMetrics(Map<String, RegionMetrics> regionMetrics) {
        if (regionMetrics.isEmpty()) {
            return ServiceMetrics.builder()
                    .aggregatedAt(Instant.now())
                    .build();
        }

        // Weight metrics by request count for latency/error rate
        BigDecimal totalRequests = BigDecimal.ZERO;
        BigDecimal weightedLatencyP99 = BigDecimal.ZERO;
        BigDecimal weightedErrorRate = BigDecimal.ZERO;
        BigDecimal totalCpu = BigDecimal.ZERO;
        int totalHealthyHosts = 0;
        int totalUnhealthyHosts = 0;
        int regionCount = 0;

        for (RegionMetrics rm : regionMetrics.values()) {
            if (rm.error != null) continue;
            
            BigDecimal requests = BigDecimal.valueOf(rm.requestCount);
            totalRequests = totalRequests.add(requests);
            weightedLatencyP99 = weightedLatencyP99.add(
                    BigDecimal.valueOf(rm.latencyP99).multiply(requests));
            weightedErrorRate = weightedErrorRate.add(
                    rm.errorRate.multiply(requests));
            totalCpu = totalCpu.add(rm.avgCpu);
            totalHealthyHosts += rm.healthyHostCount;
            totalUnhealthyHosts += rm.unhealthyHostCount;
            regionCount++;
        }

        BigDecimal avgLatencyP99 = totalRequests.compareTo(BigDecimal.ZERO) > 0 ?
                weightedLatencyP99.divide(totalRequests, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal avgErrorRate = totalRequests.compareTo(BigDecimal.ZERO) > 0 ?
                weightedErrorRate.divide(totalRequests, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal avgCpu = regionCount > 0 ?
                totalCpu.divide(BigDecimal.valueOf(regionCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return ServiceMetrics.builder()
                .aggregatedAt(Instant.now())
                .avgCpu(avgCpu)
                .latencyP99(avgLatencyP99.intValue())
                .errorRate(avgErrorRate)
                .totalRequestsPerMinute(totalRequests.intValue())
                .totalHealthyInstances(totalHealthyHosts)
                .totalUnhealthyInstances(totalUnhealthyHosts)
                .regionMetrics(regionMetrics)
                .build();
    }

    /**
     * Compute overall health status
     */
    public HealthStatus computeHealthStatus(Service service, ServiceMetrics metrics) {
        Map<String, Object> sloConfig = service.getPolicy().getSloConfig();
        
        // Extract SLO thresholds with defaults
        int latencyP99Target = getIntOrDefault(sloConfig, "latencyP99TargetMs", 500);
        BigDecimal maxErrorRate = getDecimalOrDefault(sloConfig, "maxErrorRate", new BigDecimal("0.01"));
        int minHealthyInstances = getIntOrDefault(sloConfig, "minHealthyInstances", 2);

        List<String> violations = new ArrayList<>();
        boolean healthy = true;

        // Check latency SLO
        if (metrics.latencyP99 > latencyP99Target) {
            violations.add(String.format("Latency P99 %dms exceeds target %dms", 
                    metrics.latencyP99, latencyP99Target));
            healthy = false;
        }

        // Check error rate SLO
        if (metrics.errorRate.compareTo(maxErrorRate) > 0) {
            violations.add(String.format("Error rate %.2f%% exceeds target %.2f%%",
                    metrics.errorRate.multiply(BigDecimal.valueOf(100)),
                    maxErrorRate.multiply(BigDecimal.valueOf(100))));
            healthy = false;
        }

        // Check minimum healthy instances
        if (metrics.totalHealthyInstances < minHealthyInstances) {
            violations.add(String.format("Healthy instances %d below minimum %d",
                    metrics.totalHealthyInstances, minHealthyInstances));
            healthy = false;
        }

        return HealthStatus.builder()
                .healthy(healthy)
                .status(healthy ? "HEALTHY" : (violations.size() > 2 ? "CRITICAL" : "DEGRADED"))
                .sloViolations(violations)
                .healthyInstanceCount(metrics.totalHealthyInstances)
                .unhealthyInstanceCount(metrics.totalUnhealthyInstances)
                .lastCheckedAt(Instant.now())
                .build();
    }

    private Map<String, BigDecimal> collectAsgMetrics(
            CloudWatchClient cloudWatch, String asgName, Instant startTime, Instant endTime) {
        
        if (asgName == null || asgName.isBlank()) {
            return Map.of();
        }

        Map<String, BigDecimal> metrics = new HashMap<>();
        
        List<String> metricNames = List.of("CPUUtilization", "NetworkIn", "NetworkOut");
        
        for (String metricName : metricNames) {
            try {
                GetMetricStatisticsResponse response = cloudWatch.getMetricStatistics(
                        GetMetricStatisticsRequest.builder()
                                .namespace("AWS/EC2")
                                .metricName(metricName)
                                .dimensions(Dimension.builder()
                                        .name("AutoScalingGroupName")
                                        .value(asgName)
                                        .build())
                                .startTime(startTime)
                                .endTime(endTime)
                                .period((int) METRIC_PERIOD.getSeconds())
                                .statistics(Statistic.AVERAGE)
                                .build()
                );

                if (!response.datapoints().isEmpty()) {
                    double avg = response.datapoints().stream()
                            .mapToDouble(Datapoint::average)
                            .average()
                            .orElse(0.0);
                    metrics.put(metricName, BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
                }
            } catch (Exception e) {
                log.debug("Failed to get metric {}: {}", metricName, e.getMessage());
            }
        }

        return metrics;
    }

    private Map<String, BigDecimal> collectAlbMetrics(
            CloudWatchClient cloudWatch, String albArn, String targetGroupArn,
            Instant startTime, Instant endTime) {

        Map<String, BigDecimal> metrics = new HashMap<>();
        
        // Extract ALB name from ARN
        String albName = extractAlbNameFromArn(albArn);
        String tgName = extractTargetGroupNameFromArn(targetGroupArn);

        if (albName == null) return metrics;

        // ALB metrics
        List<MetricDataQuery> queries = new ArrayList<>();
        queries.add(buildAlbMetricQuery("RequestCount", albName, "rc"));
        queries.add(buildAlbMetricQuery("HTTPCode_Target_5XX_Count", albName, "5xx"));
        queries.add(buildAlbMetricQuery("HTTPCode_Target_4XX_Count", albName, "4xx"));
        queries.add(buildAlbMetricQuery("TargetResponseTime", albName, "trt"));
        queries.add(buildAlbMetricQuery("ActiveConnectionCount", albName, "acc"));

        // Target group metrics
        if (tgName != null) {
            queries.add(buildTargetGroupMetricQuery("HealthyHostCount", albName, tgName, "hhc"));
            queries.add(buildTargetGroupMetricQuery("UnHealthyHostCount", albName, tgName, "uhc"));
        }

        try {
            GetMetricDataResponse response = cloudWatch.getMetricData(
                    GetMetricDataRequest.builder()
                            .metricDataQueries(queries)
                            .startTime(startTime)
                            .endTime(endTime)
                            .build()
            );

            for (MetricDataResult result : response.metricDataResults()) {
                if (!result.values().isEmpty()) {
                    double avg = result.values().stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);
                    
                    String metricName = switch (result.id()) {
                        case "rc" -> "RequestCount";
                        case "5xx" -> "HTTPCode_5XX";
                        case "4xx" -> "HTTPCode_4XX";
                        case "trt" -> "TargetResponseTime_p50";
                        case "acc" -> "ActiveConnectionCount";
                        case "hhc" -> "HealthyHostCount";
                        case "uhc" -> "UnHealthyHostCount";
                        default -> result.id();
                    };
                    
                    metrics.put(metricName, BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get ALB metrics: {}", e.getMessage());
        }

        return metrics;
    }

    private MetricDataQuery buildAlbMetricQuery(String metricName, String albName, String id) {
        return MetricDataQuery.builder()
                .id(id)
                .metricStat(MetricStat.builder()
                        .metric(Metric.builder()
                                .namespace("AWS/ApplicationELB")
                                .metricName(metricName)
                                .dimensions(Dimension.builder()
                                        .name("LoadBalancer")
                                        .value(albName)
                                        .build())
                                .build())
                        .period((int) METRIC_PERIOD.getSeconds())
                        .stat("Average")
                        .build())
                .build();
    }

    private MetricDataQuery buildTargetGroupMetricQuery(String metricName, String albName, 
            String targetGroupName, String id) {
        return MetricDataQuery.builder()
                .id(id)
                .metricStat(MetricStat.builder()
                        .metric(Metric.builder()
                                .namespace("AWS/ApplicationELB")
                                .metricName(metricName)
                                .dimensions(
                                        Dimension.builder()
                                                .name("LoadBalancer")
                                                .value(albName)
                                                .build(),
                                        Dimension.builder()
                                                .name("TargetGroup")
                                                .value(targetGroupName)
                                                .build()
                                )
                                .build())
                        .period((int) METRIC_PERIOD.getSeconds())
                        .stat("Average")
                        .build())
                .build();
    }

    private BigDecimal calculateErrorRate(Map<String, BigDecimal> metrics) {
        BigDecimal requests = metrics.getOrDefault("RequestCount", BigDecimal.ZERO);
        if (requests.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal errors5xx = metrics.getOrDefault("HTTPCode_5XX", BigDecimal.ZERO);
        return errors5xx.divide(requests, 4, RoundingMode.HALF_UP);
    }

    private String extractAlbNameFromArn(String arn) {
        if (arn == null) return null;
        // arn:aws:elasticloadbalancing:region:account-id:loadbalancer/app/name/id
        try {
            String[] parts = arn.split(":");
            String resource = parts[parts.length - 1];
            return resource.substring(resource.indexOf("/") + 1);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractTargetGroupNameFromArn(String arn) {
        if (arn == null) return null;
        // arn:aws:elasticloadbalancing:region:account-id:targetgroup/name/id
        try {
            String[] parts = arn.split(":");
            String resource = parts[parts.length - 1];
            return resource.substring(resource.indexOf("/") + 1);
        } catch (Exception e) {
            return null;
        }
    }

    private int getIntOrDefault(Map<String, Object> map, String key, int defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        return defaultValue;
    }

    private BigDecimal getDecimalOrDefault(Map<String, Object> map, String key, BigDecimal defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object value = map.get(key);
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return defaultValue;
    }

    // Data classes
    @lombok.Builder
    @lombok.Data
    public static class RegionMetrics {
        private String region;
        private Instant collectedAt;
        private BigDecimal avgCpu;
        private BigDecimal avgMemory;
        private BigDecimal networkIn;
        private BigDecimal networkOut;
        private int requestCount;
        private int latencyP50;
        private int latencyP95;
        private int latencyP99;
        private BigDecimal errorRate;
        private int healthyHostCount;
        private int unhealthyHostCount;
        private int activeConnections;
        private String error;
    }

    @lombok.Builder
    @lombok.Data
    public static class ServiceMetrics {
        private Instant aggregatedAt;
        private BigDecimal avgCpu;
        private int latencyP99;
        private BigDecimal errorRate;
        private int totalRequestsPerMinute;
        private int totalHealthyInstances;
        private int totalUnhealthyInstances;
        private Map<String, RegionMetrics> regionMetrics;
    }

    @lombok.Builder
    @lombok.Data
    public static class HealthStatus {
        private boolean healthy;
        private String status;
        private List<String> sloViolations;
        private int healthyInstanceCount;
        private int unhealthyInstanceCount;
        private Instant lastCheckedAt;
    }
}
