package com.cloudplatform.loadbalancing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Load Balancing Control Plane Application
 * 
 * Production-grade AWS infrastructure control plane for:
 * - Load balancing management
 * - Auto-scaling orchestration
 * - Traffic management and failover
 * - Multi-account and multi-region support
 * 
 * @author Cloud Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class LoadBalancingApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoadBalancingApplication.class, args);
    }
}
