package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.ScalingEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScalingEventRepository extends JpaRepository<ScalingEvent, UUID> {
    
    List<ScalingEvent> findByServiceIdOrderByCreatedAtDesc(UUID serviceId);
    
    Page<ScalingEvent> findByServiceIdOrderByCreatedAtDesc(UUID serviceId, Pageable pageable);
    
    List<ScalingEvent> findByServiceIdAndRegionOrderByCreatedAtDesc(UUID serviceId, String region);
    
    @Query("SELECT s FROM ScalingEvent s WHERE s.service.id = :serviceId AND s.createdAt > :since ORDER BY s.createdAt DESC")
    List<ScalingEvent> findRecentByServiceId(UUID serviceId, Instant since);
    
    @Query("SELECT s FROM ScalingEvent s WHERE s.status = 'PENDING' OR s.status = 'IN_PROGRESS' ORDER BY s.createdAt ASC")
    List<ScalingEvent> findPendingEvents();
    
    @Query("SELECT COUNT(s) FROM ScalingEvent s WHERE s.service.id = :serviceId AND s.eventType = :eventType AND s.createdAt > :since")
    long countByServiceIdAndTypeAfter(UUID serviceId, ScalingEvent.EventType eventType, Instant since);
}
