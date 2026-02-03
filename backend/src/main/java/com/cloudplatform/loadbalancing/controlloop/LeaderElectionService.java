package com.cloudplatform.loadbalancing.controlloop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Leader Election Service
 * 
 * Uses Redis for distributed leader election to ensure
 * only one platform instance runs the control loop.
 */
@Service
@Slf4j
@SuppressWarnings("null")
public class LeaderElectionService {

    private final StringRedisTemplate redisTemplate;
    private final String instanceId;
    private final boolean enabled;
    private final AtomicBoolean isLeader = new AtomicBoolean(false);

    private static final String LEADER_KEY = "loadbalancing:leader";
    private static final Duration LEADER_TTL = Duration.ofSeconds(30);
    // private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(10); // Reserved for future heartbeat

    public LeaderElectionService(
            StringRedisTemplate redisTemplate,
            @Value("${loadbalancing.instance-id:#{T(java.util.UUID).randomUUID().toString()}}") String instanceId,
            @Value("${loadbalancing.control.leader-election.enabled:true}") boolean enabled) {
        this.redisTemplate = redisTemplate;
        this.instanceId = instanceId;
        this.enabled = enabled;
        if (enabled) {
            log.info("Leader election initialized with instance ID: {}", instanceId);
        } else {
            log.info("Leader election is DISABLED - this instance will act as leader");
            isLeader.set(true);
        }
    }

    /**
     * Check if this instance is the leader
     */
    public boolean isLeader() {
        if (!enabled) {
            return true; // Always leader when disabled
        }
        return isLeader.get();
    }

    /**
     * Get the current leader's instance ID
     */
    public String getCurrentLeader() {
        if (!enabled) {
            return instanceId;
        }
        return redisTemplate.opsForValue().get(LEADER_KEY);
    }

    /**
     * Attempt to acquire leadership
     */
    public boolean tryAcquireLeadership() {
        if (!enabled) {
            return true;
        }
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(LEADER_KEY, instanceId, LEADER_TTL);
            
            if (Boolean.TRUE.equals(acquired)) {
                isLeader.set(true);
                log.info("Leadership acquired by instance: {}", instanceId);
                return true;
            }

            // Check if we already hold leadership
            String currentLeader = redisTemplate.opsForValue().get(LEADER_KEY);
            if (instanceId.equals(currentLeader)) {
                isLeader.set(true);
                return true;
            }

            isLeader.set(false);
            return false;

        } catch (Exception e) {
            log.error("Failed to acquire leadership: {}", e.getMessage());
            isLeader.set(false);
            return false;
        }
    }

    /**
     * Renew leadership (heartbeat)
     */
    public boolean renewLeadership() {
        if (!isLeader.get()) {
            return false;
        }

        try {
            String currentLeader = redisTemplate.opsForValue().get(LEADER_KEY);
            
            if (!instanceId.equals(currentLeader)) {
                isLeader.set(false);
                log.warn("Lost leadership to instance: {}", currentLeader);
                return false;
            }

            // Extend TTL
            redisTemplate.expire(LEADER_KEY, LEADER_TTL);
            return true;

        } catch (Exception e) {
            log.error("Failed to renew leadership: {}", e.getMessage());
            isLeader.set(false);
            return false;
        }
    }

    /**
     * Release leadership voluntarily
     */
    public void releaseLeadership() {
        if (!isLeader.get()) {
            return;
        }

        try {
            String currentLeader = redisTemplate.opsForValue().get(LEADER_KEY);
            if (instanceId.equals(currentLeader)) {
                redisTemplate.delete(LEADER_KEY);
                log.info("Leadership released by instance: {}", instanceId);
            }
        } catch (Exception e) {
            log.error("Failed to release leadership: {}", e.getMessage());
        } finally {
            isLeader.set(false);
        }
    }

    /**
     * Scheduled heartbeat to maintain leadership
     */
    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void heartbeat() {
        if (!enabled) {
            return; // Skip when disabled
        }
        if (isLeader.get()) {
            renewLeadership();
        } else {
            tryAcquireLeadership();
        }
    }

    /**
     * Get leadership status info
     */
    public LeadershipStatus getStatus() {
        String currentLeader = getCurrentLeader();
        return new LeadershipStatus(
                instanceId,
                isLeader.get(),
                currentLeader,
                Instant.now()
        );
    }

    public record LeadershipStatus(
            String instanceId,
            boolean isLeader,
            String currentLeader,
            Instant checkedAt
    ) {}
}
