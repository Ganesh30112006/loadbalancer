package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Policy operations
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    List<Policy> findByServiceIdOrderByVersionDesc(UUID serviceId);
    
    List<Policy> findByServiceIdAndStatus(UUID serviceId, Policy.PolicyStatus status);
    
    Optional<Policy> findByServiceIdAndIsActiveTrue(UUID serviceId);
    
    Optional<Policy> findByServiceIdAndVersion(UUID serviceId, Integer version);
    
    List<Policy> findByAwsAccountId(UUID awsAccountId);
    
    List<Policy> findActiveByAwsAccountId(UUID awsAccountId);
    
    Optional<Policy> findByNameAndStatus(String name, Policy.PolicyStatus status);
    
    @Query("SELECT MAX(p.version) FROM Policy p WHERE p.service.id = :serviceId")
    Optional<Integer> findMaxVersionByServiceId(UUID serviceId);
    
    @Query("SELECT p FROM Policy p WHERE p.service.id = :serviceId AND p.status = 'ACTIVE' ORDER BY p.version DESC")
    List<Policy> findApprovedByService(UUID serviceId);
}
