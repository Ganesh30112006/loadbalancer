package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.entity.*;
import com.cloudplatform.loadbalancing.exception.ServiceNotFoundException;
import com.cloudplatform.loadbalancing.repository.ServiceRegionRepository;
import com.cloudplatform.loadbalancing.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Service Management Service
 * 
 * Manages the lifecycle of services under control of the platform,
 * including multi-region state, automation settings, and health tracking.
 */
@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final ServiceRegionRepository serviceRegionRepository;
    private final AccountService accountService;
    private final BlueprintService blueprintService;
    private final PolicyService policyService;
    private final AuditService auditService;

    /**
     * Create a new managed service
     */
    @Transactional
    public Service createService(CreateServiceRequest request, UUID userId) {
        log.info("Creating service: {}", request.serviceName);

        AwsAccount account = accountService.getAccountEntity(request.awsAccountId);
        Blueprint blueprint = blueprintService.getBlueprintEntity(request.blueprintId);
        Policy policy = policyService.getPolicyEntity(request.policyId);

        // Validate blueprint and policy are active
        if (blueprint.getStatus() != Blueprint.BlueprintStatus.ACTIVE) {
            throw new IllegalStateException("Blueprint must be ACTIVE to create a service");
        }
        if (policy.getStatus() != Policy.PolicyStatus.ACTIVE) {
            throw new IllegalStateException("Policy must be ACTIVE to create a service");
        }

        Service service = Service.builder()
                .serviceName(request.serviceName)
                .displayName(request.displayName)
                .description(request.description)
                .awsAccount(account)
                .blueprint(blueprint)
                .policy(policy)
                .primaryRegion(request.primaryRegion)
                .status(Service.ServiceStatus.PROVISIONING)
                .automationEnabled(false) // Start with automation disabled
                .scalingEnabled(false)
                .deploymentEnabled(false)
                .createdBy(userId)
                .build();

        service = serviceRepository.save(service);

        // Create service regions
        for (String region : request.regions) {
            ServiceRegion serviceRegion = ServiceRegion.builder()
                    .service(service)
                    .region(region)
                    .status(ServiceRegion.RegionStatus.PROVISIONING)
                    .trafficWeight(region.equals(request.primaryRegion) ? 100 : 0)
                    .build();
            serviceRegionRepository.save(serviceRegion);
        }

        auditService.logServiceAction(service, "SERVICE_CREATED", 
                "Service created with regions: " + String.join(", ", request.regions), userId);

        log.info("Service created: {} (ID: {})", service.getServiceName(), service.getId());
        return service;
    }

    /**
     * Get service by ID
     */
    @Transactional(readOnly = true)
    public Service getService(UUID serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + serviceId));
    }

    /**
     * Get service with regions
     */
    @Transactional(readOnly = true)
    public Service getServiceWithRegions(UUID serviceId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + serviceId));
        
        // Eagerly load regions
        service.getRegions().size();
        return service;
    }

    /**
     * Get all services for an account
     */
    @Transactional(readOnly = true)
    public List<Service> getServicesByAccount(UUID accountId) {
        return serviceRepository.findByAwsAccountIdWithRegions(accountId);
    }

    /**
     * Get all services
     */
    @Transactional(readOnly = true)
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    /**
     * Get services enabled for automation
     */
    @Transactional(readOnly = true)
    public List<Service> getServicesForControlLoop() {
        return serviceRepository.findServicesForControlLoop();
    }

    /**
     * Enable automation for a service
     */
    @Transactional
    public Service enableAutomation(UUID serviceId, AutomationSettings settings, UUID userId) {
        Service service = getService(serviceId);

        // Validate service is in a state that allows automation
        if (service.getStatus() != Service.ServiceStatus.ACTIVE && 
            service.getStatus() != Service.ServiceStatus.DEGRADED) {
            throw new IllegalStateException("Service must be ACTIVE or DEGRADED to enable automation");
        }

        service.setAutomationEnabled(settings.automationEnabled);
        service.setScalingEnabled(settings.scalingEnabled);
        service.setDeploymentEnabled(settings.deploymentEnabled);
        
        service = serviceRepository.save(service);

        auditService.logServiceAction(service, "AUTOMATION_ENABLED",
                "Automation: %s, Scaling: %s, Deployment: %s".formatted(
                        settings.automationEnabled, settings.scalingEnabled, settings.deploymentEnabled), 
                userId);

        log.info("Automation settings updated for service: {}", service.getServiceName());
        return service;
    }

    /**
     * Update service status
     */
    @Transactional
    public Service updateStatus(UUID serviceId, Service.ServiceStatus newStatus, String reason) {
        Service service = getService(serviceId);
        Service.ServiceStatus oldStatus = service.getStatus();
        
        service.setStatus(newStatus);
        service = serviceRepository.save(service);

        log.info("Service {} status changed: {} -> {} (Reason: {})", 
                service.getServiceName(), oldStatus, newStatus, reason);
        return service;
    }

    /**
     * Update region status
     */
    @Transactional
    public ServiceRegion updateRegionStatus(UUID regionId, ServiceRegion.RegionStatus newStatus, String reason) {
        ServiceRegion region = serviceRegionRepository.findById(regionId)
                .orElseThrow(() -> new ServiceNotFoundException("Region not found: " + regionId));
        
        region.setStatus(newStatus);
        region = serviceRegionRepository.save(region);

        log.info("Region {} status updated: {} (Reason: {})", 
                region.getRegion(), newStatus, reason);
        return region;
    }

    /**
     * Update region infrastructure references
     */
    @Transactional
    public ServiceRegion updateRegionInfrastructure(
            UUID regionId, 
            String asgName, 
            String albArn, 
            String targetGroupArn) {
        
        ServiceRegion region = serviceRegionRepository.findById(regionId)
                .orElseThrow(() -> new ServiceNotFoundException("Region not found: " + regionId));
        
        region.setAsgName(asgName);
        region.setAlbArn(albArn);
        region.setTargetGroupArn(targetGroupArn);
        region.setStatus(ServiceRegion.RegionStatus.ACTIVE);
        
        return serviceRegionRepository.save(region);
    }

    /**
     * Update region capacity
     */
    @Transactional
    public ServiceRegion updateRegionCapacity(UUID regionId, Integer desiredCapacity, Integer runningInstances) {
        ServiceRegion region = serviceRegionRepository.findById(regionId)
                .orElseThrow(() -> new ServiceNotFoundException("Region not found: " + regionId));
        
        if (desiredCapacity != null) region.setDesiredCapacity(desiredCapacity);
        if (runningInstances != null) region.setRunningInstances(runningInstances);
        region.setLastMetricsAt(Instant.now());
        
        return serviceRegionRepository.save(region);
    }

    /**
     * Update region traffic weight
     */
    @Transactional
    public void updateTrafficWeights(UUID serviceId, Map<String, Integer> regionWeights, UUID userId) {
        Service service = getService(serviceId);
        
        // Validate weights sum to 100
        int totalWeight = regionWeights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight != 100) {
            throw new IllegalArgumentException("Traffic weights must sum to 100, got: " + totalWeight);
        }

        List<ServiceRegion> regions = serviceRegionRepository.findByServiceId(serviceId);
        for (ServiceRegion region : regions) {
            Integer newWeight = regionWeights.get(region.getRegion());
            if (newWeight != null) {
                int oldWeight = region.getTrafficWeight();
                region.setTrafficWeight(newWeight);
                serviceRegionRepository.save(region);

                if (oldWeight != newWeight) {
                    auditService.logTrafficShift(service, region.getRegion(), region.getRegion(),
                            oldWeight, newWeight, "Manual traffic adjustment", true);
                }
            }
        }

        log.info("Traffic weights updated for service: {}", service.getServiceName());
    }

    /**
     * Update service health status
     */
    @Transactional
    public Service updateHealthStatus(UUID serviceId, Map<String, Object> healthStatus) {
        Service service = getService(serviceId);
        service.setHealthStatus(healthStatus);
        service.setLastHealthCheckAt(Instant.now());
        return serviceRepository.save(service);
    }

    /**
     * Get region by service and region name
     */
    @Transactional(readOnly = true)
    public ServiceRegion getServiceRegion(UUID serviceId, String region) {
        return serviceRegionRepository.findByServiceIdAndRegion(serviceId, region)
                .orElseThrow(() -> new ServiceNotFoundException(
                        "Region " + region + " not found for service: " + serviceId));
    }

    /**
     * Get all regions for a service
     */
    @Transactional(readOnly = true)
    public List<ServiceRegion> getServiceRegions(UUID serviceId) {
        return serviceRegionRepository.findByServiceId(serviceId);
    }

    /**
     * Apply manual override
     */
    @Transactional
    public Service applyOverride(UUID serviceId, OverrideRequest override, UUID userId) {
        Service service = getService(serviceId);

        if (override.disableScaling != null) {
            service.setScalingEnabled(!override.disableScaling);
        }
        if (override.disableDeployment != null) {
            service.setDeploymentEnabled(!override.disableDeployment);
        }
        if (override.disableAutomation != null) {
            service.setAutomationEnabled(!override.disableAutomation);
        }

        service = serviceRepository.save(service);

        auditService.logServiceAction(service, "OVERRIDE_APPLIED", 
                "Override applied: " + override.reason, userId);

        log.info("Override applied to service: {} - Reason: {}", 
                service.getServiceName(), override.reason);
        return service;
    }

    // Request/Response records
    public record CreateServiceRequest(
            String serviceName,
            String displayName,
            String description,
            UUID awsAccountId,
            UUID blueprintId,
            UUID policyId,
            String primaryRegion,
            List<String> regions
    ) {}

    public record AutomationSettings(
            boolean automationEnabled,
            boolean scalingEnabled,
            boolean deploymentEnabled
    ) {}

    public record OverrideRequest(
            Boolean disableScaling,
            Boolean disableDeployment,
            Boolean disableAutomation,
            Integer overrideDurationMinutes,
            String reason
    ) {}
}
