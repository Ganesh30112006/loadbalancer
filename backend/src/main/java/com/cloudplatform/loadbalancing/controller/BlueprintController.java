package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.dto.BlueprintDto;
import com.cloudplatform.loadbalancing.service.BlueprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Blueprint Controller
 * 
 * Manages Application Blueprints - reusable infrastructure templates.
 */
@RestController
@RequestMapping("/v1/blueprints")
@RequiredArgsConstructor
@Tag(name = "Blueprints", description = "Application Blueprint management")
public class BlueprintController {

    private final BlueprintService blueprintService;

    @PostMapping
    @Operation(summary = "Create a new blueprint")
    public ResponseEntity<BlueprintDto.Response> createBlueprint(
            @Valid @RequestBody BlueprintDto.CreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        BlueprintDto.Response response = blueprintService.createBlueprint(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{blueprintId}")
    @Operation(summary = "Get blueprint by ID")
    public ResponseEntity<BlueprintDto.Response> getBlueprint(@PathVariable UUID blueprintId) {
        return ResponseEntity.ok(blueprintService.getBlueprint(blueprintId));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get all blueprints for an account")
    public ResponseEntity<List<BlueprintDto.Response>> getBlueprintsByAccount(
            @PathVariable UUID accountId) {
        return ResponseEntity.ok(blueprintService.getBlueprintsByAccount(accountId));
    }

    @GetMapping("/account/{accountId}/active")
    @Operation(summary = "Get active blueprints for an account")
    public ResponseEntity<List<BlueprintDto.Response>> getActiveBlueprintsByAccount(
            @PathVariable UUID accountId) {
        return ResponseEntity.ok(blueprintService.getActiveBlueprintsByAccount(accountId));
    }

    @PutMapping("/{blueprintId}")
    @Operation(summary = "Update a blueprint (creates new version if active)")
    public ResponseEntity<BlueprintDto.Response> updateBlueprint(
            @PathVariable UUID blueprintId,
            @Valid @RequestBody BlueprintDto.UpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        return ResponseEntity.ok(blueprintService.updateBlueprint(blueprintId, request, userId));
    }

    @PostMapping("/{blueprintId}/approve")
    @Operation(summary = "Approve a blueprint for use")
    public ResponseEntity<BlueprintDto.Response> approveBlueprint(
            @PathVariable UUID blueprintId,
            @RequestBody(required = false) BlueprintDto.ApprovalRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        return ResponseEntity.ok(blueprintService.approveBlueprint(blueprintId, request, userId));
    }

    @PostMapping("/{blueprintId}/validate-ami")
    @Operation(summary = "Validate that the AMI exists in a region")
    public ResponseEntity<Map<String, Object>> validateAmi(
            @PathVariable UUID blueprintId,
            @RequestParam String region) {
        
        return ResponseEntity.ok(blueprintService.validateAmi(blueprintId, region));
    }

    @DeleteMapping("/{blueprintId}")
    @Operation(summary = "Delete a draft blueprint")
    public ResponseEntity<Void> deleteBlueprint(
            @PathVariable UUID blueprintId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        blueprintService.deleteBlueprint(blueprintId, userId);
        return ResponseEntity.noContent().build();
    }
}

