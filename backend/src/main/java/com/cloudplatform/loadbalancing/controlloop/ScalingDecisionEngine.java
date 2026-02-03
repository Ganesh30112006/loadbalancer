package com.cloudplatform.loadbalancing.controlloop;

import com.cloudplatform.loadbalancing.controlloop.HybridControlLoop.*;
import com.cloudplatform.loadbalancing.entity.Policy;
import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.entity.ServiceRegion;
import com.cloudplatform.loadbalancing.service.ObservabilityService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Scaling Decision Engine
 * 
 * Analyzes metrics and determines scaling actions based on
 * policy rules. Uses deterministic rule-based logic with
 * optional AI/ML advisory signals (future enhancement).
 */
@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
public class ScalingDecisionEngine {

    // Default thresholds (overridden by policy)
    private static final BigDecimal DEFAULT_CPU_SCALE_OUT = new BigDecimal("70");
    private static final BigDecimal DEFAULT_CPU_SCALE_IN = new BigDecimal("30");
    private static final int DEFAULT_MIN_INSTANCES = 2;
    private static final int DEFAULT_MAX_INSTANCES = 20;
    private static final int DEFAULT_MAX_SCALE_STEP = 4;

    /**
     * Analyze metrics and produce scaling decision
     */
    public ScalingDecision analyze(
            Service service, 
            ServiceMetrics aggregateMetrics,
            Map<String, RegionMetrics> regionMetrics) {

        Policy policy = service.getPolicy();
        Map<String, Object> scalingRules = policy.getScalingRules() != null ? 
                policy.getScalingRules() : Map.of();

        // Extract thresholds from policy
        BigDecimal cpuScaleOutThreshold = getDecimal(scalingRules, "cpuScaleOutThreshold", DEFAULT_CPU_SCALE_OUT);
        BigDecimal cpuScaleInThreshold = getDecimal(scalingRules, "cpuScaleInThreshold", DEFAULT_CPU_SCALE_IN);
        int minInstances = getInt(scalingRules, "minInstances", DEFAULT_MIN_INSTANCES);
        int maxInstances = getInt(scalingRules, "maxInstances", DEFAULT_MAX_INSTANCES);
        int maxScaleStep = getInt(scalingRules, "maxScaleOutStep", DEFAULT_MAX_SCALE_STEP);

        // Current state
        int currentHealthy = aggregateMetrics.getTotalHealthyInstances();
        BigDecimal avgCpu = aggregateMetrics.getAvgCpu();
        BigDecimal errorRate = aggregateMetrics.getErrorRate();
        int latencyP99 = aggregateMetrics.getLatencyP99();

        // Determine scaling action
        ScalingAction action = ScalingAction.NONE;
        Map<String, Integer> regionActions = new HashMap<>();
        String reason = "No action needed";
        BigDecimal confidence = BigDecimal.ONE;

        // ===== SCALE OUT CONDITIONS =====
        if (shouldScaleOut(avgCpu, cpuScaleOutThreshold, errorRate, latencyP99, 
                currentHealthy, minInstances, policy)) {
            
            action = ScalingAction.SCALE_OUT;
            reason = buildScaleOutReason(avgCpu, cpuScaleOutThreshold, errorRate, latencyP99);
            
            // Calculate scale-out per region
            for (ServiceRegion region : service.getRegions()) {
                if (region.getStatus() == ServiceRegion.RegionStatus.ACTIVE) {
                    RegionMetrics rm = regionMetrics.get(region.getRegion());
                    if (rm != null) {
                        int delta = calculateScaleOutDelta(rm, region, maxScaleStep, maxInstances);
                        if (delta > 0) {
                            regionActions.put(region.getRegion(), delta);
                        }
                    }
                }
            }
            
            confidence = calculateConfidence(avgCpu, cpuScaleOutThreshold, true);
        }
        // ===== SCALE IN CONDITIONS =====
        else if (shouldScaleIn(avgCpu, cpuScaleInThreshold, errorRate, 
                currentHealthy, minInstances, policy)) {
            
            action = ScalingAction.SCALE_IN;
            reason = "Low CPU utilization: %.1f%% (threshold: %.1f%%)".formatted(
                    avgCpu, cpuScaleInThreshold);
            
            // Calculate scale-in per region (conservative)
            for (ServiceRegion region : service.getRegions()) {
                if (region.getStatus() == ServiceRegion.RegionStatus.ACTIVE) {
                    RegionMetrics rm = regionMetrics.get(region.getRegion());
                    if (rm != null) {
                        int delta = calculateScaleInDelta(rm, region, minInstances);
                        if (delta < 0) {
                            regionActions.put(region.getRegion(), delta);
                        }
                    }
                }
            }
            
            confidence = calculateConfidence(avgCpu, cpuScaleInThreshold, false);
        }

        // If no region actions, reset to NONE
        if (regionActions.isEmpty()) {
            action = ScalingAction.NONE;
            reason = "No actionable regions";
        }

        return new ScalingDecision(action, regionActions, reason, confidence);
    }

    private boolean shouldScaleOut(
            BigDecimal avgCpu, 
            BigDecimal threshold,
            BigDecimal errorRate,
            int latencyP99,
            int currentInstances,
            int minInstances,
            Policy policy) {

        // CPU-based scale out
        if (avgCpu.compareTo(threshold) > 0) {
            return true;
        }

        // Error rate based scale out (if errors > 1%)
        if (errorRate.compareTo(new BigDecimal("0.01")) > 0) {
            return true;
        }

        // Latency based scale out
        Map<String, Object> sloConfig = policy.getSloConfig();
        if (sloConfig != null) {
            int latencyTarget = getInt(sloConfig, "latencyP99TargetMs", 500);
            if (latencyP99 > latencyTarget * 1.2) { // 20% above target
                return true;
            }
        }

        // Below minimum instances
        if (currentInstances < minInstances) {
            return true;
        }

        return false;
    }

    private boolean shouldScaleIn(
            BigDecimal avgCpu,
            BigDecimal threshold,
            BigDecimal errorRate,
            int currentInstances,
            int minInstances,
            Policy policy) {

        // Never scale below minimum
        if (currentInstances <= minInstances) {
            return false;
        }

        // Don't scale in if error rate is elevated
        if (errorRate.compareTo(new BigDecimal("0.005")) > 0) {
            return false;
        }

        // CPU-based scale in (conservative)
        return avgCpu.compareTo(threshold) < 0;
    }

    private int calculateScaleOutDelta(RegionMetrics metrics, ServiceRegion region, 
            int maxStep, int maxInstances) {
        
        int current = region.getDesiredCapacity() != null ? region.getDesiredCapacity() : 
                region.getRunningInstances() != null ? region.getRunningInstances() : 0;
        
        // Don't exceed max
        int headroom = maxInstances - current;
        if (headroom <= 0) return 0;

        // Calculate based on CPU (how far above threshold)
        BigDecimal cpu = metrics.getAvgCpu();
        int delta;
        
        if (cpu.compareTo(new BigDecimal("90")) > 0) {
            delta = Math.min(4, maxStep); // Aggressive
        } else if (cpu.compareTo(new BigDecimal("80")) > 0) {
            delta = Math.min(2, maxStep);
        } else {
            delta = 1;
        }

        return Math.min(delta, headroom);
    }

    private int calculateScaleInDelta(RegionMetrics metrics, ServiceRegion region, int minInstances) {
        int current = region.getDesiredCapacity() != null ? region.getDesiredCapacity() : 
                region.getRunningInstances() != null ? region.getRunningInstances() : 0;
        
        // Maintain minimum per region
        int minPerRegion = Math.max(1, minInstances / 2);
        int headroom = current - minPerRegion;
        
        if (headroom <= 0) return 0;

        // Conservative scale-in (1 at a time)
        return -1;
    }

    private String buildScaleOutReason(BigDecimal cpu, BigDecimal threshold, 
            BigDecimal errorRate, int latencyP99) {
        
        List<String> reasons = new ArrayList<>();
        
        if (cpu.compareTo(threshold) > 0) {
            reasons.add("High CPU: %.1f%% (threshold: %.1f%%)".formatted(cpu, threshold));
        }
        if (errorRate.compareTo(new BigDecimal("0.01")) > 0) {
            reasons.add("Elevated errors: %.2f%%".formatted(
                    errorRate.multiply(BigDecimal.valueOf(100))));
        }
        if (latencyP99 > 500) {
            reasons.add("High latency: %dms P99".formatted(latencyP99));
        }

        return String.join("; ", reasons);
    }

    private BigDecimal calculateConfidence(BigDecimal cpu, BigDecimal threshold, boolean scaleOut) {
        // Higher confidence when further from threshold
        BigDecimal diff = cpu.subtract(threshold).abs();
        BigDecimal confidence = diff.divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP);
        
        // Clamp between 0.5 and 1.0
        if (confidence.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        if (confidence.compareTo(new BigDecimal("0.5")) < 0) {
            return new BigDecimal("0.5");
        }
        return confidence;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number number) return number.intValue();
        return defaultValue;
    }

    private BigDecimal getDecimal(Map<String, Object> map, String key, BigDecimal defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object value = map.get(key);
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return defaultValue;
    }
}
