package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.dto.PolicyDto;
import com.cloudplatform.loadbalancing.service.PolicyService;
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
 * Policy Controller
 * 
 * Manages SLO Policies that define scaling rules, cost limits,
 * and deployment strategies.
 */
@RestController
@RequestMapping("/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Policies", description = "SLO Policy management")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @Operation(summary = "Create a new policy")
    public ResponseEntity<PolicyDto.Response> createPolicy(
            @Valid @RequestBody PolicyDto.CreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        PolicyDto.Response response = policyService.createPolicy(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{policyId}")
    @Operation(summary = "Get policy by ID")
    public ResponseEntity<PolicyDto.Response> getPolicy(@PathVariable UUID policyId) {
        return ResponseEntity.ok(policyService.getPolicy(policyId));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get all policies for an account")
    public ResponseEntity<List<PolicyDto.Response>> getPoliciesByAccount(
            @PathVariable UUID accountId) {
        return ResponseEntity.ok(policyService.getPoliciesByAccount(accountId));
    }

    @GetMapping("/account/{accountId}/active")
    @Operation(summary = "Get active policies for an account")
    public ResponseEntity<List<PolicyDto.Response>> getActivePoliciesByAccount(
            @PathVariable UUID accountId) {
        return ResponseEntity.ok(policyService.getActivePoliciesByAccount(accountId));
    }

    @PutMapping("/{policyId}")
    @Operation(summary = "Update a policy (creates new version if active)")
    public ResponseEntity<PolicyDto.Response> updatePolicy(
            @PathVariable UUID policyId,
            @Valid @RequestBody PolicyDto.UpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        return ResponseEntity.ok(policyService.updatePolicy(policyId, request, userId));
    }

    @PostMapping("/{policyId}/approve")
    @Operation(summary = "Approve a policy for use")
    public ResponseEntity<PolicyDto.Response> approvePolicy(
            @PathVariable UUID policyId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        return ResponseEntity.ok(policyService.approvePolicy(policyId, userId));
    }

    @DeleteMapping("/{policyId}")
    @Operation(summary = "Delete a draft policy")
    public ResponseEntity<Void> deletePolicy(
            @PathVariable UUID policyId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        policyService.deletePolicy(policyId, userId);
        return ResponseEntity.noContent().build();
    }
}

