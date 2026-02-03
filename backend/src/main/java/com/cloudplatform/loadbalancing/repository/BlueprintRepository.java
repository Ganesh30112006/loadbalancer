package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.Blueprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Blueprint operations
 */
@Repository
public interface BlueprintRepository extends JpaRepository<Blueprint, UUID> {

    List<Blueprint> findByAwsAccountIdOrderByCreatedAtDesc(UUID awsAccountId);
    
    List<Blueprint> findByAwsAccountId(UUID awsAccountId);
    
    List<Blueprint> findByAwsAccountIdAndStatus(UUID awsAccountId, Blueprint.BlueprintStatus status);
    
    @Query("SELECT b FROM Blueprint b WHERE b.awsAccount.id = :accountId AND b.status = 'ACTIVE' ORDER BY b.name, b.version DESC")
    List<Blueprint> findActiveByAwsAccountId(UUID accountId);
    
    Optional<Blueprint> findByNameAndStatus(String name, Blueprint.BlueprintStatus status);
    
    @Query("SELECT b FROM Blueprint b WHERE b.awsAccount.id = :accountId AND b.name = :name ORDER BY b.version DESC")
    List<Blueprint> findByAccountAndNameOrderByVersionDesc(UUID accountId, String name);
    
    @Query("SELECT b FROM Blueprint b WHERE b.awsAccount.id = :accountId AND b.status = 'ACTIVE' ORDER BY b.name, b.version DESC")
    List<Blueprint> findApprovedByAccount(UUID accountId);
    
    Optional<Blueprint> findByAwsAccountIdAndNameAndVersion(UUID accountId, String name, Integer version);
    
    @Query("SELECT MAX(b.version) FROM Blueprint b WHERE b.awsAccount.id = :accountId AND b.name = :name")
    Optional<Integer> findMaxVersionByAccountAndName(UUID accountId, String name);
}
