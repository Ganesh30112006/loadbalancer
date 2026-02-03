package com.cloudplatform.loadbalancing.controlloop;

import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.service.*;
import com.cloudplatform.loadbalancing.service.ObservabilityService.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hybrid Control Loop Engine
 * 
 * Implements the OODA (Observe-Orient-Decide-Act) control loop
 * for automated infrastructure management. Runs on a fixed interval
 * and makes deterministic scaling and health decisions.
 */
@org.springframework.stereotype.Service
@Slf4j
public class HybridControlLoop {

    private final ServiceManagementService serviceManagementService;
    private final ObservabilityService observabilityService;
    private final ScalingDecisionEngine scalingEngine;
    private final InfrastructureExecutor infrastructureExecutor;
    private final GuardrailService guardrailService;
    private final LeaderElectionService leaderElectionService;
    private final AuditService auditService;
    private final boolean enabled;

    // Track cooldowns per service
    private final Map<UUID, Instant> lastScaleOutTime = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lastScaleInTime = new ConcurrentHashMap<>();

    // Control loop cycle counter
    private long cycleCounter = 0;

    public HybridControlLoop(
            ServiceManagementService serviceManagementService,
            ObservabilityService observabilityService,
            ScalingDecisionEngine scalingEngine,
            InfrastructureExecutor infrastructureExecutor,
            GuardrailService guardrailService,
            LeaderElectionService leaderElectionService,
            AuditService auditService,
            @Value("${loadbalancing.control.loop.enabled:true}") boolean enabled) {
        this.serviceManagementService = serviceManagementService;
        this.observabilityService = observabilityService;
        this.scalingEngine = scalingEngine;
        this.infrastructureExecutor = infrastructureExecutor;
        this.guardrailService = guardrailService;
        this.leaderElectionService = leaderElectionService;
        this.auditService = auditService;
        this.enabled = enabled;
        
        if (!enabled) {
            log.info("Control loop is DISABLED");
        }
    }

    /**
     * Main control loop - runs every 30 seconds
     */
    @Scheduled(fixedRateString = "${loadbalancing.control-loop.interval-seconds:30}000")
    public void runControlLoop() {
        // Skip if disabled
        if (!enabled) {
            return;
        }
        
        // Only run if this instance is the leader
        if (!leaderElectionService.isLeader()) {
            log.debug("Not the leader, skipping control loop");
            return;
        }

        cycleCounter++;
        String cycleId = String.format("cycle-%d-%d", System.currentTimeMillis(), cycleCounter);
        log.debug("Starting control loop cycle: {}", cycleId);

        try {
            // Get all services enabled for automation
            List<Service> services = serviceManagementService.getServicesForControlLoop();
            
            for (Service service : services) {
                try {
                    processService(service, cycleId);
                } catch (Exception e) {
                    log.error("Error processing service {}: {}", service.getServiceName(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Control loop error: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single service through the control loop
     */
    private void processService(Service service, String cycleId) {
        Instant startTime = Instant.now();
        Map<String, Object> observationData = new HashMap<>();
        Map<String, Object> analysisResult = new HashMap<>();
        Map<String, Object> decisionDetails = new HashMap<>();
        Map<String, Object> executionDetails = new HashMap<>();
        boolean success = true;

        try {
            // ========== OBSERVE ==========
            Map<String, RegionMetrics> regionMetrics = observabilityService.collectAllRegionMetrics(service);
            ServiceMetrics serviceMetrics = observabilityService.aggregateServiceMetrics(regionMetrics);
            
            observationData.put("avgCpu", serviceMetrics.getAvgCpu());
            observationData.put("latencyP99", serviceMetrics.getLatencyP99());
            observationData.put("errorRate", serviceMetrics.getErrorRate());
            observationData.put("healthyInstances", serviceMetrics.getTotalHealthyInstances());
            observationData.put("requestsPerMinute", serviceMetrics.getTotalRequestsPerMinute());

            // ========== ORIENT (Analyze) ==========
            HealthStatus healthStatus = observabilityService.computeHealthStatus(service, serviceMetrics);
            ScalingDecision scalingDecision = scalingEngine.analyze(service, serviceMetrics, regionMetrics);
            
            analysisResult.put("healthStatus", healthStatus.getStatus());
            analysisResult.put("sloViolations", healthStatus.getSloViolations());
            analysisResult.put("scalingRecommendation", scalingDecision.action().name());
            analysisResult.put("scalingReason", scalingDecision.reason());

            // Update service health status
            serviceManagementService.updateHealthStatus(service.getId(), Map.of(
                    "status", healthStatus.getStatus(),
                    "healthy", healthStatus.isHealthy(),
                    "violations", healthStatus.getSloViolations(),
                    "lastCheckedAt", Instant.now().toString()
            ));

            // ========== DECIDE ==========
            ExecutionPlan plan = buildExecutionPlan(service, scalingDecision, healthStatus);
            
            decisionDetails.put("planActions", plan.actions.size());
            decisionDetails.put("planDescription", plan.description);

            // ========== ACT (Execute) ==========
            if (!plan.actions.isEmpty() && service.getScalingEnabled()) {
                ExecutionResult result = executeWithGuardrails(service, plan, cycleId);
                executionDetails.put("executed", result.executed);
                executionDetails.put("success", result.success);
                executionDetails.put("message", result.message);
                success = result.success;
            } else {
                executionDetails.put("executed", false);
                executionDetails.put("reason", plan.actions.isEmpty() ? 
                        "No actions required" : "Scaling disabled");
            }

        } catch (Exception e) {
            success = false;
            executionDetails.put("error", e.getMessage());
            log.error("Control loop error for service {}: {}", service.getServiceName(), e.getMessage());
        }

        // Record audit trail
        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        auditService.logControlLoopCycle(service, cycleId, 
                observationData, analysisResult, decisionDetails, executionDetails,
                success, durationMs);
    }

    /**
     * Build execution plan based on analysis
     */
    private ExecutionPlan buildExecutionPlan(
            Service service, 
            ScalingDecision scalingDecision,
            HealthStatus healthStatus) {

        List<PlannedAction> actions = new ArrayList<>();
        StringBuilder description = new StringBuilder();

        // Handle scaling decisions
        if (scalingDecision.action() != ScalingAction.NONE) {
            // Check cooldown
            if (!isCooldownActive(service.getId(), scalingDecision.action())) {
                for (var regionAction : scalingDecision.regionActions().entrySet()) {
                    actions.add(new PlannedAction(
                            scalingDecision.action() == ScalingAction.SCALE_OUT ? 
                                    ActionType.INCREASE_CAPACITY : ActionType.DECREASE_CAPACITY,
                            regionAction.getKey(),
                            regionAction.getValue(),
                            scalingDecision.reason()
                    ));
                }
                description.append(scalingDecision.action().name())
                        .append(": ")
                        .append(scalingDecision.reason());
            } else {
                description.append("Scaling skipped due to cooldown");
            }
        }

        // Handle health-based actions
        if (!healthStatus.isHealthy() && healthStatus.getSloViolations().size() > 2) {
            description.append(" | CRITICAL: ").append(String.join(", ", healthStatus.getSloViolations()));
        }

        return new ExecutionPlan(actions, description.toString(), Instant.now());
    }

    /**
     * Execute plan with guardrail checks
     */
    private ExecutionResult executeWithGuardrails(Service service, ExecutionPlan plan, String cycleId) {
        // Pre-execution guardrail check
        GuardrailService.GuardrailResult guardrailCheck = guardrailService.checkGuardrails(service, plan);
        
        if (!guardrailCheck.allowed()) {
            return new ExecutionResult(false, false, 
                    "Blocked by guardrails: " + String.join(", ", guardrailCheck.violations()));
        }

        // Execute each action
        List<String> results = new ArrayList<>();
        boolean allSuccess = true;

        for (PlannedAction action : plan.actions) {
            try {
                boolean actionSuccess = infrastructureExecutor.executeAction(service, action);
                results.add(String.format("%s %s: %s", 
                        action.actionType, action.region, actionSuccess ? "SUCCESS" : "FAILED"));
                
                if (!actionSuccess) allSuccess = false;

                // Update cooldown
                if (actionSuccess) {
                    updateCooldown(service, action);
                }

            } catch (Exception e) {
                results.add(String.format("%s %s: ERROR - %s", 
                        action.actionType, action.region, e.getMessage()));
                allSuccess = false;
            }
        }

        return new ExecutionResult(true, allSuccess, String.join("; ", results));
    }

    private boolean isCooldownActive(UUID serviceId, ScalingAction action) {
        Instant lastAction = action == ScalingAction.SCALE_OUT ? 
                lastScaleOutTime.get(serviceId) : lastScaleInTime.get(serviceId);
        
        if (lastAction == null) return false;

        Duration cooldown = action == ScalingAction.SCALE_OUT ? 
                Duration.ofMinutes(3) : Duration.ofMinutes(5);
        
        return Instant.now().isBefore(lastAction.plus(cooldown));
    }

    private void updateCooldown(Service service, PlannedAction action) {
        Instant now = Instant.now();
        if (action.actionType == ActionType.INCREASE_CAPACITY) {
            lastScaleOutTime.put(service.getId(), now);
        } else if (action.actionType == ActionType.DECREASE_CAPACITY) {
            lastScaleInTime.put(service.getId(), now);
        }
    }

    // Enums and records
    public enum ScalingAction {
        NONE, SCALE_OUT, SCALE_IN
    }

    public enum ActionType {
        INCREASE_CAPACITY,
        DECREASE_CAPACITY,
        SHIFT_TRAFFIC,
        FAILOVER,
        RESTORE
    }

    public record ScalingDecision(
            ScalingAction action,
            Map<String, Integer> regionActions, // region -> delta
            String reason,
            BigDecimal confidence
    ) {}

    public record PlannedAction(
            ActionType actionType,
            String region,
            int delta,
            String reason
    ) {}

    public record ExecutionPlan(
            List<PlannedAction> actions,
            String description,
            Instant plannedAt
    ) {}

    public record ExecutionResult(
            boolean executed,
            boolean success,
            String message
    ) {}
}
