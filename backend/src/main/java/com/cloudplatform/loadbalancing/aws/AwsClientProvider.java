package com.cloudplatform.loadbalancing.aws;

import com.cloudplatform.loadbalancing.entity.AwsAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.route53.Route53Client;

/**
 * AWS Client Provider
 * 
 * Provides AWS SDK clients for a specific AWS account.
 * Simplifies client creation by using account entity directly.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AwsClientProvider {

    private final AwsClientFactory clientFactory;

    /**
     * Create EC2 client for an AWS account
     */
    public Ec2Client ec2(AwsAccount account, String region) {
        return clientFactory.createEc2Client(
                account.getAccountId(),
                account.getRoleArn(),
                account.getExternalId(),
                region
        );
    }

    /**
     * Alias method for EC2 client
     */
    public Ec2Client getEc2Client(AwsAccount account, String region) {
        return ec2(account, region);
    }

    /**
     * Create Auto Scaling client for an AWS account
     */
    public AutoScalingClient autoScaling(AwsAccount account, String region) {
        return clientFactory.createAutoScalingClient(
                account.getAccountId(),
                account.getRoleArn(),
                account.getExternalId(),
                region
        );
    }

    /**
     * Alias method for Auto Scaling client
     */
    public AutoScalingClient getAutoScalingClient(AwsAccount account, String region) {
        return autoScaling(account, region);
    }

    /**
     * Create ELB V2 client for an AWS account
     */
    public ElasticLoadBalancingV2Client elb(AwsAccount account, String region) {
        return clientFactory.createElbClient(
                account.getAccountId(),
                account.getRoleArn(),
                account.getExternalId(),
                region
        );
    }

    /**
     * Create CloudWatch client for an AWS account
     */
    public CloudWatchClient cloudWatch(AwsAccount account, String region) {
        return clientFactory.createCloudWatchClient(
                account.getAccountId(),
                account.getRoleArn(),
                account.getExternalId(),
                region
        );
    }

    /**
     * Alias method for CloudWatch client
     */
    public CloudWatchClient getCloudWatchClient(AwsAccount account, String region) {
        return cloudWatch(account, region);
    }

    /**
     * Create Route 53 client for an AWS account
     */
    public Route53Client route53(AwsAccount account) {
        return clientFactory.createRoute53Client(
                account.getAccountId(),
                account.getRoleArn(),
                account.getExternalId()
        );
    }

    /**
     * Create Cost Explorer client for an AWS account
     */
    public CostExplorerClient costExplorer(AwsAccount account) {
        return clientFactory.createCostExplorerClient(
                account.getAccountId(),
                account.getRoleArn(),
                account.getExternalId()
        );
    }

    /**
     * Validate credentials for an AWS account
     */
    public boolean validateCredentials(AwsAccount account) {
        return clientFactory.validateCredentials(
                account.getAccountId(),
                account.getRoleArn(),
                account.getExternalId()
        );
    }

    /**
     * Invalidate cached credentials for an account
     */
    public void invalidateCredentials(AwsAccount account) {
        clientFactory.invalidateCredentials(account.getAccountId(), account.getRoleArn());
    }
}
