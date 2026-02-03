package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.aws.AwsClientFactory;
import com.cloudplatform.loadbalancing.dto.AccountDto;
import com.cloudplatform.loadbalancing.entity.AwsAccount;
import com.cloudplatform.loadbalancing.exception.AccountNotFoundException;
import com.cloudplatform.loadbalancing.exception.AccountValidationException;
import com.cloudplatform.loadbalancing.exception.DuplicateAccountException;
import com.cloudplatform.loadbalancing.repository.AwsAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AWS Account Service
 * 
 * Manages AWS account onboarding, External ID generation,
 * credential validation, and account lifecycle.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AccountService {

    private final AwsAccountRepository accountRepository;
    private final AwsClientFactory awsClientFactory;
    private final AuditService auditService;

    /**
     * Generate a unique External ID for a new AWS account
     * External ID is platform-generated and used in STS AssumeRole
     */
    public String generateExternalId() {
        String externalId;
        do {
            externalId = "ext-" + UUID.randomUUID().toString();
        } while (accountRepository.existsByExternalId(externalId));
        return externalId;
    }

    /**
     * Onboard a new AWS account
     */
    @Transactional
    public AccountDto.OnboardingResponse onboardAccount(AccountDto.CreateRequest request, UUID userId) {
        log.info("Onboarding AWS account: {}", request.getAccountId());

        // Check for duplicate account
        if (accountRepository.existsByAccountId(request.getAccountId())) {
            throw new DuplicateAccountException("AWS account already exists: " + request.getAccountId());
        }

        // Generate External ID
        String externalId = generateExternalId();

        // Create account entity
        AwsAccount account = AwsAccount.builder()
                .accountId(request.getAccountId())
                .accountName(request.getAccountName())
                .roleArn(request.getRoleArn())
                .externalId(externalId)
                .status(AwsAccount.AccountStatus.PENDING_VALIDATION)
                .enabledRegions(request.getEnabledRegions() != null ? 
                        String.join(",", request.getEnabledRegions()) : "us-east-1")
                .createdBy(userId)
                .build();

        account = accountRepository.save(account);

        // Log audit
        auditService.logAccountAction(account, "ACCOUNT_ONBOARDED", 
                "Account onboarded, pending validation", userId);

        // Generate trust policy template
        String trustPolicyTemplate = generateTrustPolicyTemplate(externalId);
        String setupInstructions = generateSetupInstructions(request.getAccountId(), externalId);

        return AccountDto.OnboardingResponse.builder()
                .id(account.getId())
                .accountId(account.getAccountId())
                .accountName(account.getAccountName())
                .externalId(externalId)
                .trustPolicyTemplate(trustPolicyTemplate)
                .iamRoleSetupInstructions(setupInstructions)
                .build();
    }

    /**
     * Validate AWS account credentials
     */
    @Transactional
    public AccountDto.ValidationResult validateAccount(UUID accountId, UUID userId) {
        AwsAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        log.info("Validating credentials for account: {}", account.getAccountId());

        try {
            boolean valid = awsClientFactory.validateCredentials(
                    account.getAccountId(),
                    account.getRoleArn(),
                    account.getExternalId()
            );

            if (valid) {
                account.setStatus(AwsAccount.AccountStatus.ACTIVE);
                account.setLastValidatedAt(Instant.now());
                account.setValidationError(null);
                accountRepository.save(account);

                auditService.logAccountAction(account, "ACCOUNT_VALIDATED", 
                        "Credentials validated successfully", userId);

                return AccountDto.ValidationResult.builder()
                        .valid(true)
                        .accountId(account.getAccountId())
                        .assumedRoleArn(account.getRoleArn())
                        .validatedAt(Instant.now())
                        .build();
            } else {
                throw new AccountValidationException("Credential validation returned false");
            }

        } catch (Exception e) {
            log.error("Credential validation failed for account: {}", account.getAccountId(), e);

            account.setStatus(AwsAccount.AccountStatus.VALIDATION_FAILED);
            account.setValidationError(e.getMessage());
            accountRepository.save(account);

            auditService.logAccountAction(account, "ACCOUNT_VALIDATION_FAILED", 
                    "Validation failed: " + e.getMessage(), userId);

            return AccountDto.ValidationResult.builder()
                    .valid(false)
                    .accountId(account.getAccountId())
                    .errorMessage(e.getMessage())
                    .validatedAt(Instant.now())
                    .build();
        }
    }

    /**
     * Get account by ID
     */
    @Transactional(readOnly = true)
    public AccountDto.Response getAccount(UUID accountId) {
        AwsAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        return mapToResponse(account);
    }

    /**
     * Get all accounts
     */
    @Transactional(readOnly = true)
    public List<AccountDto.Response> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active accounts
     */
    @Transactional(readOnly = true)
    public List<AccountDto.Response> getActiveAccounts() {
        return accountRepository.findAllActiveAccounts().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update account
     */
    @Transactional
    public AccountDto.Response updateAccount(UUID accountId, AccountDto.UpdateRequest request, UUID userId) {
        AwsAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        if (request.getAccountName() != null) {
            account.setAccountName(request.getAccountName());
        }
        if (request.getRoleArn() != null) {
            account.setRoleArn(request.getRoleArn());
            account.setStatus(AwsAccount.AccountStatus.PENDING_VALIDATION);
            // Invalidate cached credentials
            awsClientFactory.invalidateCredentials(account.getAccountId(), account.getRoleArn());
        }
        if (request.getEnabledRegions() != null) {
            account.setEnabledRegions(String.join(",", request.getEnabledRegions()));
        }

        account = accountRepository.save(account);
        auditService.logAccountAction(account, "ACCOUNT_UPDATED", "Account updated", userId);

        return mapToResponse(account);
    }

    /**
     * Suspend account
     */
    @Transactional
    public void suspendAccount(UUID accountId, UUID userId) {
        AwsAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        account.setStatus(AwsAccount.AccountStatus.SUSPENDED);
        accountRepository.save(account);

        // Invalidate credentials
        awsClientFactory.invalidateCredentials(account.getAccountId(), account.getRoleArn());

        auditService.logAccountAction(account, "ACCOUNT_SUSPENDED", "Account suspended", userId);
        log.info("Suspended account: {}", account.getAccountId());
    }

    /**
     * Get account entity by ID
     */
    @Transactional(readOnly = true)
    public AwsAccount getAccountEntity(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    private AccountDto.Response mapToResponse(AwsAccount account) {
        return AccountDto.Response.builder()
                .id(account.getId())
                .accountId(account.getAccountId())
                .accountName(account.getAccountName())
                .roleArn(account.getRoleArn())
                .externalId(account.getExternalId())
                .status(account.getStatus().name())
                .enabledRegions(account.getEnabledRegions() != null ? 
                        Arrays.asList(account.getEnabledRegions().split(",")) : List.of())
                .lastValidatedAt(account.getLastValidatedAt())
                .validationError(account.getValidationError())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private String generateTrustPolicyTemplate(String externalId) {
        return """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": {
                    "AWS": "arn:aws:iam::PLATFORM_ACCOUNT_ID:root"
                  },
                  "Action": "sts:AssumeRole",
                  "Condition": {
                    "StringEquals": {
                      "sts:ExternalId": "%s"
                    }
                  }
                }
              ]
            }
            """.formatted(externalId);
    }

    private String generateSetupInstructions(String accountId, String externalId) {
        return """
            # IAM Role Setup Instructions
            
            1. Sign in to the AWS Console for account %s
            2. Navigate to IAM > Roles > Create Role
            3. Select "Another AWS account" as the trusted entity
            4. Enter the Platform Account ID
            5. Check "Require external ID" and enter: %s
            6. Attach the required policies (EC2, ASG, ELB, CloudWatch, Route 53)
            7. Name the role and note the ARN
            8. Return to this platform and validate the credentials
            
            Required IAM Permissions:
            - ec2:Describe*
            - ec2:CreateTags
            - autoscaling:*
            - elasticloadbalancing:*
            - cloudwatch:GetMetricData
            - cloudwatch:GetMetricStatistics
            - cloudwatch:ListMetrics
            - route53:*
            - ce:GetCostAndUsage
            """.formatted(accountId, externalId);
    }
}
