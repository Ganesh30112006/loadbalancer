package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.AwsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AWS Account operations
 */
@Repository
public interface AwsAccountRepository extends JpaRepository<AwsAccount, UUID> {

    Optional<AwsAccount> findByAccountId(String accountId);
    
    Optional<AwsAccount> findByExternalId(String externalId);
    
    boolean existsByAccountId(String accountId);
    
    boolean existsByExternalId(String externalId);
    
    List<AwsAccount> findByStatus(AwsAccount.AccountStatus status);
    
    @Query("SELECT a FROM AwsAccount a WHERE a.status = 'ACTIVE' ORDER BY a.accountName")
    List<AwsAccount> findAllActiveAccounts();
    
    @Query("SELECT a FROM AwsAccount a WHERE a.createdBy = :userId ORDER BY a.createdAt DESC")
    List<AwsAccount> findByCreatedBy(UUID userId);
}
