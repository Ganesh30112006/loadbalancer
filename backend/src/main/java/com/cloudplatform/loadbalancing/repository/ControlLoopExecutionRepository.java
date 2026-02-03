package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.ControlLoopExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ControlLoopExecutionRepository extends JpaRepository<ControlLoopExecution, UUID> {
    
    List<ControlLoopExecution> findByServiceIdOrderByCreatedAtDesc(UUID serviceId);
    
    Page<ControlLoopExecution> findByServiceIdOrderByCreatedAtDesc(UUID serviceId, Pageable pageable);
    
    List<ControlLoopExecution> findByServiceIdAndPhaseOrderByCreatedAtDesc(UUID serviceId, ControlLoopExecution.ExecutionPhase phase);
    
    @Query("SELECT c FROM ControlLoopExecution c WHERE c.service.id = :serviceId AND c.createdAt > :since ORDER BY c.createdAt DESC")
    List<ControlLoopExecution> findRecentByServiceId(UUID serviceId, Instant since);
    
    @Query("SELECT c FROM ControlLoopExecution c WHERE c.phase = 'EXECUTING' OR c.phase = 'ANALYZING' OR c.phase = 'OBSERVING' ORDER BY c.createdAt DESC")
    List<ControlLoopExecution> findActiveExecutions();
    
    @Query("SELECT c FROM ControlLoopExecution c WHERE c.service.id = :serviceId ORDER BY c.createdAt DESC LIMIT 1")
    ControlLoopExecution findLatestByServiceId(UUID serviceId);
}
