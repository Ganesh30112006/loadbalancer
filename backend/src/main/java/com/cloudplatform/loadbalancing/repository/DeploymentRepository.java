package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.Deployment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Deployment operations
 */
@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {

    Page<Deployment> findByServiceIdOrderByCreatedAtDesc(UUID serviceId, Pageable pageable);
    
    Optional<Deployment> findByServiceIdAndStatusIn(UUID serviceId, List<Deployment.DeploymentStatus> statuses);
    
    @Query("SELECT d FROM Deployment d WHERE d.service.id = :serviceId AND d.status IN ('PENDING', 'APPROVED', 'IN_PROGRESS', 'VERIFYING')")
    Optional<Deployment> findActiveDeploymentByService(UUID serviceId);
    
    @Query("SELECT d FROM Deployment d WHERE d.status IN ('IN_PROGRESS', 'VERIFYING') ORDER BY d.startedAt")
    List<Deployment> findAllInProgressDeployments();
    
    List<Deployment> findByServiceIdAndStatus(UUID serviceId, Deployment.DeploymentStatus status);
    
    @Query("SELECT COUNT(d) FROM Deployment d WHERE d.service.id = :serviceId AND d.status = 'COMPLETED'")
    long countSuccessfulDeployments(UUID serviceId);
    
    @Query("SELECT COUNT(d) FROM Deployment d WHERE d.service.id = :serviceId AND d.status IN ('ROLLED_BACK', 'FAILED')")
    long countFailedDeployments(UUID serviceId);
}
