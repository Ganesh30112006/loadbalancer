package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.entity.AuditLog;
import com.cloudplatform.loadbalancing.entity.AwsAccount;
import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;
import java.util.UUID;

/**
 * Audit Service
 * 
 * Records all control plane decisions, actions, and events
 * for compliance, debugging, and operational visibility.
 */
@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an account-level action
     */
    @Async
    public void logAccountAction(AwsAccount account, String actionType, String description, UUID userId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .awsAccountId(account.getId())
                    .actionType(AuditLog.ActionType.valueOf(actionType))
                    .actionCategory(AuditLog.ActionCategory.ACCOUNT)
                    .description(description)
                    .status(AuditLog.ActionStatus.COMPLETED)
                    .triggeredBy(userId)
                    .isManualOverride(userId != null)
                    .platformInstanceId(getInstanceId())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit logged: {} for account {}", actionType, account.getAccountId());
        } catch (Exception e) {
            log.error("Failed to log audit: {}", e.getMessage());
        }
    }

    /**
     * Log a service-level action
     */
    @Async
    public void logServiceAction(Service service, String actionType, String description, UUID userId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .serviceId(service.getId())
                    .awsAccountId(service.getAwsAccount().getId())
                    .actionType(AuditLog.ActionType.valueOf(actionType))
                    .actionCategory(categorizeAction(actionType))
                    .description(description)
                    .status(AuditLog.ActionStatus.COMPLETED)
                    .triggeredBy(userId)
                    .isManualOverride(userId != null)
                    .platformInstanceId(getInstanceId())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit: {}", e.getMessage());
        }
    }

    /**
     * Log a control loop cycle
     */
    public void logControlLoopCycle(
            Service service,
            String cycleId,
            Map<String, Object> observationData,
            Map<String, Object> analysisResult,
            Map<String, Object> decisionDetails,
            Map<String, Object> executionDetails,
            boolean success,
            long durationMs) {
        
        try {
            AuditLog auditLog = AuditLog.builder()
                    .serviceId(service.getId())
                    .awsAccountId(service.getAwsAccount().getId())
                    .cycleId(cycleId)
                    .actionType(AuditLog.ActionType.CONTROL_LOOP_CYCLE)
                    .actionCategory(AuditLog.ActionCategory.CONTROL_LOOP)
                    .description("Control loop cycle " + cycleId)
                    .status(success ? AuditLog.ActionStatus.COMPLETED : AuditLog.ActionStatus.FAILED)
                    .observationData(observationData)
                    .analysisResult(analysisResult)
                    .decisionDetails(decisionDetails)
                    .executionDetails(executionDetails)
                    .durationMs(durationMs)
                    .platformInstanceId(getInstanceId())
                    .isManualOverride(false)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log control loop cycle: {}", e.getMessage());
        }
    }

    /**
     * Log a scaling action
     */
    public void logScalingAction(
            Service service,
            String cycleId,
            boolean scaleOut,
            int previousCapacity,
            int newCapacity,
            String reason,
            boolean success,
            String errorMessage) {
        
        try {
            AuditLog auditLog = AuditLog.builder()
                    .serviceId(service.getId())
                    .awsAccountId(service.getAwsAccount().getId())
                    .cycleId(cycleId)
                    .actionType(scaleOut ? AuditLog.ActionType.SCALE_OUT : AuditLog.ActionType.SCALE_IN)
                    .actionCategory(AuditLog.ActionCategory.SCALING)
                    .description("%s: %d -> %d instances. Reason: %s".formatted(
                            scaleOut ? "Scale out" : "Scale in",
                            previousCapacity, newCapacity, reason))
                    .status(success ? AuditLog.ActionStatus.COMPLETED : AuditLog.ActionStatus.FAILED)
                    .decisionDetails(Map.of(
                            "previousCapacity", previousCapacity,
                            "newCapacity", newCapacity,
                            "reason", reason
                    ))
                    .errorMessage(errorMessage)
                    .platformInstanceId(getInstanceId())
                    .isManualOverride(false)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log scaling action: {}", e.getMessage());
        }
    }

    /**
     * Log a deployment action
     */
    public void logDeploymentAction(
            Service service,
            UUID deploymentId,
            String actionType,
            String description,
            Map<String, Object> details,
            boolean success,
            UUID userId) {
        
        try {
            AuditLog auditLog = AuditLog.builder()
                    .serviceId(service.getId())
                    .awsAccountId(service.getAwsAccount().getId())
                    .actionType(AuditLog.ActionType.valueOf(actionType))
                    .actionCategory(AuditLog.ActionCategory.DEPLOYMENT)
                    .description(description)
                    .status(success ? AuditLog.ActionStatus.COMPLETED : AuditLog.ActionStatus.FAILED)
                    .executionDetails(details)
                    .triggeredBy(userId)
                    .isManualOverride(userId != null)
                    .platformInstanceId(getInstanceId())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log deployment action: {}", e.getMessage());
        }
    }

    /**
     * Log traffic shift action
     */
    public void logTrafficShift(
            Service service,
            String sourceRegion,
            String targetRegion,
            int previousWeight,
            int newWeight,
            String reason,
            boolean success) {
        
        try {
            AuditLog auditLog = AuditLog.builder()
                    .serviceId(service.getId())
                    .awsAccountId(service.getAwsAccount().getId())
                    .actionType(AuditLog.ActionType.TRAFFIC_SHIFT)
                    .actionCategory(AuditLog.ActionCategory.TRAFFIC)
                    .description("Traffic shift %s->%s: %d%% -> %d%%. Reason: %s".formatted(
                            sourceRegion, targetRegion, previousWeight, newWeight, reason))
                    .status(success ? AuditLog.ActionStatus.COMPLETED : AuditLog.ActionStatus.FAILED)
                    .decisionDetails(Map.of(
                            "sourceRegion", sourceRegion,
                            "targetRegion", targetRegion,
                            "previousWeight", previousWeight,
                            "newWeight", newWeight,
                            "reason", reason
                    ))
                    .platformInstanceId(getInstanceId())
                    .isManualOverride(false)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log traffic shift: {}", e.getMessage());
        }
    }

    private AuditLog.ActionCategory categorizeAction(String actionType) {
        if (actionType.startsWith("SCALE")) return AuditLog.ActionCategory.SCALING;
        if (actionType.startsWith("DEPLOYMENT")) return AuditLog.ActionCategory.DEPLOYMENT;
        if (actionType.startsWith("TRAFFIC") || actionType.startsWith("FAILOVER")) 
            return AuditLog.ActionCategory.TRAFFIC;
        if (actionType.startsWith("HEALTH")) return AuditLog.ActionCategory.HEALTH;
        if (actionType.startsWith("POLICY") || actionType.startsWith("BLUEPRINT") || 
            actionType.startsWith("OVERRIDE")) return AuditLog.ActionCategory.CONFIGURATION;
        if (actionType.startsWith("ACCOUNT")) return AuditLog.ActionCategory.ACCOUNT;
        return AuditLog.ActionCategory.CONTROL_LOOP;
    }

    private String getInstanceId() {
        // In production, this would be the container/pod ID or EC2 instance ID
        return System.getenv().getOrDefault("HOSTNAME", "local-" + ProcessHandle.current().pid());
    }
}
