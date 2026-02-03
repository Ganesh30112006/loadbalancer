package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.dto.DeploymentWorkflowDto;
import com.cloudplatform.loadbalancing.service.DeploymentWorkflowService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/deployment-workflows")
@RequiredArgsConstructor
public class DeploymentWorkflowController {

    private final DeploymentWorkflowService workflowService;

    @PostMapping("/blue-green")
    public ResponseEntity<DeploymentWorkflowDto> startBlueGreen(@RequestBody StartDeploymentRequest request) {
        return ResponseEntity.ok(
                workflowService.startBlueGreenDeployment(
                        request.getServiceId(),
                        request.getDeploymentId(),
                        request.getInitiatedBy()
                )
        );
    }

    @PostMapping("/canary")
    public ResponseEntity<DeploymentWorkflowDto> startCanary(@RequestBody StartCanaryRequest request) {
        return ResponseEntity.ok(
                workflowService.startCanaryDeployment(
                        request.getServiceId(),
                        request.getDeploymentId(),
                        request.getCanarySteps(),
                        request.getStepDurationMinutes(),
                        request.getInitiatedBy()
                )
        );
    }

    @PostMapping("/{workflowId}/shift-traffic")
    public ResponseEntity<DeploymentWorkflowDto> shiftTraffic(
            @PathVariable UUID workflowId,
            @RequestBody Map<String, Integer> request) {
        return ResponseEntity.ok(
                workflowService.shiftTraffic(workflowId, request.get("greenPercent"))
        );
    }

    @PostMapping("/{workflowId}/advance-canary")
    public ResponseEntity<DeploymentWorkflowDto> advanceCanary(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(workflowService.advanceCanaryStep(workflowId));
    }

    @PostMapping("/{workflowId}/rollback")
    public ResponseEntity<DeploymentWorkflowDto> rollback(
            @PathVariable UUID workflowId,
            @RequestBody RollbackRequest request) {
        return ResponseEntity.ok(
                workflowService.rollback(workflowId, request.getReason(), request.getRolledBackBy())
        );
    }

    @PostMapping("/{workflowId}/complete")
    public ResponseEntity<DeploymentWorkflowDto> complete(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(workflowService.completeDeployment(workflowId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<DeploymentWorkflowDto>> getActiveWorkflows() {
        return ResponseEntity.ok(workflowService.getActiveWorkflows());
    }

    @GetMapping
    public ResponseEntity<List<DeploymentWorkflowDto>> getAllWorkflows(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(workflowService.getAllWorkflows(limit));
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<DeploymentWorkflowDto> getWorkflow(@PathVariable UUID workflowId) {
        return workflowService.getWorkflow(workflowId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/services/{serviceId}")
    public ResponseEntity<List<DeploymentWorkflowDto>> getWorkflowsForService(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(workflowService.getWorkflowsForService(serviceId));
    }

    @Data
    public static class StartDeploymentRequest {
        private UUID serviceId;
        private UUID deploymentId;
        private String initiatedBy;
    }

    @Data
    public static class StartCanaryRequest {
        private UUID serviceId;
        private UUID deploymentId;
        private Integer canarySteps;
        private Integer stepDurationMinutes;
        private String initiatedBy;
    }

    @Data
    public static class RollbackRequest {
        private String reason;
        private String rolledBackBy;
    }
}

