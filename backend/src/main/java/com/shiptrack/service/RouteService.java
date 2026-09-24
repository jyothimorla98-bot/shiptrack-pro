package com.shiptrack.service;

import com.shiptrack.dto.RouteDtos;
import com.shiptrack.exception.ApiException;
import com.shiptrack.model.GeoPoint;
import com.shiptrack.model.RoutePlan;
import com.shiptrack.model.Shipment;
import com.shiptrack.repository.RoutePlanRepository;
import com.shiptrack.repository.UserRepository;
import com.shiptrack.util.GeoUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class RouteService {

    private final RoutePlanRepository routePlanRepository;
    private final UserRepository userRepository;

    public RouteService(RoutePlanRepository routePlanRepository, UserRepository userRepository) {
        this.routePlanRepository = routePlanRepository;
        this.userRepository = userRepository;
    }

    /** Builds a straight-line corridor between origin and destination with intermediate waypoints. */
    public RoutePlan planFor(Shipment shipment) {
        GeoPoint origin = pointFrom(shipment, true);
        GeoPoint destination = pointFrom(shipment, false);

        List<GeoPoint> waypoints = new ArrayList<>();
        if (origin != null && destination != null) {
            waypoints.add(origin);
            waypoints.addAll(GeoUtils.interpolate(origin, destination, 5));
            waypoints.add(destination);
        }

        double distance = GeoUtils.pathLengthKm(waypoints);
        double traffic = 1.05 + Math.min(distance / 4000d, 0.45d);

        RoutePlan plan = routePlanRepository.findByShipmentId(shipment.getId())
                .orElseGet(() -> RoutePlan.builder().shipmentId(shipment.getId()).build());

        plan.setTrackingNumber(shipment.getTrackingNumber());
        plan.setWaypoints(waypoints);
        plan.setDistanceKm(GeoUtils.roundKm(distance));
        plan.setTrafficFactor(Math.round(traffic * 100) / 100d);
        plan.setEstimatedDurationMinutes((int) Math.round((distance / 46d) * traffic * 60));
        plan.setStatus("PLANNED");
        plan.setUpdatedAt(Instant.now());

        if (plan.getTravelledPath() == null) plan.setTravelledPath(new ArrayList<>());
        if (origin != null && plan.getTravelledPath().isEmpty()) {
            plan.getTravelledPath().add(origin);
        }
        return routePlanRepository.save(plan);
    }

    public RoutePlan replan(Shipment shipment, RouteDtos.PlanRouteRequest request) {
        RoutePlan plan = routePlanRepository.findByShipmentId(shipment.getId())
                .orElseGet(() -> planFor(shipment));

        if (request.waypoints() != null && request.waypoints().size() >= 2) {
            plan.setWaypoints(new ArrayList<>(request.waypoints()));
            plan.setDistanceKm(GeoUtils.roundKm(GeoUtils.pathLengthKm(plan.getWaypoints())));
        }
        if (request.trafficFactor() != null && request.trafficFactor() > 0) {
            plan.setTrafficFactor(request.trafficFactor());
        }
        if (request.driverId() != null && !request.driverId().isBlank()) {
            plan.setDriverId(request.driverId());
            userRepository.findById(request.driverId())
                    .ifPresent(driver -> plan.setDriverName(driver.getFullName()));
        }
        plan.setEstimatedDurationMinutes((int) Math.round((plan.getDistanceKm() / 46d) * plan.getTrafficFactor() * 60));
        plan.setUpdatedAt(Instant.now());
        return routePlanRepository.save(plan);
    }

    /** Reorders intermediate stops with a nearest-neighbour pass, keeping origin and destination fixed. */
    public RoutePlan optimize(String shipmentId) {
        RoutePlan plan = getByShipment(shipmentId);
        List<GeoPoint> waypoints = plan.getWaypoints();
        if (waypoints == null || waypoints.size() < 4) {
            plan.setOptimized(true);
            return routePlanRepository.save(plan);
        }

        GeoPoint origin = waypoints.get(0);
        GeoPoint destination = waypoints.get(waypoints.size() - 1);
        List<GeoPoint> middle = new ArrayList<>(waypoints.subList(1, waypoints.size() - 1));

        double before = GeoUtils.pathLengthKm(waypoints);

        List<GeoPoint> ordered = new ArrayList<>();
        ordered.add(origin);
        ordered.addAll(GeoUtils.nearestNeighbourOrder(middle));
        ordered.add(destination);

        double after = GeoUtils.pathLengthKm(ordered);
        if (after <= before) {
            plan.setWaypoints(ordered);
            plan.setDistanceKm(GeoUtils.roundKm(after));
        }
        plan.setOptimized(true);
        plan.setEstimatedDurationMinutes((int) Math.round((plan.getDistanceKm() / 46d) * plan.getTrafficFactor() * 60));
        plan.setUpdatedAt(Instant.now());
        return routePlanRepository.save(plan);
    }

    public RoutePlan recordProgress(String shipmentId, GeoPoint point) {
        RoutePlan plan = getByShipment(shipmentId);
        if (plan.getTravelledPath() == null) plan.setTravelledPath(new ArrayList<>());
        plan.getTravelledPath().add(point);
        plan.setStatus("IN_PROGRESS");
        plan.setUpdatedAt(Instant.now());
        return routePlanRepository.save(plan);
    }

    public RoutePlan complete(String shipmentId) {
        return routePlanRepository.findByShipmentId(shipmentId)
                .map(plan -> {
                    plan.setStatus("COMPLETED");
                    plan.setUpdatedAt(Instant.now());
                    return routePlanRepository.save(plan);
                })
                .orElse(null);
    }

    public RoutePlan getByShipment(String shipmentId) {
        return routePlanRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> ApiException.notFound("No route has been planned for this shipment"));
    }

    public List<RoutePlan> forDriver(String driverId) {
        return routePlanRepository.findByDriverId(driverId);
    }

    public List<RoutePlan> all() {
        return routePlanRepository.findAll();
    }

    public void deleteForShipment(String shipmentId) {
        routePlanRepository.deleteByShipmentId(shipmentId);
    }

    private GeoPoint pointFrom(Shipment shipment, boolean origin) {
        var address = origin ? shipment.getSender() : shipment.getReceiver();
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) return null;
        return GeoPoint.of(address.getLatitude(), address.getLongitude(), address.getCity());
    }
}
