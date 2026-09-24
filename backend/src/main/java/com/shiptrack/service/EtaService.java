package com.shiptrack.service;

import com.shiptrack.dto.ShipmentDtos;
import com.shiptrack.model.GeoPoint;
import com.shiptrack.model.RoutePlan;
import com.shiptrack.model.Shipment;
import com.shiptrack.model.ShipmentStatus;
import com.shiptrack.repository.RoutePlanRepository;
import com.shiptrack.repository.ShipmentRepository;
import com.shiptrack.util.GeoUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * ETA model: handling time for the service level, plus travel time over the remaining
 * distance at a speed that is discounted by live traffic and by how this lane has
 * historically performed.
 */
@Service
public class EtaService {

    private final ShipmentRepository shipmentRepository;
    private final RoutePlanRepository routePlanRepository;

    public EtaService(ShipmentRepository shipmentRepository, RoutePlanRepository routePlanRepository) {
        this.shipmentRepository = shipmentRepository;
        this.routePlanRepository = routePlanRepository;
    }

    public double baseSpeedKmh(String serviceType) {
        return switch (serviceType == null ? "STANDARD" : serviceType.toUpperCase()) {
            case "EXPRESS" -> 62d;
            case "SAME_DAY" -> 34d;
            default -> 46d;
        };
    }

    public double handlingHours(String serviceType) {
        return switch (serviceType == null ? "STANDARD" : serviceType.toUpperCase()) {
            case "EXPRESS" -> 3.5d;
            case "SAME_DAY" -> 1d;
            default -> 11d;
        };
    }

    /** Hours a driver is off the road per 24 hours of a long haul. */
    private double restHours(double travelHours) {
        return Math.floor(travelHours / 9d) * 8d;
    }

    public double trafficFactor(Shipment shipment) {
        return routePlanRepository.findByShipmentId(shipment.getId())
                .map(RoutePlan::getTrafficFactor)
                .filter(f -> f != null && f > 0)
                .orElse(1.15d);
    }

    /** Average over-run on recently delivered shipments of the same service level. */
    public double historicalDelayFactor(String serviceType) {
        List<Shipment> delivered = shipmentRepository.findByStatus(ShipmentStatus.DELIVERED).stream()
                .filter(s -> serviceType == null || serviceType.equalsIgnoreCase(s.getServiceType()))
                .filter(s -> s.getActualDeliveryAt() != null && s.getOriginalEstimatedDeliveryAt() != null)
                .sorted(Comparator.comparing(Shipment::getActualDeliveryAt).reversed())
                .limit(50)
                .toList();

        if (delivered.size() < 3) return 1.0d;

        double totalRatio = 0d;
        int counted = 0;
        for (Shipment s : delivered) {
            long promised = Duration.between(s.getCreatedAt(), s.getOriginalEstimatedDeliveryAt()).toMinutes();
            long actual = Duration.between(s.getCreatedAt(), s.getActualDeliveryAt()).toMinutes();
            if (promised > 0 && actual > 0) {
                totalRatio += (double) actual / promised;
                counted++;
            }
        }
        if (counted == 0) return 1.0d;
        double factor = totalRatio / counted;
        return Math.min(Math.max(factor, 0.85d), 1.5d);
    }

    public double remainingDistanceKm(Shipment shipment) {
        GeoPoint destination = toPoint(shipment.getReceiver() == null ? null : shipment.getReceiver().getLatitude(),
                shipment.getReceiver() == null ? null : shipment.getReceiver().getLongitude());
        GeoPoint from = shipment.getCurrentLocation() != null
                ? shipment.getCurrentLocation()
                : toPoint(shipment.getSender() == null ? null : shipment.getSender().getLatitude(),
                          shipment.getSender() == null ? null : shipment.getSender().getLongitude());

        if (from == null || destination == null) {
            return shipment.getDistanceKm() == null ? 0d : shipment.getDistanceKm();
        }
        return GeoUtils.roundKm(GeoUtils.distanceKm(from, destination));
    }

    public double totalDistanceKm(Shipment shipment) {
        GeoPoint origin = toPoint(shipment.getSender() == null ? null : shipment.getSender().getLatitude(),
                shipment.getSender() == null ? null : shipment.getSender().getLongitude());
        GeoPoint destination = toPoint(shipment.getReceiver() == null ? null : shipment.getReceiver().getLatitude(),
                shipment.getReceiver() == null ? null : shipment.getReceiver().getLongitude());
        if (origin == null || destination == null) return 0d;
        return GeoUtils.roundKm(GeoUtils.distanceKm(origin, destination));
    }

    private GeoPoint toPoint(Double lat, Double lng) {
        if (lat == null || lng == null) return null;
        return GeoPoint.of(lat, lng, null);
    }

    /** Minutes still needed to complete the delivery from right now. */
    public long remainingMinutes(Shipment shipment) {
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) return 0;

        double distance = remainingDistanceKm(shipment);
        double speed = baseSpeedKmh(shipment.getServiceType()) / trafficFactor(shipment);
        double travelHours = speed <= 0 ? 0 : distance / speed;
        double handling = switch (shipment.getStatus()) {
            case CREATED -> handlingHours(shipment.getServiceType());
            case PICKED_UP -> handlingHours(shipment.getServiceType()) * 0.4;
            case OUT_FOR_DELIVERY -> 0.4;
            default -> 0.8;
        };
        double total = (travelHours + restHours(travelHours) + handling) * historicalDelayFactor(shipment.getServiceType());
        return Math.max(Math.round(total * 60), 0);
    }

    public Instant calculateEta(Shipment shipment) {
        return Instant.now().plus(Duration.ofMinutes(remainingMinutes(shipment)));
    }

    /** Recalculates the ETA in place and flags the shipment when it slips past the original promise. */
    public Shipment refresh(Shipment shipment) {
        if (shipment.getStatus().isTerminal()) return shipment;

        Instant eta = calculateEta(shipment);
        shipment.setEstimatedDeliveryAt(eta);
        shipment.setDistanceKm(totalDistanceKm(shipment));

        if (shipment.getOriginalEstimatedDeliveryAt() == null) {
            shipment.setOriginalEstimatedDeliveryAt(eta);
        }

        long slip = Duration.between(shipment.getOriginalEstimatedDeliveryAt(), eta).toMinutes();
        shipment.setDelayMinutes(Math.max(slip, 0));
        shipment.setDelayed(slip > 30);
        shipment.setUpdatedAt(Instant.now());
        return shipment;
    }

    public ShipmentDtos.EtaResponse describe(Shipment shipment) {
        long minutes = remainingMinutes(shipment);
        double distance = remainingDistanceKm(shipment);
        double traffic = trafficFactor(shipment);
        double history = historicalDelayFactor(shipment.getServiceType());

        double confidence = 0.95d - (traffic - 1) * 0.35d - Math.abs(history - 1) * 0.3d
                - (distance > 800 ? 0.08d : 0d);
        confidence = Math.min(Math.max(confidence, 0.35d), 0.97d);

        String explanation = String.format(
                "%.0f km remaining at about %.0f km/h effective speed. Traffic factor %.2f, historical lane performance %.2f.",
                distance, baseSpeedKmh(shipment.getServiceType()) / traffic, traffic, history);

        return new ShipmentDtos.EtaResponse(
                shipment.getTrackingNumber(),
                shipment.getEstimatedDeliveryAt(),
                shipment.getOriginalEstimatedDeliveryAt(),
                minutes,
                distance,
                shipment.isDelayed(),
                shipment.getDelayMinutes(),
                Math.round(confidence * 100) / 100d,
                explanation);
    }

    /** Shipments whose current ETA has already slipped past the promise, worst first. */
    public List<Shipment> delayRiskList() {
        return shipmentRepository.findByStatusIn(List.of(
                        ShipmentStatus.CREATED, ShipmentStatus.PICKED_UP,
                        ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY))
                .stream()
                .map(this::refresh)
                .filter(Shipment::isDelayed)
                .sorted(Comparator.comparingLong(Shipment::getDelayMinutes).reversed())
                .toList();
    }

    public Optional<RoutePlan> routeFor(Shipment shipment) {
        return routePlanRepository.findByShipmentId(shipment.getId());
    }
}
