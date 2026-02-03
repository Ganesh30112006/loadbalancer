package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.DiscoveredResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscoveredResourceRepository extends JpaRepository<DiscoveredResource, UUID> {
    
    List<DiscoveredResource> findByAwsAccountId(UUID accountId);
    
    List<DiscoveredResource> findByAwsAccountIdAndResourceType(UUID accountId, DiscoveredResource.ResourceType type);
    
    List<DiscoveredResource> findByAwsAccountIdAndRegion(UUID accountId, String region);
    
    List<DiscoveredResource> findByAwsAccountIdAndAdoptionStatus(UUID accountId, DiscoveredResource.AdoptionStatus status);
    
    Optional<DiscoveredResource> findByAwsAccountIdAndResourceArn(UUID accountId, String resourceArn);
    
    @Query("SELECT DISTINCT d.region FROM DiscoveredResource d WHERE d.awsAccount.id = :accountId")
    List<String> findDistinctRegionsByAccountId(UUID accountId);
    
    @Query("SELECT COUNT(d) FROM DiscoveredResource d WHERE d.awsAccount.id = :accountId AND d.adoptionStatus = 'DISCOVERED'")
    long countUnadoptedByAccountId(UUID accountId);
}
