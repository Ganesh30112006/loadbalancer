package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.dto.AccountDto;
import com.cloudplatform.loadbalancing.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * AWS Account Controller
 * 
 * Manages AWS account onboarding, validation, and lifecycle.
 */
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "AWS Accounts", description = "AWS Account onboarding and management")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Onboard a new AWS account")
    public ResponseEntity<AccountDto.OnboardingResponse> onboardAccount(
            @Valid @RequestBody AccountDto.CreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        AccountDto.OnboardingResponse response = accountService.onboardAccount(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{accountId}/validate")
    @Operation(summary = "Validate AWS account credentials")
    public ResponseEntity<AccountDto.ValidationResult> validateAccount(
            @PathVariable UUID accountId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        AccountDto.ValidationResult result = accountService.validateAccount(accountId, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountDto.Response> getAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    @GetMapping
    @Operation(summary = "Get all accounts")
    public ResponseEntity<List<AccountDto.Response>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active accounts only")
    public ResponseEntity<List<AccountDto.Response>> getActiveAccounts() {
        return ResponseEntity.ok(accountService.getActiveAccounts());
    }

    @PutMapping("/{accountId}")
    @Operation(summary = "Update account")
    public ResponseEntity<AccountDto.Response> updateAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody AccountDto.UpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        return ResponseEntity.ok(accountService.updateAccount(accountId, request, userId));
    }

    @PostMapping("/{accountId}/suspend")
    @Operation(summary = "Suspend account")
    public ResponseEntity<Void> suspendAccount(
            @PathVariable UUID accountId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        accountService.suspendAccount(accountId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/external-id/generate")
    @Operation(summary = "Generate a new External ID for account setup")
    public ResponseEntity<String> generateExternalId() {
        return ResponseEntity.ok(accountService.generateExternalId());
    }
}

