package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.dto.DiscoveredResourceDto;
import com.cloudplatform.loadbalancing.entity.DiscoveredResource;
import com.cloudplatform.loadbalancing.service.ResourceDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/discovery")
@RequiredArgsConstructor
public class ResourceDiscoveryController {

    private final ResourceDiscoveryService discoveryService;

    @PostMapping("/accounts/{accountId}/scan")
    public ResponseEntity<Map<String, String>> triggerDiscovery(@PathVariable UUID accountId) {
        discoveryService.discoverResources(accountId);
        return ResponseEntity.accepted().body(Map.of(
                "message", "Resource discovery started",
                "accountId", accountId.toString()
        ));
    }

    @GetMapping("/accounts/{accountId}/resources")
    public ResponseEntity<List<DiscoveredResourceDto>> getDiscoveredResources(@PathVariable UUID accountId) {
        return ResponseEntity.ok(discoveryService.getDiscoveredResources(accountId));
    }

    @GetMapping("/accounts/{accountId}/resources/type/{type}")
    public ResponseEntity<List<DiscoveredResourceDto>> getResourcesByType(
            @PathVariable UUID accountId,
            @PathVariable DiscoveredResource.ResourceType type) {
        return ResponseEntity.ok(discoveryService.getDiscoveredResourcesByType(accountId, type));
    }

    @GetMapping("/accounts/{accountId}/regions")
    public ResponseEntity<List<String>> getDiscoveredRegions(@PathVariable UUID accountId) {
        return ResponseEntity.ok(discoveryService.getDiscoveredRegions(accountId));
    }

    @PutMapping("/resources/{resourceId}/status")
    public ResponseEntity<DiscoveredResourceDto> updateAdoptionStatus(
            @PathVariable UUID resourceId,
            @RequestBody Map<String, String> request) {
        DiscoveredResource.AdoptionStatus status = 
                DiscoveredResource.AdoptionStatus.valueOf(request.get("status"));
        return ResponseEntity.ok(discoveryService.updateAdoptionStatus(resourceId, status));
    }
}

