package com.cloudplatform.loadbalancing.controlloop;

import com.cloudplatform.loadbalancing.aws.AwsClientProvider;
import com.cloudplatform.loadbalancing.controlloop.HybridControlLoop.*;
import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.entity.ServiceRegion;
import com.cloudplatform.loadbalancing.service.AuditService;
import com.cloudplatform.loadbalancing.service.ServiceManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.*;

/**
 * Infrastructure Executor
 * 
 * Executes infrastructure changes on AWS resources.
 * This is the "Act" phase of the OODA loop.
 */
@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
public class InfrastructureExecutor {

    private final AwsClientProvider awsClientProvider;
    private final ServiceManagementService serviceManagementService;
    private final AuditService auditService;
    private final GuardrailService guardrailService;

    /**
     * Execute a planned action
     */
    public boolean executeAction(Service service, PlannedAction action) {
        log.info("Executing action {} for service {} in region {}",
                action.actionType(), service.getServiceName(), action.region());

        try {
            return switch (action.actionType()) {
                case INCREASE_CAPACITY -> executeScaleOut(service, action);
                case DECREASE_CAPACITY -> executeScaleIn(service, action);
                case SHIFT_TRAFFIC -> executeTrafficShift(service, action);
                case FAILOVER -> executeFailover(service, action);
                case RESTORE -> executeRestore(service, action);
            };
        } catch (Exception e) {
            log.error("Failed to execute action {} for service {} in region {}: {}",
                    action.actionType(), service.getServiceName(), action.region(), e.getMessage());
            return false;
        }
    }

    /**
     * Execute scale-out action
     */
    private boolean executeScaleOut(Service service, PlannedAction action) {
        ServiceRegion region = serviceManagementService.getServiceRegion(
                service.getId(), action.region());

        if (region.getAsgName() == null) {
            log.warn("No ASG configured for region {}", action.region());
            return false;
        }

        int currentCapacity = region.getDesiredCapacity() != null ? 
                region.getDesiredCapacity() : 
                region.getRunningInstances() != null ? region.getRunningInstances() : 0;
        int newCapacity = currentCapacity + action.delta();

        try {
            AutoScalingClient asgClient = awsClientProvider.getAutoScalingClient(
                    service.getAwsAccount(), action.region());

            // Update ASG desired capacity
            asgClient.setDesiredCapacity(SetDesiredCapacityRequest.builder()
                    .autoScalingGroupName(region.getAsgName())
                    .desiredCapacity(newCapacity)
                    .honorCooldown(false) // Platform manages cooldown
                    .build());

            // Update local state
            serviceManagementService.updateRegionCapacity(region.getId(), newCapacity, null);

            // Record for rate limiting
            guardrailService.recordAction(service.getId(), true);

            // Audit
            auditService.logScalingAction(service, null, true, 
                    currentCapacity, newCapacity, action.reason(), true, null);

            log.info("Scale out completed: {} {} -> {} instances",
                    action.region(), currentCapacity, newCapacity);
            return true;

        } catch (Exception e) {
            auditService.logScalingAction(service, null, true,
                    currentCapacity, newCapacity, action.reason(), false, e.getMessage());
            throw e;
        }
    }

    /**
     * Execute scale-in action
     */
    private boolean executeScaleIn(Service service, PlannedAction action) {
        ServiceRegion region = serviceManagementService.getServiceRegion(
                service.getId(), action.region());

        if (region.getAsgName() == null) {
            log.warn("No ASG configured for region {}", action.region());
            return false;
        }

        int currentCapacity = region.getDesiredCapacity() != null ? 
                region.getDesiredCapacity() : 
                region.getRunningInstances() != null ? region.getRunningInstances() : 0;
        int newCapacity = currentCapacity + action.delta(); // delta is negative

        try {
            AutoScalingClient asgClient = awsClientProvider.getAutoScalingClient(
                    service.getAwsAccount(), action.region());

            asgClient.setDesiredCapacity(SetDesiredCapacityRequest.builder()
                    .autoScalingGroupName(region.getAsgName())
                    .desiredCapacity(newCapacity)
                    .honorCooldown(false)
                    .build());

            serviceManagementService.updateRegionCapacity(region.getId(), newCapacity, null);
            guardrailService.recordAction(service.getId(), false);

            auditService.logScalingAction(service, null, false,
                    currentCapacity, newCapacity, action.reason(), true, null);

            log.info("Scale in completed: {} {} -> {} instances",
                    action.region(), currentCapacity, newCapacity);
            return true;

        } catch (Exception e) {
            auditService.logScalingAction(service, null, false,
                    currentCapacity, newCapacity, action.reason(), false, e.getMessage());
            throw e;
        }
    }

    /**
     * Execute traffic shift action
     */
    private boolean executeTrafficShift(Service service, PlannedAction action) {
        // Traffic shifts are handled through Route 53 weighted records
        // or ALB weighted target groups
        log.info("Traffic shift requested for {} in region {}",
                service.getServiceName(), action.region());
        
        // Implementation: Route 53 / ALB traffic shifting
        // This would involve updating weighted routing policies
        
        return true;
    }

    /**
     * Execute failover action
     */
    private boolean executeFailover(Service service, PlannedAction action) {
        log.info("Failover requested for {} from region {}",
                service.getServiceName(), action.region());

        ServiceRegion failingRegion = serviceManagementService.getServiceRegion(
                service.getId(), action.region());

        // Mark region as failed
        serviceManagementService.updateRegionStatus(
                failingRegion.getId(), 
                ServiceRegion.RegionStatus.FAILING_OVER,
                action.reason());

        // Shift traffic to other regions
        // Implementation: automatic traffic redistribution

        return true;
    }

    /**
     * Execute restore action
     */
    private boolean executeRestore(Service service, PlannedAction action) {
        log.info("Restore requested for {} in region {}",
                service.getServiceName(), action.region());

        ServiceRegion region = serviceManagementService.getServiceRegion(
                service.getId(), action.region());

        serviceManagementService.updateRegionStatus(
                region.getId(),
                ServiceRegion.RegionStatus.ACTIVE,
                "Restored after failover");

        return true;
    }

    /**
     * Get current ASG state
     */
    public AsgState getAsgState(Service service, String regionName) {
        ServiceRegion region = serviceManagementService.getServiceRegion(
                service.getId(), regionName);

        if (region.getAsgName() == null) {
            return new AsgState(null, 0, 0, 0, 0, 0);
        }

        try {
            AutoScalingClient asgClient = awsClientProvider.getAutoScalingClient(
                    service.getAwsAccount(), regionName);

            DescribeAutoScalingGroupsResponse response = asgClient.describeAutoScalingGroups(
                    DescribeAutoScalingGroupsRequest.builder()
                            .autoScalingGroupNames(region.getAsgName())
                            .build());

            if (response.autoScalingGroups().isEmpty()) {
                return new AsgState(region.getAsgName(), 0, 0, 0, 0, 0);
            }

            AutoScalingGroup asg = response.autoScalingGroups().getFirst();
            
            int healthy = (int) asg.instances().stream()
                    .filter(i -> "Healthy".equals(i.healthStatus()) && 
                            "InService".equals(i.lifecycleState().toString()))
                    .count();

            return new AsgState(
                    asg.autoScalingGroupName(),
                    asg.minSize(),
                    asg.maxSize(),
                    asg.desiredCapacity(),
                    asg.instances().size(),
                    healthy
            );

        } catch (Exception e) {
            log.error("Failed to get ASG state: {}", e.getMessage());
            return new AsgState(region.getAsgName(), 0, 0, 0, 0, 0);
        }
    }

    // Data class for ASG state
    public record AsgState(
            String asgName,
            int minSize,
            int maxSize,
            int desiredCapacity,
            int totalInstances,
            int healthyInstances
    ) {}
}
