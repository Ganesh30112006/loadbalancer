package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.aws.AwsClientProvider;
import com.cloudplatform.loadbalancing.dto.BlueprintDto;
import com.cloudplatform.loadbalancing.entity.AwsAccount;
import com.cloudplatform.loadbalancing.entity.Blueprint;
import com.cloudplatform.loadbalancing.exception.BlueprintNotFoundException;
import com.cloudplatform.loadbalancing.repository.BlueprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.Image;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Blueprint Service
 * 
 * Manages Application Blueprints - reusable templates that define
 * AMI, instance type, launch template, and other infrastructure settings.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class BlueprintService {

    private final BlueprintRepository blueprintRepository;
    private final AccountService accountService;
    private final AwsClientProvider awsClientProvider;
    // private final AuditService auditService; // Reserved for future audit logging

    /**
     * Create a new blueprint
     */
    @Transactional
    public BlueprintDto.Response createBlueprint(BlueprintDto.CreateRequest request, UUID userId) {
        log.info("Creating blueprint: {}", request.getName());

        AwsAccount account = accountService.getAccountEntity(request.getAwsAccountId());

        // Build launch template configuration
        Map<String, Object> launchTemplateConfig = buildLaunchTemplateConfig(request);

        Blueprint blueprint = Blueprint.builder()
                .name(request.getName())
                .description(request.getDescription())
                .awsAccount(account)
                .amiId(request.getAmiId())
                .instanceType(request.getInstanceType())
                .launchTemplateConfig(launchTemplateConfig)
                .healthCheckPath(request.getHealthCheckPath())
                .healthCheckIntervalSeconds(request.getHealthCheckIntervalSeconds() != null ? 
                        request.getHealthCheckIntervalSeconds() : 30)
                .drainTimeSeconds(request.getDrainTimeSeconds() != null ? 
                        request.getDrainTimeSeconds() : 300)
                .startupTimeSeconds(request.getStartupTimeSeconds() != null ? 
                        request.getStartupTimeSeconds() : 120)
                .version(1)
                .status(Blueprint.BlueprintStatus.DRAFT)
                .createdBy(userId)
                .build();

        blueprint = blueprintRepository.save(blueprint);

        log.info("Blueprint created: {} (ID: {})", blueprint.getName(), blueprint.getId());
        return mapToResponse(blueprint);
    }

    /**
     * Update a blueprint (creates new version)
     */
    @Transactional
    public BlueprintDto.Response updateBlueprint(UUID blueprintId, BlueprintDto.UpdateRequest request, UUID userId) {
        Blueprint blueprint = blueprintRepository.findById(blueprintId)
                .orElseThrow(() -> new BlueprintNotFoundException("Blueprint not found: " + blueprintId));

        // Cannot update active blueprints directly - must create new version
        if (blueprint.getStatus() == Blueprint.BlueprintStatus.ACTIVE) {
            // Clone and create new version
            Blueprint newVersion = cloneBlueprint(blueprint);
            applyUpdates(newVersion, request);
            newVersion.setVersion(blueprint.getVersion() + 1);
            newVersion.setStatus(Blueprint.BlueprintStatus.DRAFT);
            newVersion.setCreatedBy(userId);
            newVersion.setApprovedBy(null);
            newVersion.setApprovedAt(null);

            newVersion = blueprintRepository.save(newVersion);
            log.info("Created new blueprint version: {} v{}", newVersion.getName(), newVersion.getVersion());
            return mapToResponse(newVersion);
        }

        // Update draft/deprecated blueprints directly
        applyUpdates(blueprint, request);
        blueprint = blueprintRepository.save(blueprint);

        log.info("Updated blueprint: {} (ID: {})", blueprint.getName(), blueprint.getId());
        return mapToResponse(blueprint);
    }

    /**
     * Validate AMI exists and is accessible
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateAmi(UUID blueprintId, String region) {
        Blueprint blueprint = blueprintRepository.findById(blueprintId)
                .orElseThrow(() -> new BlueprintNotFoundException("Blueprint not found: " + blueprintId));

        try {
            Ec2Client ec2Client = awsClientProvider.getEc2Client(blueprint.getAwsAccount(), region);
            
            DescribeImagesResponse response = ec2Client.describeImages(
                    DescribeImagesRequest.builder()
                            .imageIds(blueprint.getAmiId())
                            .build()
            );

            if (response.images().isEmpty()) {
                return Map.of(
                        "valid", false,
                        "amiId", blueprint.getAmiId(),
                        "region", region,
                        "error", "AMI not found"
                );
            }

            Image ami = response.images().get(0);
            return Map.of(
                    "valid", true,
                    "amiId", blueprint.getAmiId(),
                    "region", region,
                    "name", ami.name() != null ? ami.name() : "N/A",
                    "state", ami.stateAsString(),
                    "architecture", ami.architectureAsString(),
                    "platform", ami.platformAsString() != null ? ami.platformAsString() : "Linux"
            );

        } catch (Exception e) {
            log.error("Failed to validate AMI: {}", e.getMessage());
            return Map.of(
                    "valid", false,
                    "amiId", blueprint.getAmiId(),
                    "region", region,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Approve a blueprint for use
     */
    @Transactional
    public BlueprintDto.Response approveBlueprint(UUID blueprintId, BlueprintDto.ApprovalRequest request, UUID userId) {
        Blueprint blueprint = blueprintRepository.findById(blueprintId)
                .orElseThrow(() -> new BlueprintNotFoundException("Blueprint not found: " + blueprintId));

        if (blueprint.getStatus() != Blueprint.BlueprintStatus.DRAFT) {
            throw new IllegalStateException("Only draft blueprints can be approved");
        }

        // Deprecate previous active versions
        blueprintRepository.findByNameAndStatus(blueprint.getName(), Blueprint.BlueprintStatus.ACTIVE)
                .ifPresent(activeBlueprint -> {
                    activeBlueprint.setStatus(Blueprint.BlueprintStatus.DEPRECATED);
                    blueprintRepository.save(activeBlueprint);
                });

        blueprint.setStatus(Blueprint.BlueprintStatus.ACTIVE);
        blueprint.setApprovedBy(userId);
        blueprint.setApprovedAt(Instant.now());
        blueprint = blueprintRepository.save(blueprint);

        log.info("Blueprint approved: {} v{}", blueprint.getName(), blueprint.getVersion());
        return mapToResponse(blueprint);
    }

    /**
     * Get blueprint by ID
     */
    @Transactional(readOnly = true)
    public BlueprintDto.Response getBlueprint(UUID blueprintId) {
        Blueprint blueprint = blueprintRepository.findById(blueprintId)
                .orElseThrow(() -> new BlueprintNotFoundException("Blueprint not found: " + blueprintId));
        return mapToResponse(blueprint);
    }

    /**
     * Get all blueprints for an account
     */
    @Transactional(readOnly = true)
    public List<BlueprintDto.Response> getBlueprintsByAccount(UUID accountId) {
        return blueprintRepository.findByAwsAccountId(accountId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active blueprints for an account
     */
    @Transactional(readOnly = true)
    public List<BlueprintDto.Response> getActiveBlueprintsByAccount(UUID accountId) {
        return blueprintRepository.findActiveByAwsAccountId(accountId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get blueprint entity by ID
     */
    @Transactional(readOnly = true)
    public Blueprint getBlueprintEntity(UUID blueprintId) {
        return blueprintRepository.findById(blueprintId)
                .orElseThrow(() -> new BlueprintNotFoundException("Blueprint not found: " + blueprintId));
    }

    /**
     * Delete a draft blueprint
     */
    @Transactional
    public void deleteBlueprint(UUID blueprintId, UUID userId) {
        Blueprint blueprint = blueprintRepository.findById(blueprintId)
                .orElseThrow(() -> new BlueprintNotFoundException("Blueprint not found: " + blueprintId));

        if (blueprint.getStatus() == Blueprint.BlueprintStatus.ACTIVE) {
            throw new IllegalStateException("Cannot delete active blueprints. Deprecate first.");
        }

        blueprintRepository.delete(blueprint);
        log.info("Deleted blueprint: {} (ID: {})", blueprint.getName(), blueprintId);
    }

    private Blueprint cloneBlueprint(Blueprint source) {
        return Blueprint.builder()
                .name(source.getName())
                .description(source.getDescription())
                .awsAccount(source.getAwsAccount())
                .amiId(source.getAmiId())
                .instanceType(source.getInstanceType())
                .launchTemplateConfig(new HashMap<>(source.getLaunchTemplateConfig()))
                .healthCheckPath(source.getHealthCheckPath())
                .healthCheckIntervalSeconds(source.getHealthCheckIntervalSeconds())
                .drainTimeSeconds(source.getDrainTimeSeconds())
                .startupTimeSeconds(source.getStartupTimeSeconds())
                .build();
    }

    private void applyUpdates(Blueprint blueprint, BlueprintDto.UpdateRequest request) {
        if (request.getName() != null) blueprint.setName(request.getName());
        if (request.getDescription() != null) blueprint.setDescription(request.getDescription());
        if (request.getAmiId() != null) blueprint.setAmiId(request.getAmiId());
        if (request.getInstanceType() != null) blueprint.setInstanceType(request.getInstanceType());
        if (request.getHealthCheckPath() != null) blueprint.setHealthCheckPath(request.getHealthCheckPath());
        if (request.getHealthCheckIntervalSeconds() != null) 
            blueprint.setHealthCheckIntervalSeconds(request.getHealthCheckIntervalSeconds());
        if (request.getDrainTimeSeconds() != null) 
            blueprint.setDrainTimeSeconds(request.getDrainTimeSeconds());
        if (request.getStartupTimeSeconds() != null) 
            blueprint.setStartupTimeSeconds(request.getStartupTimeSeconds());

        // Merge launch template config
        if (request.getSecurityGroupIds() != null || request.getSubnetIds() != null ||
            request.getIamInstanceProfile() != null || request.getUserData() != null) {
            Map<String, Object> config = blueprint.getLaunchTemplateConfig() != null ?
                    new HashMap<>(blueprint.getLaunchTemplateConfig()) : new HashMap<>();
            
            if (request.getSecurityGroupIds() != null) config.put("securityGroupIds", request.getSecurityGroupIds());
            if (request.getSubnetIds() != null) config.put("subnetIds", request.getSubnetIds());
            if (request.getIamInstanceProfile() != null) config.put("iamInstanceProfile", request.getIamInstanceProfile());
            if (request.getUserData() != null) config.put("userData", request.getUserData());
            
            blueprint.setLaunchTemplateConfig(config);
        }
    }

    private Map<String, Object> buildLaunchTemplateConfig(BlueprintDto.CreateRequest request) {
        Map<String, Object> config = new HashMap<>();
        if (request.getSecurityGroupIds() != null) config.put("securityGroupIds", request.getSecurityGroupIds());
        if (request.getSubnetIds() != null) config.put("subnetIds", request.getSubnetIds());
        if (request.getIamInstanceProfile() != null) config.put("iamInstanceProfile", request.getIamInstanceProfile());
        if (request.getUserData() != null) config.put("userData", request.getUserData());
        if (request.getTags() != null) config.put("tags", request.getTags());
        return config;
    }

    @SuppressWarnings("unchecked")
    private BlueprintDto.Response mapToResponse(Blueprint blueprint) {
        Map<String, Object> config = blueprint.getLaunchTemplateConfig();
        
        return BlueprintDto.Response.builder()
                .id(blueprint.getId())
                .name(blueprint.getName())
                .description(blueprint.getDescription())
                .awsAccountId(blueprint.getAwsAccount().getId())
                .awsAccountName(blueprint.getAwsAccount().getAccountName())
                .amiId(blueprint.getAmiId())
                .instanceType(blueprint.getInstanceType())
                .securityGroupIds(config != null ? (List<String>) config.get("securityGroupIds") : null)
                .subnetIds(config != null ? (List<String>) config.get("subnetIds") : null)
                .iamInstanceProfile(config != null ? (String) config.get("iamInstanceProfile") : null)
                .healthCheckPath(blueprint.getHealthCheckPath())
                .healthCheckIntervalSeconds(blueprint.getHealthCheckIntervalSeconds())
                .drainTimeSeconds(blueprint.getDrainTimeSeconds())
                .startupTimeSeconds(blueprint.getStartupTimeSeconds())
                .version(blueprint.getVersion())
                .status(blueprint.getStatus().name())
                .createdBy(blueprint.getCreatedBy())
                .approvedBy(blueprint.getApprovedBy())
                .approvedAt(blueprint.getApprovedAt())
                .createdAt(blueprint.getCreatedAt())
                .updatedAt(blueprint.getUpdatedAt())
                .build();
    }
}
