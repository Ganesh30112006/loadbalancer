package com.cloudplatform.loadbalancing.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AWS Client Factory
 * 
 * Creates AWS SDK clients using STS AssumeRole credentials.
 * Manages credential caching and client lifecycle.
 */
@Component
@Slf4j
public class AwsClientFactory {

    @Value("${aws.region:us-east-1}")
    private String defaultRegion;

    @Value("${aws.sts.session-duration-seconds:3600}")
    private int sessionDurationSeconds;

    @Value("${aws.sts.session-name-prefix:LoadBalancingPlatform}")
    private String sessionNamePrefix;

    private final StsClient stsClient;
    
    // Cache for credential providers (key: accountId:roleArn)
    private final Map<String, StsAssumeRoleCredentialsProvider> credentialProviderCache = new ConcurrentHashMap<>();

    public AwsClientFactory() {
        // STS client uses platform's default credentials (IAM role)
        // In development mode, this may fail if AWS credentials are not configured
        StsClient tempClient = null;
        try {
            tempClient = StsClient.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
            log.info("AWS STS client initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize AWS STS client - running in development mode without AWS credentials", e);
        }
        this.stsClient = tempClient;
    }

    /**
     * Get or create credential provider for an AWS account
     * 
     * @param accountId AWS account ID
     * @param roleArn IAM role ARN to assume
     * @param externalId Platform-generated external ID
     * @return AWS credentials provider
     */
    public AwsCredentialsProvider getCredentialsProvider(String accountId, String roleArn, String externalId) {
        if (stsClient == null) {
            log.warn("Cannot create credentials provider - STS client not initialized (running in development mode)");
            return DefaultCredentialsProvider.create();
        }
        
        String cacheKey = accountId + ":" + roleArn;
        
        return credentialProviderCache.computeIfAbsent(cacheKey, key -> {
            log.info("Creating STS AssumeRole credentials provider for account: {}", accountId);
            
            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName(sessionNamePrefix + "-" + accountId)
                    .externalId(externalId)
                    .durationSeconds(sessionDurationSeconds)
                    .build();

            return StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(assumeRoleRequest)
                    .staleTime(Duration.ofMinutes(5))
                    .prefetchTime(Duration.ofMinutes(10))
                    .build();
        });
    }

    /**
     * Invalidate cached credentials for an account
     */
    public void invalidateCredentials(String accountId, String roleArn) {
        String cacheKey = accountId + ":" + roleArn;
        StsAssumeRoleCredentialsProvider provider = credentialProviderCache.remove(cacheKey);
        if (provider != null) {
            provider.close();
            log.info("Invalidated credentials for account: {}", accountId);
        }
    }

    /**
     * Create EC2 client for a specific account and region
     */
    public Ec2Client createEc2Client(String accountId, String roleArn, String externalId, String region) {
        return Ec2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(getCredentialsProvider(accountId, roleArn, externalId))
                .build();
    }

    /**
     * Get EC2 client (alias for createEc2Client)
     */
    public Ec2Client getEc2Client(String accountId, String roleArn, String externalId, String region) {
        return createEc2Client(accountId, roleArn, externalId, region);
    }

    /**
     * Create Auto Scaling client for a specific account and region
     */
    public AutoScalingClient createAutoScalingClient(String accountId, String roleArn, String externalId, String region) {
        return AutoScalingClient.builder()
                .region(Region.of(region))
                .credentialsProvider(getCredentialsProvider(accountId, roleArn, externalId))
                .build();
    }

    /**
     * Get Auto Scaling client (alias for createAutoScalingClient)
     */
    public AutoScalingClient getAutoScalingClient(String accountId, String roleArn, String externalId, String region) {
        return createAutoScalingClient(accountId, roleArn, externalId, region);
    }

    /**
     * Create Elastic Load Balancing V2 client for a specific account and region
     */
    public ElasticLoadBalancingV2Client createElbClient(String accountId, String roleArn, String externalId, String region) {
        return ElasticLoadBalancingV2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(getCredentialsProvider(accountId, roleArn, externalId))
                .build();
    }

    /**
     * Get ELB client (alias for createElbClient)
     */
    public ElasticLoadBalancingV2Client getElbClient(String accountId, String roleArn, String externalId, String region) {
        return createElbClient(accountId, roleArn, externalId, region);
    }

    /**
     * Create CloudWatch client for a specific account and region
     */
    public CloudWatchClient createCloudWatchClient(String accountId, String roleArn, String externalId, String region) {
        return CloudWatchClient.builder()
                .region(Region.of(region))
                .credentialsProvider(getCredentialsProvider(accountId, roleArn, externalId))
                .build();
    }

    /**
     * Create Route 53 client for a specific account (global service)
     */
    public Route53Client createRoute53Client(String accountId, String roleArn, String externalId) {
        return Route53Client.builder()
                .region(Region.US_EAST_1) // Route 53 is global, uses us-east-1
                .credentialsProvider(getCredentialsProvider(accountId, roleArn, externalId))
                .build();
    }

    /**
     * Create Cost Explorer client for a specific account (global service)
     */
    public CostExplorerClient createCostExplorerClient(String accountId, String roleArn, String externalId) {
        return CostExplorerClient.builder()
                .region(Region.US_EAST_1) // Cost Explorer uses us-east-1
                .credentialsProvider(getCredentialsProvider(accountId, roleArn, externalId))
                .build();
    }

    /**
     * Validate credentials by attempting to get caller identity
     * 
     * @return true if credentials are valid
     */
    public boolean validateCredentials(String accountId, String roleArn, String externalId) {
        if (stsClient == null) {
            log.warn("Cannot validate credentials - STS client not initialized (running in development mode)");
            return false;
        }
        try {
            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName(sessionNamePrefix + "-validate-" + accountId)
                    .externalId(externalId)
                    .durationSeconds(900) // 15 minutes for validation
                    .build();

            var response = stsClient.assumeRole(assumeRoleRequest);
            log.info("Validated credentials for account: {}, assumed role: {}", 
                    accountId, response.assumedRoleUser().arn());
            return true;
        } catch (Exception e) {
            log.error("Failed to validate credentials for account: {}, error: {}", 
                    accountId, e.getMessage());
            return false;
        }
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }
}
