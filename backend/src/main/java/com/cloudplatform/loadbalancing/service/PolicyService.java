package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.dto.PolicyDto;
import com.cloudplatform.loadbalancing.entity.AwsAccount;
import com.cloudplatform.loadbalancing.entity.Policy;
import com.cloudplatform.loadbalancing.exception.PolicyNotFoundException;
import com.cloudplatform.loadbalancing.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Policy Service
 * 
 * Manages SLO Policies that define scaling rules, cost limits,
 * deployment strategies, and governance constraints.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final AccountService accountService;

    /**
     * Create a new policy
     */
    @Transactional
    public PolicyDto.Response createPolicy(PolicyDto.CreateRequest request, UUID userId) {
        log.info("Creating policy: {}", request.getName());

        AwsAccount account = accountService.getAccountEntity(request.getAwsAccountId());

        // Build SLO configuration
        Map<String, Object> sloConfig = buildSloConfig(request.getSloConfig());
        
        // Build scaling rules
        Map<String, Object> scalingRules = buildScalingRules(request.getScalingRules());
        
        // Build cost configuration
        Map<String, Object> costConfig = buildCostConfig(request.getCostConfig());
        
        // Build deployment configuration
        Map<String, Object> deploymentConfig = buildDeploymentConfig(request.getDeploymentConfig());

        Policy policy = Policy.builder()
                .name(request.getName())
                .description(request.getDescription())
                .awsAccount(account)
                .sloConfig(sloConfig)
                .scalingRules(scalingRules)
                .costConfig(costConfig)
                .deploymentConfig(deploymentConfig)
                .version(1)
                .status(Policy.PolicyStatus.DRAFT)
                .createdBy(userId)
                .build();

        policy = policyRepository.save(policy);

        log.info("Policy created: {} (ID: {})", policy.getName(), policy.getId());
        return mapToResponse(policy);
    }

    /**
     * Update a policy (creates new version if active)
     */
    @Transactional
    public PolicyDto.Response updatePolicy(UUID policyId, PolicyDto.UpdateRequest request, UUID userId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));

        if (policy.getStatus() == Policy.PolicyStatus.ACTIVE) {
            // Create new version
            Policy newVersion = clonePolicy(policy);
            applyUpdates(newVersion, request);
            newVersion.setVersion(policy.getVersion() + 1);
            newVersion.setStatus(Policy.PolicyStatus.DRAFT);
            newVersion.setCreatedBy(userId);
            newVersion.setApprovedBy(null);
            newVersion.setApprovedAt(null);

            newVersion = policyRepository.save(newVersion);
            log.info("Created new policy version: {} v{}", newVersion.getName(), newVersion.getVersion());
            return mapToResponse(newVersion);
        }

        applyUpdates(policy, request);
        policy = policyRepository.save(policy);

        log.info("Updated policy: {} (ID: {})", policy.getName(), policy.getId());
        return mapToResponse(policy);
    }

    /**
     * Approve a policy for use
     */
    @Transactional
    public PolicyDto.Response approvePolicy(UUID policyId, UUID userId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));

        if (policy.getStatus() != Policy.PolicyStatus.DRAFT) {
            throw new IllegalStateException("Only draft policies can be approved");
        }

        // Deprecate previous active versions
        policyRepository.findByNameAndStatus(policy.getName(), Policy.PolicyStatus.ACTIVE)
                .ifPresent(activePolicy -> {
                    activePolicy.setStatus(Policy.PolicyStatus.DEPRECATED);
                    policyRepository.save(activePolicy);
                });

        policy.setStatus(Policy.PolicyStatus.ACTIVE);
        policy.setApprovedBy(userId);
        policy.setApprovedAt(Instant.now());
        policy = policyRepository.save(policy);

        log.info("Policy approved: {} v{}", policy.getName(), policy.getVersion());
        return mapToResponse(policy);
    }

    /**
     * Get policy by ID
     */
    @Transactional(readOnly = true)
    public PolicyDto.Response getPolicy(UUID policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));
        return mapToResponse(policy);
    }

    /**
     * Get all policies for an account
     */
    @Transactional(readOnly = true)
    public List<PolicyDto.Response> getPoliciesByAccount(UUID accountId) {
        return policyRepository.findByAwsAccountId(accountId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active policies for an account
     */
    @Transactional(readOnly = true)
    public List<PolicyDto.Response> getActivePoliciesByAccount(UUID accountId) {
        return policyRepository.findActiveByAwsAccountId(accountId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get policy entity by ID
     */
    @Transactional(readOnly = true)
    public Policy getPolicyEntity(UUID policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));
    }

    /**
     * Delete a draft policy
     */
    @Transactional
    public void deletePolicy(UUID policyId, UUID userId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyId));

        if (policy.getStatus() == Policy.PolicyStatus.ACTIVE) {
            throw new IllegalStateException("Cannot delete active policies. Deprecate first.");
        }

        policyRepository.delete(policy);
        log.info("Deleted policy: {} (ID: {})", policy.getName(), policyId);
    }

    /**
     * Extract SLO thresholds from policy
     */
    public Map<String, Object> getSloThresholds(Policy policy) {
        return policy.getSloConfig() != null ? policy.getSloConfig() : Map.of();
    }

    /**
     * Extract scaling rules from policy
     */
    public Map<String, Object> getScalingRules(Policy policy) {
        return policy.getScalingRules() != null ? policy.getScalingRules() : Map.of();
    }

    /**
     * Extract cost limits from policy
     */
    public Map<String, Object> getCostLimits(Policy policy) {
        return policy.getCostConfig() != null ? policy.getCostConfig() : Map.of();
    }

    /**
     * Build SLO configuration map
     */
    private Map<String, Object> buildSloConfig(Map<String, Object> input) {
        return input != null ? new HashMap<>(input) : new HashMap<>();
    }

    /**
     * Build scaling rules map
     */
    private Map<String, Object> buildScalingRules(Map<String, Object> input) {
        return input != null ? new HashMap<>(input) : new HashMap<>();
    }

    /**
     * Build cost configuration map
     */
    private Map<String, Object> buildCostConfig(Map<String, Object> input) {
        return input != null ? new HashMap<>(input) : new HashMap<>();
    }

    /**
     * Build deployment configuration map
     */
    private Map<String, Object> buildDeploymentConfig(Map<String, Object> input) {
        return input != null ? new HashMap<>(input) : new HashMap<>();
    }

    private Policy clonePolicy(Policy source) {
        return Policy.builder()
                .name(source.getName())
                .description(source.getDescription())
                .awsAccount(source.getAwsAccount())
                .sloConfig(source.getSloConfig() != null ? new HashMap<>(source.getSloConfig()) : null)
                .scalingRules(source.getScalingRules() != null ? new HashMap<>(source.getScalingRules()) : null)
                .costConfig(source.getCostConfig() != null ? new HashMap<>(source.getCostConfig()) : null)
                .deploymentConfig(source.getDeploymentConfig() != null ? 
                        new HashMap<>(source.getDeploymentConfig()) : null)
                .build();
    }

    private void applyUpdates(Policy policy, PolicyDto.UpdateRequest request) {
        if (request.getName() != null) policy.setName(request.getName());
        if (request.getDescription() != null) policy.setDescription(request.getDescription());
        if (request.getSloConfig() != null) policy.setSloConfig(buildSloConfig(request.getSloConfig()));
        if (request.getScalingRules() != null) policy.setScalingRules(buildScalingRules(request.getScalingRules()));
        if (request.getCostConfig() != null) policy.setCostConfig(buildCostConfig(request.getCostConfig()));
        if (request.getDeploymentConfig() != null) 
            policy.setDeploymentConfig(buildDeploymentConfig(request.getDeploymentConfig()));
    }

    private PolicyDto.Response mapToResponse(Policy policy) {
        return PolicyDto.Response.builder()
                .id(policy.getId())
                .name(policy.getName())
                .description(policy.getDescription())
                .version(policy.getVersion())
                .status(policy.getStatus().name())
                .isActive(policy.getIsActive())
                .createdBy(policy.getCreatedBy())
                .approvedBy(policy.getApprovedBy())
                .approvedAt(policy.getApprovedAt())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .sloAvailability(policy.getSloAvailability())
                .sloLatencyP99Ms(policy.getSloLatencyP99Ms())
                .sloErrorRate(policy.getSloErrorRate())
                .minInstances(policy.getMinInstances())
                .maxInstances(policy.getMaxInstances())
                .desiredInstances(policy.getDesiredInstances())
                .scaleOutCpuThreshold(policy.getScaleOutCpuThreshold())
                .scaleInCpuThreshold(policy.getScaleInCpuThreshold())
                .scaleOutCooldownSeconds(policy.getScaleOutCooldownSeconds())
                .scaleInCooldownSeconds(policy.getScaleInCooldownSeconds())
                .maxScaleOutStep(policy.getMaxScaleOutStep())
                .maxScaleInStep(policy.getMaxScaleInStep())
                .minOnDemandInstances(policy.getMinOnDemandInstances())
                .maxSpotRatio(policy.getMaxSpotRatio())
                .maxHourlyCost(policy.getMaxHourlyCost())
                .deploymentStrategy(policy.getDeploymentStrategy() != null ? 
                        policy.getDeploymentStrategy().name() : null)
                .canarySteps(policy.getCanarySteps())
                .canaryStepIntervalMinutes(policy.getCanaryStepIntervalMinutes())
                .autoRollbackEnabled(policy.getAutoRollbackEnabled())
                .policyConfig(policy.getPolicyConfig())
                .build();
    }
}
