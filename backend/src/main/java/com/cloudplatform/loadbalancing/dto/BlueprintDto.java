package com.cloudplatform.loadbalancing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for Blueprint operations
 */
public class BlueprintDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotNull(message = "AWS Account ID is required")
        private UUID awsAccountId;
        
        @NotBlank(message = "Blueprint name is required")
        @Size(max = 100, message = "Name must be less than 100 characters")
        private String name;
        
        @Size(max = 500, message = "Description must be less than 500 characters")
        private String description;
        
        @NotBlank(message = "AMI ID is required")
        private String amiId;
        
        @NotBlank(message = "AMI region is required")
        private String amiRegion;
        
        @NotBlank(message = "Instance type is required")
        private String instanceType;
        
        private String instanceProfileArn;
        private String iamInstanceProfile;
        private String securityGroupIds;
        private String subnetIds;
        private String keyName;
        private String userData;
        private Map<String, String> requiredTags;
        private Map<String, String> tags;
        private Map<String, Object> launchTemplateConfig;
        
        private String healthCheckPath;
        private Integer healthCheckIntervalSeconds;
        private Integer drainTimeSeconds;
        private Integer startupTimeSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @Size(max = 100, message = "Name must be less than 100 characters")
        private String name;
        
        @Size(max = 500, message = "Description must be less than 500 characters")
        private String description;
        
        private String amiId;
        private String instanceType;
        private String instanceProfileArn;
        private String iamInstanceProfile;
        private String securityGroupIds;
        private String subnetIds;
        private String keyName;
        private String userData;
        private Map<String, String> requiredTags;
        private Map<String, String> tags;
        private Map<String, Object> launchTemplateConfig;
        
        private String healthCheckPath;
        private Integer healthCheckIntervalSeconds;
        private Integer drainTimeSeconds;
        private Integer startupTimeSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID awsAccountId;
        private String awsAccountName;
        private String name;
        private String description;
        private Integer version;
        private String amiId;
        private String amiRegion;
        private String instanceType;
        private String instanceProfileArn;
        private String iamInstanceProfile;
        private List<String> securityGroupIds;
        private List<String> subnetIds;
        private String keyName;
        private Map<String, String> requiredTags;
        private Map<String, Object> launchTemplateConfig;
        private String launchTemplateId;
        private String healthCheckPath;
        private Integer healthCheckIntervalSeconds;
        private Integer drainTimeSeconds;
        private Integer startupTimeSeconds;
        private String status;
        private UUID createdBy;
        private UUID approvedBy;
        private Instant approvedAt;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalRequest {
        private String approvalComments;
    }
}
