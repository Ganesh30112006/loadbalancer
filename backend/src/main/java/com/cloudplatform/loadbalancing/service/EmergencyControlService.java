package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.dto.EmergencyOverrideDto;
import com.cloudplatform.loadbalancing.dto.ScalingEventDto;
import com.cloudplatform.loadbalancing.entity.EmergencyOverride;
import com.cloudplatform.loadbalancing.entity.ScalingEvent;
import com.cloudplatform.loadbalancing.entity.Service;
import com.cloudplatform.loadbalancing.repository.EmergencyOverrideRepository;
import com.cloudplatform.loadbalancing.repository.ScalingEventRepository;
import com.cloudplatform.loadbalancing.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class EmergencyControlService {

    private final ServiceRepository serviceRepository;
    private final EmergencyOverrideRepository overrideRepository;
    private final ScalingEventRepository scalingEventRepository;

    /**
     * Pause automation for a specific service
     */
    @Transactional
    public EmergencyOverrideDto pauseServiceAutomation(UUID serviceId, String reason, String initiatedBy) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        boolean previousState = service.getAutomationEnabled();
        service.setAutomationEnabled(false);
        serviceRepository.save(service);

        EmergencyOverride override = EmergencyOverride.builder()
                .service(service)
                .overrideType(EmergencyOverride.OverrideType.PAUSE_AUTOMATION)
                .scope(EmergencyOverride.OverrideScope.SERVICE)
                .status(EmergencyOverride.OverrideStatus.ACTIVE)
                .reason(reason)
                .previousAutomationState(previousState)
                .initiatedBy(initiatedBy)
                .build();

        override = overrideRepository.save(override);
        log.info("Automation paused for service {} by {} - Reason: {}", 
                service.getServiceName(), initiatedBy, reason);

        return EmergencyOverrideDto.fromEntity(override);
    }

    /**
     * Pause automation globally for all services
     */
    @Transactional
    public EmergencyOverrideDto pauseGlobalAutomation(String reason, String initiatedBy) {
        // Disable automation on all active services
        List<Service> activeServices = serviceRepository.findAll().stream()
                .filter(s -> s.getAutomationEnabled())
                .collect(Collectors.toList());

        for (Service service : activeServices) {
            service.setAutomationEnabled(false);
            serviceRepository.save(service);
        }

        EmergencyOverride override = EmergencyOverride.builder()
                .overrideType(EmergencyOverride.OverrideType.PAUSE_AUTOMATION)
                .scope(EmergencyOverride.OverrideScope.GLOBAL)
                .status(EmergencyOverride.OverrideStatus.ACTIVE)
                .reason(reason)
                .initiatedBy(initiatedBy)
                .build();

        override = overrideRepository.save(override);
        log.warn("GLOBAL automation pause by {} - Reason: {} - {} services affected", 
                initiatedBy, reason, activeServices.size());

        return EmergencyOverrideDto.fromEntity(override);
    }

    /**
     * Manual capacity override
     */
    @Transactional
    public EmergencyOverrideDto setCapacityOverride(UUID serviceId, Integer targetCapacity, 
                                                      String reason, String initiatedBy) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        Integer previousCapacity = service.getCurrentDesiredCapacity();

        // Create scaling event for the capacity change
        ScalingEvent scalingEvent = ScalingEvent.builder()
                .service(service)
                .region(service.getPrimaryRegion())
                .eventType(ScalingEvent.EventType.MANUAL_OVERRIDE)
                .eventSource(ScalingEvent.EventSource.MANUAL)
                .status(ScalingEvent.EventStatus.PENDING)
                .previousCapacity(previousCapacity != null ? previousCapacity : 0)
                .targetCapacity(targetCapacity)
                .reason(reason)
                .triggeredBy(initiatedBy)
                .isManualOverride(true)
                .overrideReason(reason)
                .startedAt(Instant.now())
                .build();

        scalingEventRepository.save(scalingEvent);

        // Create override record
        EmergencyOverride override = EmergencyOverride.builder()
                .service(service)
                .overrideType(EmergencyOverride.OverrideType.CAPACITY_OVERRIDE)
                .scope(EmergencyOverride.OverrideScope.SERVICE)
                .status(EmergencyOverride.OverrideStatus.ACTIVE)
                .reason(reason)
                .overrideCapacity(targetCapacity)
                .previousCapacity(previousCapacity)
                .initiatedBy(initiatedBy)
                .build();

        override = overrideRepository.save(override);
        log.info("Capacity override for service {} by {}: {} → {} - Reason: {}", 
                service.getServiceName(), initiatedBy, previousCapacity, targetCapacity, reason);

        return EmergencyOverrideDto.fromEntity(override);
    }

    /**
     * Lift an active override
     */
    @Transactional
    public EmergencyOverrideDto liftOverride(UUID overrideId, String liftReason, String liftedBy) {
        EmergencyOverride override = overrideRepository.findById(overrideId)
                .orElseThrow(() -> new IllegalArgumentException("Override not found"));

        if (override.getStatus() != EmergencyOverride.OverrideStatus.ACTIVE) {
            throw new IllegalStateException("Override is not active");
        }

        // Restore previous state if applicable
        if (override.getOverrideType() == EmergencyOverride.OverrideType.PAUSE_AUTOMATION 
                && override.getService() != null && override.getPreviousAutomationState() != null) {
            Service service = override.getService();
            service.setAutomationEnabled(override.getPreviousAutomationState());
            serviceRepository.save(service);
        }

        override.setStatus(EmergencyOverride.OverrideStatus.LIFTED);
        override.setLiftedAt(Instant.now());
        override.setLiftedBy(liftedBy);
        override.setLiftReason(liftReason);

        override = overrideRepository.save(override);
        log.info("Override {} lifted by {} - Reason: {}", overrideId, liftedBy, liftReason);

        return EmergencyOverrideDto.fromEntity(override);
    }

    /**
     * Get all active overrides
     */
    public List<EmergencyOverrideDto> getActiveOverrides() {
        return overrideRepository.findAllActive().stream()
                .map(EmergencyOverrideDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get active overrides for a service
     */
    public List<EmergencyOverrideDto> getActiveOverridesForService(UUID serviceId) {
        return overrideRepository.findActiveByServiceId(serviceId).stream()
                .map(EmergencyOverrideDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Check if automation is blocked for a service
     */
    public boolean isAutomationBlocked(UUID serviceId) {
        // Check for global pause
        if (!overrideRepository.findActiveGlobalOverrides().isEmpty()) {
            return true;
        }

        // Check for service-specific pause
        return !overrideRepository.findActiveByServiceId(serviceId).stream()
                .filter(o -> o.getOverrideType() == EmergencyOverride.OverrideType.PAUSE_AUTOMATION)
                .toList().isEmpty();
    }

    /**
     * Get scaling events for a service
     */
    public List<ScalingEventDto> getScalingEvents(UUID serviceId, int limit) {
        return scalingEventRepository.findByServiceIdOrderByCreatedAtDesc(serviceId).stream()
                .limit(limit)
                .map(ScalingEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Resume automation for a service
     */
    @Transactional
    public void resumeServiceAutomation(UUID serviceId, String resumedBy) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));

        // Lift all active pause overrides for this service
        List<EmergencyOverride> activeOverrides = overrideRepository.findActiveByServiceId(serviceId);
        for (EmergencyOverride override : activeOverrides) {
            if (override.getOverrideType() == EmergencyOverride.OverrideType.PAUSE_AUTOMATION) {
                override.setStatus(EmergencyOverride.OverrideStatus.LIFTED);
                override.setLiftedAt(Instant.now());
                override.setLiftedBy(resumedBy);
                override.setLiftReason("Automation resumed");
                overrideRepository.save(override);
            }
        }

        service.setAutomationEnabled(true);
        serviceRepository.save(service);

        log.info("Automation resumed for service {} by {}", service.getServiceName(), resumedBy);
    }
}
