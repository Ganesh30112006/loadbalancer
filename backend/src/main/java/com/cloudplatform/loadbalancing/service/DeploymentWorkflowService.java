package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.dto.DeploymentWorkflowDto;
import com.cloudplatform.loadbalancing.entity.Deployment;
import com.cloudplatform.loadbalancing.entity.DeploymentWorkflow;
import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.repository.DeploymentRepository;
import com.cloudplatform.loadbalancing.repository.DeploymentWorkflowRepository;
import com.cloudplatform.loadbalancing.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DeploymentWorkflowService {

    private final ServiceRepository serviceRepository;
    private final DeploymentRepository deploymentRepository;
    private final DeploymentWorkflowRepository workflowRepository;

    /**
     * Start a Blue/Green deployment
     */
    @Transactional
    public DeploymentWorkflowDto startBlueGreenDeployment(UUID serviceId, UUID deploymentId, String initiatedBy) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found"));

        // Check for existing active workflow
        Optional<DeploymentWorkflow> existingWorkflow = workflowRepository.findActiveByServiceId(serviceId);
        if (existingWorkflow.isPresent()) {
            throw new IllegalStateException("An active deployment workflow already exists for this service");
        }

        DeploymentWorkflow workflow = DeploymentWorkflow.builder()
                .service(service)
                .deployment(deployment)
                .strategy(DeploymentWorkflow.DeploymentStrategy.BLUE_GREEN)
                .status(DeploymentWorkflow.WorkflowStatus.IN_PROGRESS)
                .currentPhase(DeploymentWorkflow.WorkflowPhase.INITIALIZING)
                .blueTrafficPercent(100)
                .greenTrafficPercent(0)
                .initiatedBy(initiatedBy)
                .startedAt(Instant.now())
                .build();

        workflow.appendProgressLog("Blue/Green deployment initiated by " + initiatedBy);
        workflow = workflowRepository.save(workflow);

        log.info("Blue/Green deployment started for service {} by {}", service.getServiceName(), initiatedBy);

        // Start async workflow execution
        executeBlueGreenWorkflow(workflow.getId());

        return DeploymentWorkflowDto.fromEntity(workflow);
    }

    /**
     * Start a Canary deployment
     */
    @Transactional
    public DeploymentWorkflowDto startCanaryDeployment(UUID serviceId, UUID deploymentId, 
                                                        Integer canarySteps, Integer stepDurationMinutes,
                                                        String initiatedBy) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found"));

        // Check for existing active workflow
        Optional<DeploymentWorkflow> existingWorkflow = workflowRepository.findActiveByServiceId(serviceId);
        if (existingWorkflow.isPresent()) {
            throw new IllegalStateException("An active deployment workflow already exists for this service");
        }

        DeploymentWorkflow workflow = DeploymentWorkflow.builder()
                .service(service)
                .deployment(deployment)
                .strategy(DeploymentWorkflow.DeploymentStrategy.CANARY)
                .status(DeploymentWorkflow.WorkflowStatus.IN_PROGRESS)
                .currentPhase(DeploymentWorkflow.WorkflowPhase.INITIALIZING)
                .blueTrafficPercent(100)
                .canaryTrafficPercent(0)
                .canarySteps(canarySteps != null ? canarySteps : 5)
                .stepDurationMinutes(stepDurationMinutes != null ? stepDurationMinutes : 5)
                .currentCanaryStep(0)
                .initiatedBy(initiatedBy)
                .startedAt(Instant.now())
                .build();

        workflow.appendProgressLog("Canary deployment initiated by " + initiatedBy);
        workflow.appendProgressLog("Configuration: " + workflow.getCanarySteps() + " steps, " + 
                workflow.getStepDurationMinutes() + " minutes per step");
        workflow = workflowRepository.save(workflow);

        log.info("Canary deployment started for service {} by {}", service.getServiceName(), initiatedBy);

        return DeploymentWorkflowDto.fromEntity(workflow);
    }

    /**
     * Shift traffic in Blue/Green deployment
     */
    @Transactional
    public DeploymentWorkflowDto shiftTraffic(UUID workflowId, Integer greenPercent) {
        DeploymentWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        if (workflow.getStatus() != DeploymentWorkflow.WorkflowStatus.IN_PROGRESS) {
            throw new IllegalStateException("Workflow is not in progress");
        }

        int bluePercent = 100 - greenPercent;
        workflow.setBlueTrafficPercent(bluePercent);
        workflow.setGreenTrafficPercent(greenPercent);
        workflow.setCurrentPhase(DeploymentWorkflow.WorkflowPhase.TRAFFIC_SHIFTING);
        workflow.appendProgressLog("Traffic shifted: Blue=" + bluePercent + "%, Green=" + greenPercent + "%");

        workflow = workflowRepository.save(workflow);
        log.info("Traffic shifted for workflow {}: {}% to green", workflowId, greenPercent);

        return DeploymentWorkflowDto.fromEntity(workflow);
    }

    /**
     * Advance canary to next step
     */
    @Transactional
    public DeploymentWorkflowDto advanceCanaryStep(UUID workflowId) {
        DeploymentWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        if (workflow.getStrategy() != DeploymentWorkflow.DeploymentStrategy.CANARY) {
            throw new IllegalStateException("Not a canary deployment");
        }

        if (workflow.getStatus() != DeploymentWorkflow.WorkflowStatus.IN_PROGRESS) {
            throw new IllegalStateException("Workflow is not in progress");
        }

        int nextStep = workflow.getCurrentCanaryStep() + 1;
        if (nextStep > workflow.getCanarySteps()) {
            throw new IllegalStateException("Canary already at final step");
        }

        int canaryPercent = (nextStep * 100) / workflow.getCanarySteps();
        int bluePercent = 100 - canaryPercent;

        workflow.setCurrentCanaryStep(nextStep);
        workflow.setBlueTrafficPercent(bluePercent);
        workflow.setCanaryTrafficPercent(canaryPercent);
        workflow.setCurrentPhase(DeploymentWorkflow.WorkflowPhase.TRAFFIC_SHIFTING);
        workflow.appendProgressLog("Canary step " + nextStep + "/" + workflow.getCanarySteps() + 
                ": " + canaryPercent + "% traffic");

        workflow = workflowRepository.save(workflow);
        log.info("Canary advanced to step {}/{} for workflow {}", nextStep, workflow.getCanarySteps(), workflowId);

        return DeploymentWorkflowDto.fromEntity(workflow);
    }

    /**
     * Rollback deployment
     */
    @Transactional
    public DeploymentWorkflowDto rollback(UUID workflowId, String reason, String rolledBackBy) {
        DeploymentWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        if (workflow.getStatus() == DeploymentWorkflow.WorkflowStatus.COMPLETED ||
            workflow.getStatus() == DeploymentWorkflow.WorkflowStatus.ROLLED_BACK) {
            throw new IllegalStateException("Cannot rollback a completed or already rolled back workflow");
        }

        workflow.setStatus(DeploymentWorkflow.WorkflowStatus.ROLLED_BACK);
        workflow.setCurrentPhase(DeploymentWorkflow.WorkflowPhase.ROLLING_BACK);
        workflow.setBlueTrafficPercent(100);
        workflow.setGreenTrafficPercent(0);
        workflow.setCanaryTrafficPercent(0);
        workflow.setRollbackTriggeredAt(Instant.now());
        workflow.setRollbackReason(reason);
        workflow.setCompletedAt(Instant.now());
        workflow.setDurationMs(workflow.getCompletedAt().toEpochMilli() - workflow.getStartedAt().toEpochMilli());
        workflow.appendProgressLog("ROLLBACK triggered by " + rolledBackBy + ": " + reason);

        workflow = workflowRepository.save(workflow);
        log.warn("Deployment rollback for workflow {} by {}: {}", workflowId, rolledBackBy, reason);

        return DeploymentWorkflowDto.fromEntity(workflow);
    }

    /**
     * Complete deployment successfully
     */
    @Transactional
    public DeploymentWorkflowDto completeDeployment(UUID workflowId) {
        DeploymentWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        workflow.setStatus(DeploymentWorkflow.WorkflowStatus.COMPLETED);
        workflow.setCurrentPhase(DeploymentWorkflow.WorkflowPhase.COMPLETED);
        workflow.setBlueTrafficPercent(0);
        workflow.setGreenTrafficPercent(100);
        workflow.setCompletedAt(Instant.now());
        workflow.setDurationMs(workflow.getCompletedAt().toEpochMilli() - workflow.getStartedAt().toEpochMilli());
        workflow.appendProgressLog("Deployment completed successfully");

        workflow = workflowRepository.save(workflow);
        log.info("Deployment completed for workflow {}", workflowId);

        return DeploymentWorkflowDto.fromEntity(workflow);
    }

    /**
     * Get active workflows
     */
    public List<DeploymentWorkflowDto> getActiveWorkflows() {
        return workflowRepository.findActiveWorkflows().stream()
                .map(DeploymentWorkflowDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all workflows with limit
     */
    public List<DeploymentWorkflowDto> getAllWorkflows(int limit) {
        return workflowRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, limit, 
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        ).getContent().stream()
                .map(DeploymentWorkflowDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get workflow by ID
     */
    public Optional<DeploymentWorkflowDto> getWorkflow(UUID workflowId) {
        return workflowRepository.findById(workflowId)
                .map(DeploymentWorkflowDto::fromEntity);
    }

    /**
     * Get workflows for a service
     */
    public List<DeploymentWorkflowDto> getWorkflowsForService(UUID serviceId) {
        return workflowRepository.findByServiceIdOrderByCreatedAtDesc(serviceId).stream()
                .map(DeploymentWorkflowDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Async
    protected void executeBlueGreenWorkflow(UUID workflowId) {
        // This would contain the actual deployment logic
        // For now, it's a placeholder that would:
        // 1. Provision green environment
        // 2. Run health checks
        // 3. Gradually shift traffic
        // 4. Monitor metrics
        // 5. Complete or rollback
        log.info("Blue/Green workflow execution started for {}", workflowId);
    }
}
