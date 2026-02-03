package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.aws.AwsClientFactory;
import com.cloudplatform.loadbalancing.dto.DiscoveredResourceDto;
import com.cloudplatform.loadbalancing.entity.AwsAccount;
import com.cloudplatform.loadbalancing.entity.DiscoveredResource;
import com.cloudplatform.loadbalancing.repository.AwsAccountRepository;
import com.cloudplatform.loadbalancing.repository.DiscoveredResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplatesRequest;
import software.amazon.awssdk.services.ec2.model.LaunchTemplate;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ResourceDiscoveryService {

    private final AwsAccountRepository accountRepository;
    private final DiscoveredResourceRepository resourceRepository;
    private final AwsClientFactory awsClientFactory;

    private static final List<String> COMMON_REGIONS = List.of(
            "us-east-1", "us-east-2", "us-west-1", "us-west-2",
            "eu-west-1", "eu-west-2", "eu-central-1",
            "ap-south-1", "ap-southeast-1", "ap-southeast-2", "ap-northeast-1"
    );

    public List<DiscoveredResourceDto> getDiscoveredResources(UUID accountId) {
        return resourceRepository.findByAwsAccountId(accountId).stream()
                .map(DiscoveredResourceDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<DiscoveredResourceDto> getDiscoveredResourcesByType(UUID accountId, DiscoveredResource.ResourceType type) {
        return resourceRepository.findByAwsAccountIdAndResourceType(accountId, type).stream()
                .map(DiscoveredResourceDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<String> getDiscoveredRegions(UUID accountId) {
        return resourceRepository.findDistinctRegionsByAccountId(accountId);
    }

    @Async
    @Transactional
    public void discoverResources(UUID accountId) {
        log.info("Starting resource discovery for account: {}", accountId);
        
        AwsAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        List<String> regionsToScan = account.getDefaultRegion() != null 
                ? List.of(account.getDefaultRegion())
                : COMMON_REGIONS;

        for (String region : regionsToScan) {
            try {
                discoverResourcesInRegion(account, region);
            } catch (Exception e) {
                log.warn("Failed to discover resources in region {} for account {}: {}", 
                        region, accountId, e.getMessage());
            }
        }

        log.info("Resource discovery completed for account: {}", accountId);
    }

    private void discoverResourcesInRegion(AwsAccount account, String region) {
        log.debug("Discovering resources in region: {} for account: {}", region, account.getId());

        // Discover Auto Scaling Groups
        try {
            discoverAutoScalingGroups(account, region);
        } catch (Exception e) {
            log.warn("Failed to discover ASGs in {}: {}", region, e.getMessage());
        }

        // Discover Launch Templates
        try {
            discoverLaunchTemplates(account, region);
        } catch (Exception e) {
            log.warn("Failed to discover Launch Templates in {}: {}", region, e.getMessage());
        }

        // Discover Load Balancers
        try {
            discoverLoadBalancers(account, region);
        } catch (Exception e) {
            log.warn("Failed to discover ALBs in {}: {}", region, e.getMessage());
        }

        // Discover Target Groups
        try {
            discoverTargetGroups(account, region);
        } catch (Exception e) {
            log.warn("Failed to discover Target Groups in {}: {}", region, e.getMessage());
        }
    }

    private void discoverAutoScalingGroups(AwsAccount account, String region) {
        try (AutoScalingClient asgClient = awsClientFactory.getAutoScalingClient(
                account.getAccountId(), account.getRoleArn(), account.getExternalId(), region)) {
            
            var response = asgClient.describeAutoScalingGroups(
                    DescribeAutoScalingGroupsRequest.builder().build());

            for (AutoScalingGroup asg : response.autoScalingGroups()) {
                saveOrUpdateResource(account, region, DiscoveredResource.ResourceType.AUTO_SCALING_GROUP,
                        asg.autoScalingGroupName(), asg.autoScalingGroupARN(), asg.autoScalingGroupName(),
                        asg.desiredCapacity(), asg.minSize(), asg.maxSize());
            }
        }
    }

    private void discoverLaunchTemplates(AwsAccount account, String region) {
        try (Ec2Client ec2Client = awsClientFactory.getEc2Client(
                account.getAccountId(), account.getRoleArn(), account.getExternalId(), region)) {
            
            var response = ec2Client.describeLaunchTemplates(
                    DescribeLaunchTemplatesRequest.builder().build());

            for (LaunchTemplate lt : response.launchTemplates()) {
                String arn = String.format("arn:aws:ec2:%s:%s:launch-template/%s",
                        region, account.getAccountId(), lt.launchTemplateId());
                saveOrUpdateResource(account, region, DiscoveredResource.ResourceType.LAUNCH_TEMPLATE,
                        lt.launchTemplateId(), arn, lt.launchTemplateName(),
                        null, null, null);
            }
        }
    }

    private void discoverLoadBalancers(AwsAccount account, String region) {
        try (ElasticLoadBalancingV2Client elbClient = awsClientFactory.getElbClient(
                account.getAccountId(), account.getRoleArn(), account.getExternalId(), region)) {
            
            var response = elbClient.describeLoadBalancers(
                    DescribeLoadBalancersRequest.builder().build());

            for (LoadBalancer lb : response.loadBalancers()) {
                if ("application".equals(lb.typeAsString())) {
                    saveOrUpdateResource(account, region, DiscoveredResource.ResourceType.APPLICATION_LOAD_BALANCER,
                            lb.loadBalancerName(), lb.loadBalancerArn(), lb.loadBalancerName(),
                            null, null, null);
                }
            }
        }
    }

    private void discoverTargetGroups(AwsAccount account, String region) {
        try (ElasticLoadBalancingV2Client elbClient = awsClientFactory.getElbClient(
                account.getAccountId(), account.getRoleArn(), account.getExternalId(), region)) {
            
            var response = elbClient.describeTargetGroups(
                    DescribeTargetGroupsRequest.builder().build());

            for (TargetGroup tg : response.targetGroups()) {
                saveOrUpdateResource(account, region, DiscoveredResource.ResourceType.TARGET_GROUP,
                        tg.targetGroupName(), tg.targetGroupArn(), tg.targetGroupName(),
                        null, null, null);
            }
        }
    }

    @Transactional
    protected void saveOrUpdateResource(AwsAccount account, String region, 
                                         DiscoveredResource.ResourceType type,
                                         String resourceId, String resourceArn, String resourceName,
                                         Integer currentCapacity, Integer minCapacity, Integer maxCapacity) {
        
        DiscoveredResource resource = resourceRepository
                .findByAwsAccountIdAndResourceArn(account.getId(), resourceArn)
                .orElse(DiscoveredResource.builder()
                        .awsAccount(account)
                        .resourceType(type)
                        .resourceId(resourceId)
                        .resourceArn(resourceArn)
                        .resourceName(resourceName)
                        .region(region)
                        .adoptionStatus(DiscoveredResource.AdoptionStatus.DISCOVERED)
                        .build());

        resource.setCurrentCapacity(currentCapacity);
        resource.setMinCapacity(minCapacity);
        resource.setMaxCapacity(maxCapacity);
        resource.setLastSyncedAt(Instant.now());

        resourceRepository.save(resource);
        log.debug("Saved/updated resource: {} in {}", resourceName, region);
    }

    @Transactional
    public DiscoveredResourceDto updateAdoptionStatus(UUID resourceId, DiscoveredResource.AdoptionStatus status) {
        DiscoveredResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
        
        resource.setAdoptionStatus(status);
        resource = resourceRepository.save(resource);
        
        log.info("Resource {} adoption status updated to {}", resource.getResourceName(), status);
        return DiscoveredResourceDto.fromEntity(resource);
    }
}
