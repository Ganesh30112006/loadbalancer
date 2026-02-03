package com.cloudplatform.loadbalancing.controller;

import com.cloudplatform.loadbalancing.entity.AuditLog;
import com.cloudplatform.loadbalancing.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAllAuditLogs(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        List<AuditLog> logs;
        if (category != null && !category.isEmpty()) {
            AuditLog.ActionCategory actionCategory = AuditLog.ActionCategory.valueOf(category.toUpperCase());
            logs = auditLogRepository.findByActionCategoryOrderByCreatedAtDesc(actionCategory, pageRequest).getContent();
        } else {
            logs = auditLogRepository.findAll(pageRequest).getContent();
        }
        
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/services/{serviceId}/logs")
    public ResponseEntity<List<AuditLog>> getServiceAuditLogs(
            @PathVariable UUID serviceId,
            @RequestParam(defaultValue = "50") int limit) {
        
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AuditLog> logs = auditLogRepository.findByServiceIdOrderByCreatedAtDesc(serviceId, pageRequest).getContent();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/accounts/{accountId}/logs")
    public ResponseEntity<List<AuditLog>> getAccountAuditLogs(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "50") int limit) {
        
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AuditLog> logs = auditLogRepository.findByAwsAccountIdOrderByCreatedAtDesc(accountId, pageRequest).getContent();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/{logId}")
    public ResponseEntity<AuditLog> getAuditLog(@PathVariable UUID logId) {
        return auditLogRepository.findById(logId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/logs/recent")
    public ResponseEntity<List<AuditLog>> getRecentLogs(@RequestParam(defaultValue = "20") int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(auditLogRepository.findAll(pageRequest).getContent());
    }
}

