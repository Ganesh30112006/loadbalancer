package com.cloudplatform.loadbalancing.repository;

import com.cloudplatform.loadbalancing.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Audit Log operations
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByServiceIdOrderByCreatedAtDesc(UUID serviceId, Pageable pageable);
    
    Page<AuditLog> findByAwsAccountIdOrderByCreatedAtDesc(UUID awsAccountId, Pageable pageable);
    
    List<AuditLog> findByCycleId(String cycleId);
    
    Page<AuditLog> findByActionTypeOrderByCreatedAtDesc(AuditLog.ActionType actionType, Pageable pageable);
    
    Page<AuditLog> findByActionCategoryOrderByCreatedAtDesc(AuditLog.ActionCategory category, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.serviceId = :serviceId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findByServiceIdSince(UUID serviceId, Instant since);
    
    @Query("SELECT a FROM AuditLog a WHERE a.serviceId = :serviceId ORDER BY a.createdAt DESC LIMIT :limit")
    List<AuditLog> findRecentByServiceId(UUID serviceId, int limit);
    
    @Query("SELECT a FROM AuditLog a WHERE a.serviceId = :serviceId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findByServiceIdAndCreatedAtAfter(UUID serviceId, Instant since);
    
    @Query("SELECT a FROM AuditLog a WHERE a.actionType IN :actionTypes AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findByActionTypesSince(List<AuditLog.ActionType> actionTypes, Instant since);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.serviceId = :serviceId AND a.actionType = :actionType AND a.createdAt >= :since")
    long countByServiceAndActionTypeSince(UUID serviceId, AuditLog.ActionType actionType, Instant since);
}
