package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.DeploymentWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeploymentWorkflowRepository extends JpaRepository<DeploymentWorkflow, UUID> {
    
    List<DeploymentWorkflow> findByServiceIdOrderByCreatedAtDesc(UUID serviceId);
    
    Optional<DeploymentWorkflow> findByDeploymentId(UUID deploymentId);
    
    @Query("SELECT d FROM DeploymentWorkflow d WHERE d.status = 'IN_PROGRESS' OR d.status = 'PAUSED' ORDER BY d.createdAt DESC")
    List<DeploymentWorkflow> findActiveWorkflows();
    
    @Query("SELECT d FROM DeploymentWorkflow d WHERE d.service.id = :serviceId AND (d.status = 'IN_PROGRESS' OR d.status = 'PAUSED')")
    Optional<DeploymentWorkflow> findActiveByServiceId(UUID serviceId);
}
