package com.cloudplatform.loadbalancing.controlloop;

import com.cloudplatform.loadbalancing.controlloop.HybridControlLoop.*;
import com.cloudplatform.loadbalancing.entity.Policy;
import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.entity.ServiceRegion;
import com.cloudplatform.loadbalancing.service.ServiceManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guardrail Service
 * 
 * Implements safety constraints that prevent dangerous actions.
 * Acts as a final gate before any infrastructure changes.
 */
@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
public class GuardrailService {

    private final ServiceManagementService serviceManagementService;

    // Track recent actions for rate limiting
    private final Map<UUID, List<Instant>> recentScaleOuts = new ConcurrentHashMap<>();
    private final Map<UUID, List<Instant>> recentScaleIns = new ConcurrentHashMap<>();

    // Guardrail constants
    private static final int MAX_SCALE_ACTIONS_PER_HOUR = 10;
    private static final int ABSOLUTE_MIN_INSTANCES = 1;
    private static final int ABSOLUTE_MAX_INSTANCES = 100;
    private static final Duration ACTION_HISTORY_RETENTION = Duration.ofHours(1);

    /**
     * Check all guardrails before execution
     */
    public GuardrailResult checkGuardrails(Service service, ExecutionPlan plan) {
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (PlannedAction action : plan.actions()) {
            // Check per-action guardrails
            checkActionGuardrails(service, action, violations, warnings);
        }

        // Check aggregate guardrails
        checkAggregateGuardrails(service, plan, violations, warnings);

        // Check rate limiting
        checkRateLimits(service, plan, violations, warnings);

        // Check cost guardrails
        checkCostGuardrails(service, plan, violations, warnings);

        boolean allowed = violations.isEmpty();
        
        if (!violations.isEmpty()) {
            log.warn("Guardrail violations for service {}: {}", 
                    service.getServiceName(), violations);
        }

        return new GuardrailResult(allowed, violations, warnings);
    }

    /**
     * Check guardrails for a single action
     */
    private void checkActionGuardrails(
            Service service, 
            PlannedAction action,
            List<String> violations,
            List<String> warnings) {

        ServiceRegion region = serviceManagementService.getServiceRegion(service.getId(), action.region());
        Policy policy = service.getPolicy();
        Map<String, Object> scalingRules = policy.getScalingRules() != null ? 
                policy.getScalingRules() : Map.of();

        int minInstances = getInt(scalingRules, "minInstances", 2);
        int maxInstances = getInt(scalingRules, "maxInstances", 20);
        int maxScaleStep = getInt(scalingRules, "maxScaleOutStep", 4);

        int currentCapacity = region.getDesiredCapacity() != null ? 
                region.getDesiredCapacity() : 
                region.getRunningInstances() != null ? region.getRunningInstances() : 0;
        
        int newCapacity = currentCapacity + action.delta();

        // Check minimum instances
        if (newCapacity < ABSOLUTE_MIN_INSTANCES) {
            violations.add("Region %s: Cannot scale below absolute minimum of %d instances".formatted(
                    action.region(), ABSOLUTE_MIN_INSTANCES));
        } else if (newCapacity < minInstances) {
            violations.add("Region %s: Cannot scale below policy minimum of %d instances".formatted(
                    action.region(), minInstances));
        }

        // Check maximum instances
        if (newCapacity > ABSOLUTE_MAX_INSTANCES) {
            violations.add("Region %s: Cannot scale above absolute maximum of %d instances".formatted(
                    action.region(), ABSOLUTE_MAX_INSTANCES));
        } else if (newCapacity > maxInstances) {
            violations.add("Region %s: Cannot scale above policy maximum of %d instances".formatted(
                    action.region(), maxInstances));
        }

        // Check scale step size
        if (Math.abs(action.delta()) > maxScaleStep) {
            violations.add("Region %s: Scale step %d exceeds maximum step size of %d".formatted(
                    action.region(), Math.abs(action.delta()), maxScaleStep));
        }

        // Warning if scaling down more than 50%
        if (action.delta() < 0 && Math.abs(action.delta()) > currentCapacity / 2) {
            warnings.add("Region %s: Large scale-in of %d instances (>50%% reduction)".formatted(
                    action.region(), Math.abs(action.delta())));
        }
    }

    /**
     * Check aggregate guardrails across all planned actions
     */
    private void checkAggregateGuardrails(
            Service service,
            ExecutionPlan plan,
            List<String> violations,
            List<String> warnings) {

        // Calculate total capacity change
        int totalDelta = plan.actions().stream()
                .mapToInt(PlannedAction::delta)
                .sum();

        // Prevent massive scale-out in a single cycle
        if (totalDelta > 10) {
            violations.add("Total scale-out of %d instances exceeds single-cycle limit of 10".formatted(
                    totalDelta));
        }

        // Prevent scaling all regions simultaneously in the same direction
        long scaleOutRegions = plan.actions().stream()
                .filter(a -> a.delta() > 0)
                .count();
        long scaleInRegions = plan.actions().stream()
                .filter(a -> a.delta() < 0)
                .count();

        if (scaleOutRegions > 0 && scaleInRegions > 0) {
            warnings.add("Mixed scaling actions: some regions scaling out while others scaling in");
        }
    }

    /**
     * Check rate limits
     */
    private void checkRateLimits(
            Service service,
            ExecutionPlan plan,
            List<String> violations,
            List<String> warnings) {

        UUID serviceId = service.getId();
        Instant now = Instant.now();
        Instant cutoff = now.minus(ACTION_HISTORY_RETENTION);

        // Clean old entries and count recent actions
        cleanAndCount(recentScaleOuts, serviceId, cutoff);
        cleanAndCount(recentScaleIns, serviceId, cutoff);

        int recentOuts = recentScaleOuts.getOrDefault(serviceId, List.of()).size();
        int recentIns = recentScaleIns.getOrDefault(serviceId, List.of()).size();
        int totalRecent = recentOuts + recentIns;

        if (totalRecent >= MAX_SCALE_ACTIONS_PER_HOUR) {
            violations.add("Rate limit exceeded: %d scale actions in the last hour (max: %d)".formatted(
                    totalRecent, MAX_SCALE_ACTIONS_PER_HOUR));
        } else if (totalRecent >= MAX_SCALE_ACTIONS_PER_HOUR - 2) {
            warnings.add("Approaching rate limit: %d/%d scale actions in the last hour".formatted(
                    totalRecent, MAX_SCALE_ACTIONS_PER_HOUR));
        }
    }

    /**
     * Check cost guardrails
     */
    private void checkCostGuardrails(
            Service service,
            ExecutionPlan plan,
            List<String> violations,
            List<String> warnings) {

        Policy policy = service.getPolicy();
        Map<String, Object> costConfig = policy.getCostConfig();
        
        if (costConfig == null) return;

        BigDecimal dailyLimit = getDecimal(costConfig, "dailySpendLimit");
        if (dailyLimit == null) return;

        // Calculate projected cost increase
        // Simplified: assume each instance costs ~$0.10/hour
        int totalDelta = plan.actions().stream()
                .filter(a -> a.delta() > 0)
                .mapToInt(PlannedAction::delta)
                .sum();

        BigDecimal projectedDailyIncrease = BigDecimal.valueOf(totalDelta * 0.10 * 24);
        
        // This is a simplified check - in production would integrate with Cost Explorer
        if (projectedDailyIncrease.compareTo(dailyLimit.multiply(new BigDecimal("0.1"))) > 0) {
            warnings.add("Scaling will increase daily costs by approximately $%.2f".formatted(
                    projectedDailyIncrease));
        }
    }

    /**
     * Record an action for rate limiting
     */
    public void recordAction(UUID serviceId, boolean scaleOut) {
        Map<UUID, List<Instant>> target = scaleOut ? recentScaleOuts : recentScaleIns;
        target.computeIfAbsent(serviceId, k -> new ArrayList<>()).add(Instant.now());
    }

    private void cleanAndCount(Map<UUID, List<Instant>> map, UUID serviceId, Instant cutoff) {
        List<Instant> times = map.get(serviceId);
        if (times != null) {
            times.removeIf(t -> t.isBefore(cutoff));
        }
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number number) return number.intValue();
        return defaultValue;
    }

    private BigDecimal getDecimal(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        Object value = map.get(key);
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return null;
    }

    // Result record
    public record GuardrailResult(
            boolean allowed,
            List<String> violations,
            List<String> warnings
    ) {}
}
