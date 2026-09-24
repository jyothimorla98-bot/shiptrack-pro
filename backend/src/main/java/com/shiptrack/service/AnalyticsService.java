package com.shiptrack.service;

import com.shiptrack.dto.AnalyticsDtos;
import com.shiptrack.model.*;
import com.shiptrack.repository.*;
import com.shiptrack.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ProofOfDeliveryRepository podRepository;
    private final RoutePlanRepository routePlanRepository;

    public AnalyticsService(ShipmentRepository shipmentRepository,
                            UserRepository userRepository,
                            NotificationRepository notificationRepository,
                            ProofOfDeliveryRepository podRepository,
                            RoutePlanRepository routePlanRepository) {
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.podRepository = podRepository;
        this.routePlanRepository = routePlanRepository;
    }

    // ------------------------------------------------------------- dashboards

    public AnalyticsDtos.CustomerSummary customerSummary(UserPrincipal principal) {
        List<Shipment> mine = shipmentRepository.findByOwnerId(principal.getId());

        long active = mine.stream().filter(s -> !s.getStatus().isTerminal()).count();
        long delivered = mine.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERED).count();
        long inTransit = mine.stream().filter(s -> s.getStatus() == ShipmentStatus.IN_TRANSIT
                || s.getStatus() == ShipmentStatus.OUT_FOR_DELIVERY).count();
        long delayed = mine.stream().filter(Shipment::isDelayed).count();

        return new AnalyticsDtos.CustomerSummary(
                active, delivered, inTransit, delayed,
                notificationRepository.countByUserIdAndReadFalse(principal.getId()),
                statusBreakdown(mine),
                trend(mine, 14));
    }

    public AnalyticsDtos.BusinessSummary businessSummary(UserPrincipal principal) {
        List<Shipment> scope = principal.isStaff()
                ? shipmentRepository.findAll()
                : shipmentRepository.findByOwnerId(principal.getId());

        long delivered = scope.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERED).count();
        long inFlight = scope.stream().filter(s -> !s.getStatus().isTerminal()).count();
        long delayed = scope.stream().filter(Shipment::isDelayed).count();
        long failed = scope.stream().filter(s -> s.getStatus() == ShipmentStatus.FAILED_DELIVERY).count();

        return new AnalyticsDtos.BusinessSummary(
                scope.size(), delivered, inFlight, delayed, failed,
                onTimeRate(scope),
                averageDeliveryHours(scope),
                statusBreakdown(scope),
                topDestinations(scope, 5),
                trend(scope, 14));
    }

    public AnalyticsDtos.AdminSummary adminSummary() {
        List<Shipment> all = shipmentRepository.findAll();

        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            usersByRole.put(role.name(), userRepository.countByRole(role));
        }

        return new AnalyticsDtos.AdminSummary(
                userRepository.count(),
                all.size(),
                all.stream().filter(s -> !s.getStatus().isTerminal()).count(),
                all.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERED).count(),
                all.stream().filter(Shipment::isDelayed).count(),
                podRepository.countByVerified(false),
                onTimeRate(all),
                usersByRole,
                statusBreakdown(all),
                operatorPerformance(all),
                trend(all, 30));
    }

    // ----------------------------------------------------------------- pieces

    public Map<String, Long> statusBreakdown(List<Shipment> shipments) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ShipmentStatus status : ShipmentStatus.values()) {
            counts.put(status.name(), 0L);
        }
        for (Shipment shipment : shipments) {
            counts.merge(shipment.getStatus().name(), 1L, Long::sum);
        }
        return counts;
    }

    /** Share of delivered shipments that landed on or before the original promised time. */
    public double onTimeRate(List<Shipment> shipments) {
        List<Shipment> delivered = shipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.DELIVERED && s.getActualDeliveryAt() != null)
                .toList();
        if (delivered.isEmpty()) return 100d;

        long onTime = delivered.stream()
                .filter(s -> s.getOriginalEstimatedDeliveryAt() == null
                        || !s.getActualDeliveryAt().isAfter(s.getOriginalEstimatedDeliveryAt()))
                .count();
        return Math.round((onTime * 1000d / delivered.size())) / 10d;
    }

    public double averageDeliveryHours(List<Shipment> shipments) {
        List<Shipment> delivered = shipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.DELIVERED && s.getActualDeliveryAt() != null)
                .toList();
        if (delivered.isEmpty()) return 0d;

        double totalHours = delivered.stream()
                .mapToDouble(s -> Duration.between(s.getCreatedAt(), s.getActualDeliveryAt()).toMinutes() / 60d)
                .sum();
        return Math.round((totalHours / delivered.size()) * 10d) / 10d;
    }

    public List<AnalyticsDtos.CountByLabel> topDestinations(List<Shipment> shipments, int limit) {
        return shipments.stream()
                .filter(s -> s.getReceiver() != null && s.getReceiver().getCity() != null)
                .collect(Collectors.groupingBy(s -> s.getReceiver().getCity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new AnalyticsDtos.CountByLabel(e.getKey(), e.getValue()))
                .toList();
    }

    public List<AnalyticsDtos.CountByLabel> operatorPerformance(List<Shipment> shipments) {
        return shipments.stream()
                .filter(s -> s.getAssignedDriverName() != null && s.getStatus() == ShipmentStatus.DELIVERED)
                .collect(Collectors.groupingBy(Shipment::getAssignedDriverName, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(e -> new AnalyticsDtos.CountByLabel(e.getKey(), e.getValue()))
                .toList();
    }

    /** Created versus delivered counts per day for the last n days. */
    public List<AnalyticsDtos.TrendPoint> trend(List<Shipment> shipments, int days) {
        List<AnalyticsDtos.TrendPoint> points = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Instant from = day.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = from.plus(Duration.ofDays(1));

            long created = shipments.stream()
                    .filter(s -> s.getCreatedAt() != null
                            && !s.getCreatedAt().isBefore(from) && s.getCreatedAt().isBefore(to))
                    .count();
            long delivered = shipments.stream()
                    .filter(s -> s.getActualDeliveryAt() != null
                            && !s.getActualDeliveryAt().isBefore(from) && s.getActualDeliveryAt().isBefore(to))
                    .count();
            points.add(new AnalyticsDtos.TrendPoint(day.toString(), created, delivered));
        }
        return points;
    }

    public List<AnalyticsDtos.RoutePerformance> routePerformance() {
        List<RoutePlan> plans = routePlanRepository.findAll();
        Map<String, Shipment> shipments = shipmentRepository.findAll().stream()
                .collect(Collectors.toMap(Shipment::getId, s -> s, (a, b) -> a));

        List<AnalyticsDtos.RoutePerformance> rows = new ArrayList<>();
        for (RoutePlan plan : plans) {
            Shipment shipment = shipments.get(plan.getShipmentId());
            if (shipment == null) continue;

            Long actual = null;
            if (shipment.getPickedUpAt() != null && shipment.getActualDeliveryAt() != null) {
                actual = Duration.between(shipment.getPickedUpAt(), shipment.getActualDeliveryAt()).toMinutes();
            }
            rows.add(new AnalyticsDtos.RoutePerformance(
                    shipment.getTrackingNumber(),
                    plan.getDriverName() == null ? "Unassigned" : plan.getDriverName(),
                    plan.getDistanceKm() == null ? 0d : plan.getDistanceKm(),
                    plan.getEstimatedDurationMinutes(),
                    actual,
                    shipment.isDelayed()));
        }
        rows.sort(Comparator.comparingDouble(AnalyticsDtos.RoutePerformance::distanceKm).reversed());
        return rows;
    }
}
