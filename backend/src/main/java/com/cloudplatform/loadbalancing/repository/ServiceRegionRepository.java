package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.ServiceRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ServiceRegion operations
 */
@Repository
public interface ServiceRegionRepository extends JpaRepository<ServiceRegion, UUID> {

    List<ServiceRegion> findByServiceIdOrderByIsPrimaryDescRegionAsc(UUID serviceId);
    
    List<ServiceRegion> findByServiceId(UUID serviceId);
    
    Optional<ServiceRegion> findByServiceIdAndRegion(UUID serviceId, String region);
    
    Optional<ServiceRegion> findByServiceIdAndIsPrimaryTrue(UUID serviceId);
    
    List<ServiceRegion> findByServiceIdAndStatus(UUID serviceId, ServiceRegion.RegionStatus status);
    
    @Query("SELECT sr FROM ServiceRegion sr WHERE sr.service.id = :serviceId AND sr.status IN ('ACTIVE', 'DEGRADED')")
    List<ServiceRegion> findHealthyRegionsByService(UUID serviceId);
    
    @Query("SELECT sr FROM ServiceRegion sr JOIN sr.service s WHERE s.awsAccount.id = :accountId AND sr.status = 'ACTIVE'")
    List<ServiceRegion> findActiveRegionsByAccount(UUID accountId);
    
    @Query("SELECT SUM(sr.runningInstances) FROM ServiceRegion sr WHERE sr.service.id = :serviceId")
    Integer getTotalRunningInstancesByService(UUID serviceId);
}
