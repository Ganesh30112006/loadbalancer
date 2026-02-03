package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Service operations
 */
@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    List<Service> findByAwsAccountIdOrderByServiceName(UUID awsAccountId);
    
    List<Service> findByAwsAccountIdAndStatus(UUID awsAccountId, Service.ServiceStatus status);
    
    Optional<Service> findByAwsAccountIdAndServiceName(UUID awsAccountId, String serviceName);
    
    @Query("SELECT s FROM Service s LEFT JOIN FETCH s.regions WHERE s.awsAccount.id = :accountId")
    List<Service> findByAwsAccountIdWithRegions(UUID accountId);
    
    @Query("SELECT s FROM Service s LEFT JOIN FETCH s.regions WHERE s.status IN ('ACTIVE', 'DEGRADED', 'SCALING', 'DEPLOYING') AND s.automationEnabled = true")
    List<Service> findServicesForControlLoop();
    
    @Query("SELECT s FROM Service s WHERE s.status IN ('ACTIVE', 'DEGRADED', 'SCALING', 'DEPLOYING') AND s.automationEnabled = true")
    List<Service> findActiveServicesForControlLoop();
    
    @Query("SELECT s FROM Service s WHERE s.awsAccount.id = :accountId AND s.status IN ('ACTIVE', 'DEGRADED', 'SCALING', 'DEPLOYING')")
    List<Service> findActiveServicesByAccount(UUID accountId);
    
    @Query("SELECT COUNT(s) FROM Service s WHERE s.awsAccount.id = :accountId AND s.status NOT IN ('DELETED', 'SUSPENDED')")
    long countActiveServicesByAccount(UUID accountId);
}
