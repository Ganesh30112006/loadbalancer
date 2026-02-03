package com.cloudplatform.loadbalancing.websocket;

import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.entity.AuditLog;
import com.cloudplatform.loadbalancing.service.ObservabilityService;
import com.cloudplatform.loadbalancing.service.ServiceManagementService;
import com.cloudplatform.loadbalancing.controlloop.LeaderElectionService;
import com.cloudplatform.loadbalancing.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Handler for Real-Time Metrics
 * 
 * Streams live metrics to connected clients for dashboard updates.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class MetricsWebSocketHandler extends TextWebSocketHandler {

    private final ServiceManagementService serviceManagementService;
    private final ObservabilityService observabilityService;
    private final LeaderElectionService leaderElectionService;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // Track active sessions and their subscriptions
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> sessionSubscriptions = new ConcurrentHashMap<>();
    // Track sessions subscribed to dashboard updates
    private final Set<String> dashboardSubscribers = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        sessionSubscriptions.put(sessionId, new HashSet<>());
        
        log.info("WebSocket connection established: {}", sessionId);
        
        // Send welcome message
        sendMessage(session, new WsMessage("connected", Map.of(
                "sessionId", sessionId,
                "message", "Connected to Load Balancing metrics stream"
        )));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        sessionSubscriptions.remove(sessionId);
        
        log.info("WebSocket connection closed: {} ({})", sessionId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            WsCommand command = objectMapper.readValue(message.getPayload(), WsCommand.class);
            handleCommand(session, command);
        } catch (Exception e) {
            log.error("Failed to handle WebSocket message: {}", e.getMessage());
            sendMessage(session, new WsMessage("error", Map.of(
                    "message", "Invalid command: " + e.getMessage()
            )));
        }
    }

    private void handleCommand(WebSocketSession session, WsCommand command) throws IOException {
        String sessionId = session.getId();
        
        switch (command.action()) {
            case "subscribe" -> {
                UUID serviceId = UUID.fromString(command.serviceId());
                sessionSubscriptions.get(sessionId).add(serviceId);
                log.info("Session {} subscribed to service {}", sessionId, serviceId);
                sendMessage(session, new WsMessage("subscribed", Map.of(
                        "serviceId", serviceId.toString()
                )));
                // Send initial metrics
                sendMetricsForService(session, serviceId);
            }
            case "unsubscribe" -> {
                UUID serviceId = UUID.fromString(command.serviceId());
                sessionSubscriptions.get(sessionId).remove(serviceId);
                log.info("Session {} unsubscribed from service {}", sessionId, serviceId);
                sendMessage(session, new WsMessage("unsubscribed", Map.of(
                        "serviceId", serviceId.toString()
                )));
            }
            case "subscribe_dashboard" -> {
                dashboardSubscribers.add(sessionId);
                log.info("Session {} subscribed to dashboard updates", sessionId);
                sendMessage(session, new WsMessage("dashboard_subscribed", Map.of(
                        "message", "Subscribed to real-time dashboard updates"
                )));
                // Send initial dashboard data
                sendDashboardData(session);
            }
            case "unsubscribe_dashboard" -> {
                dashboardSubscribers.remove(sessionId);
                log.info("Session {} unsubscribed from dashboard updates", sessionId);
                sendMessage(session, new WsMessage("dashboard_unsubscribed", Map.of(
                        "message", "Unsubscribed from dashboard updates"
                )));
            }
            case "refresh" -> {
                if (command.serviceId() != null) {
                    sendMetricsForService(session, UUID.fromString(command.serviceId()));
                }
            }
            case "refresh_dashboard" -> {
                sendDashboardData(session);
            }
            default -> {
                sendMessage(session, new WsMessage("error", Map.of(
                        "message", "Unknown action: " + command.action()
                )));
            }
        }
    }

    /**
     * Push metrics to all subscribed clients (every 1 second)
     */
    @Scheduled(fixedRate = 1000)
    public void pushMetrics() {
        if (sessions.isEmpty()) return;

        // Push dashboard updates to dashboard subscribers
        if (!dashboardSubscribers.isEmpty()) {
            try {
                Map<String, Object> dashboardData = collectDashboardData();
                WsMessage message = new WsMessage("dashboard_update", dashboardData);
                broadcastToDashboardSubscribers(message);
            } catch (Exception e) {
                log.debug("Failed to broadcast dashboard data: {}", e.getMessage());
            }
        }

        // Collect unique services that need metrics
        Set<UUID> activeSubscriptions = new HashSet<>();
        for (Set<UUID> subs : sessionSubscriptions.values()) {
            activeSubscriptions.addAll(subs);
        }

        // Collect and broadcast metrics for each service
        for (UUID serviceId : activeSubscriptions) {
            try {
                Map<String, Object> metrics = collectMetrics(serviceId);
                broadcastToSubscribers(serviceId, new WsMessage("metrics", metrics));
            } catch (Exception e) {
                log.debug("Failed to collect metrics for service {}: {}", serviceId, e.getMessage());
            }
        }
    }

    private void sendMetricsForService(WebSocketSession session, UUID serviceId) throws IOException {
        try {
            Map<String, Object> metrics = collectMetrics(serviceId);
            sendMessage(session, new WsMessage("metrics", metrics));
        } catch (Exception e) {
            sendMessage(session, new WsMessage("error", Map.of(
                    "serviceId", serviceId.toString(),
                    "message", "Failed to collect metrics: " + e.getMessage()
            )));
        }
    }

    private Map<String, Object> collectMetrics(UUID serviceId) {
        Service service = serviceManagementService.getServiceWithRegions(serviceId);
        Map<String, ObservabilityService.RegionMetrics> regionMetrics = 
                observabilityService.collectAllRegionMetrics(service);
        ObservabilityService.ServiceMetrics aggregated = 
                observabilityService.aggregateServiceMetrics(regionMetrics);
        ObservabilityService.HealthStatus health = 
                observabilityService.computeHealthStatus(service, aggregated);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("serviceId", serviceId.toString());
        metrics.put("serviceName", service.getServiceName());
        metrics.put("status", service.getStatus().name());
        metrics.put("healthStatus", health.getStatus());
        metrics.put("avgCpu", aggregated.getAvgCpu());
        metrics.put("latencyP99", aggregated.getLatencyP99());
        metrics.put("errorRate", aggregated.getErrorRate());
        metrics.put("requestsPerMinute", aggregated.getTotalRequestsPerMinute());
        metrics.put("healthyInstances", aggregated.getTotalHealthyInstances());
        metrics.put("unhealthyInstances", aggregated.getTotalUnhealthyInstances());
        metrics.put("sloViolations", health.getSloViolations());
        metrics.put("automationEnabled", service.getAutomationEnabled());
        metrics.put("scalingEnabled", service.getScalingEnabled());
        metrics.put("timestamp", Instant.now().toString());

        // Region-level metrics
        List<Map<String, Object>> regions = new ArrayList<>();
        for (var entry : regionMetrics.entrySet()) {
            Map<String, Object> rm = new HashMap<>();
            rm.put("region", entry.getKey());
            rm.put("avgCpu", entry.getValue().getAvgCpu());
            rm.put("latencyP99", entry.getValue().getLatencyP99());
            rm.put("errorRate", entry.getValue().getErrorRate());
            rm.put("healthyHosts", entry.getValue().getHealthyHostCount());
            rm.put("unhealthyHosts", entry.getValue().getUnhealthyHostCount());
            rm.put("requestCount", entry.getValue().getRequestCount());
            regions.add(rm);
        }
        metrics.put("regions", regions);

        return metrics;
    }

    private void broadcastToSubscribers(UUID serviceId, WsMessage message) {
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize message: {}", e.getMessage());
            return;
        }

        for (var entry : sessionSubscriptions.entrySet()) {
            if (entry.getValue().contains(serviceId)) {
                WebSocketSession session = sessions.get(entry.getKey());
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(jsonMessage));
                    } catch (IOException e) {
                        log.debug("Failed to send message to session {}: {}", 
                                entry.getKey(), e.getMessage());
                    }
                }
            }
        }
    }

    private void sendMessage(WebSocketSession session, WsMessage message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }

    // Dashboard data collection and broadcasting
    private Map<String, Object> collectDashboardData() {
        Map<String, Object> data = new ConcurrentHashMap<>();
        
        try {
            // Get all services
            var services = serviceManagementService.getAllServices();
            data.put("services", services);
            
            // Calculate stats
            long totalServices = services.size();
            long healthyServices = services.stream()
                    .filter(s -> s.getStatus() == com.cloudplatform.loadbalancing.entity.Service.ServiceStatus.ACTIVE)
                    .count();
            long activeRegions = services.stream()
                    .flatMap(s -> s.getRegions() != null ? s.getRegions().stream() : java.util.stream.Stream.empty())
                    .map(r -> r.getRegion())
                    .distinct()
                    .count();
            
            // Calculate total requests from all regions
            long totalRequests = services.stream()
                    .flatMap(s -> s.getRegions() != null ? s.getRegions().stream() : java.util.stream.Stream.empty())
                    .mapToLong(r -> r.getRequestsPerSecond() != null ? r.getRequestsPerSecond().longValue() : 0L)
                    .sum();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalServices", totalServices);
            stats.put("healthyServices", healthyServices);
            stats.put("activeRegions", activeRegions);
            stats.put("totalRequests", totalRequests);
            data.put("stats", stats);
            
            // Get control loop status
            if (leaderElectionService != null) {
                Map<String, Object> controlLoop = new HashMap<>();
                controlLoop.put("isLeader", leaderElectionService.isLeader());
                controlLoop.put("status", leaderElectionService.getStatus());
                data.put("controlLoop", controlLoop);
            }
            
            // Get recent audit logs
            if (auditLogRepository != null) {
                var recentLogs = auditLogRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(0, 10,
                                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp"))
                ).getContent();
                data.put("recentActions", recentLogs);
            }
            
            data.put("timestamp", java.time.Instant.now().toString());
            
        } catch (Exception e) {
            log.debug("Error collecting dashboard data: {}", e.getMessage());
            data.put("error", e.getMessage());
        }
        
        return data;
    }

    private void sendDashboardData(WebSocketSession session) {
        try {
            Map<String, Object> dashboardData = collectDashboardData();
            WsMessage message = new WsMessage("dashboard_update", dashboardData);
            sendMessage(session, message);
        } catch (Exception e) {
            log.debug("Failed to send dashboard data to session {}: {}", session.getId(), e.getMessage());
        }
    }

    private void broadcastToDashboardSubscribers(WsMessage message) {
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize dashboard message: {}", e.getMessage());
            return;
        }

        for (String sessionId : dashboardSubscribers) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    log.debug("Failed to send dashboard message to session {}: {}", sessionId, e.getMessage());
                }
            }
        }
    }

    // DTOs
    public record WsCommand(String action, String serviceId) {}
    public record WsMessage(String type, Object data) {}
}
