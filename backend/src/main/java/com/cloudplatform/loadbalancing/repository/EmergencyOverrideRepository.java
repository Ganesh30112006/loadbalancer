package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.EmergencyOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmergencyOverrideRepository extends JpaRepository<EmergencyOverride, UUID> {
    
    List<EmergencyOverride> findByServiceIdOrderByCreatedAtDesc(UUID serviceId);
    
    @Query("SELECT e FROM EmergencyOverride e WHERE e.status = 'ACTIVE' ORDER BY e.createdAt DESC")
    List<EmergencyOverride> findAllActive();
    
    @Query("SELECT e FROM EmergencyOverride e WHERE e.service.id = :serviceId AND e.status = 'ACTIVE'")
    List<EmergencyOverride> findActiveByServiceId(UUID serviceId);
    
    @Query("SELECT e FROM EmergencyOverride e WHERE e.scope = 'GLOBAL' AND e.status = 'ACTIVE'")
    List<EmergencyOverride> findActiveGlobalOverrides();
    
    @Query("SELECT e FROM EmergencyOverride e WHERE e.overrideType = :type AND e.status = 'ACTIVE'")
    List<EmergencyOverride> findActiveByType(EmergencyOverride.OverrideType type);
}
