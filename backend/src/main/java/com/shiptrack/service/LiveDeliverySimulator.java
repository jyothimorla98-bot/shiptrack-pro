package com.shiptrack.service;

import com.shiptrack.dto.ShipmentDtos;
import com.shiptrack.model.GeoPoint;
import com.shiptrack.model.RoutePlan;
import com.shiptrack.model.Shipment;
import com.shiptrack.model.ShipmentStatus;
import com.shiptrack.repository.RoutePlanRepository;
import com.shiptrack.repository.ShipmentRepository;
import com.shiptrack.util.GeoUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Nudges in-transit shipments along their planned route every few seconds so the live map
 * has something to show without real telematics hardware. Turn it off with
 * app.simulation.enabled=false and post real positions to the location endpoint instead.
 */
@Service
public class LiveDeliverySimulator {

    private final ShipmentRepository shipmentRepository;
    private final RoutePlanRepository routePlanRepository;
    private final EtaService etaService;
    private final SimpMessagingTemplate messagingTemplate;
    private final boolean enabled;

    public LiveDeliverySimulator(ShipmentRepository shipmentRepository,
                                 RoutePlanRepository routePlanRepository,
                                 EtaService etaService,
                                 SimpMessagingTemplate messagingTemplate,
                                 @Value("${app.simulation.enabled:true}") boolean enabled) {
        this.shipmentRepository = shipmentRepository;
        this.routePlanRepository = routePlanRepository;
        this.etaService = etaService;
        this.messagingTemplate = messagingTemplate;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${app.simulation.interval-ms:8000}")
    public void advanceActiveShipments() {
        if (!enabled) return;

        List<Shipment> moving = shipmentRepository.findByStatusIn(
                List.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY));

        for (Shipment shipment : moving) {
            RoutePlan plan = routePlanRepository.findByShipmentId(shipment.getId()).orElse(null);
            if (plan == null || plan.getWaypoints() == null || plan.getWaypoints().size() < 2) continue;

            GeoPoint destination = plan.getWaypoints().get(plan.getWaypoints().size() - 1);
            GeoPoint current = shipment.getCurrentLocation() == null
                    ? plan.getWaypoints().get(0)
                    : shipment.getCurrentLocation();

            double remaining = GeoUtils.distanceKm(current, destination);
            if (remaining < 0.5) continue;

            // Move a slice of the remaining leg, capped so long hauls do not teleport.
            double step = Math.min(0.06, 12d / Math.max(remaining, 1d));
            double lat = current.getLatitude() + (destination.getLatitude() - current.getLatitude()) * step;
            double lng = current.getLongitude() + (destination.getLongitude() - current.getLongitude()) * step;

            GeoPoint next = GeoPoint.of(GeoUtils.round(lat), GeoUtils.round(lng),
                    remaining < 25 ? "Approaching " + destination.getLabel() : "En route");

            shipment.setCurrentLocation(next);
            etaService.refresh(shipment);
            shipment.setUpdatedAt(Instant.now());
            Shipment saved = shipmentRepository.save(shipment);

            plan.getTravelledPath().add(next);
            plan.setStatus("IN_PROGRESS");
            plan.setUpdatedAt(Instant.now());
            routePlanRepository.save(plan);

            ShipmentDtos.ShipmentResponse payload = ShipmentDtos.ShipmentResponse.from(saved);
            messagingTemplate.convertAndSend("/topic/shipments/" + saved.getTrackingNumber(), payload);
            messagingTemplate.convertAndSend("/topic/fleet", payload);
        }
    }
}
