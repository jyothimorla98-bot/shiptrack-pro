package com.shiptrack.dto;

import java.util.List;
import java.util.Map;

public final class AnalyticsDtos {

    private AnalyticsDtos() {}

    public record CountByLabel(String label, long count) {}

    public record TrendPoint(String date, long created, long delivered) {}

    public record CustomerSummary(
            long activeShipments,
            long deliveredShipments,
            long inTransit,
            long delayed,
            long unreadNotifications,
            Map<String, Long> statusBreakdown,
            List<TrendPoint> trend
    ) {}

    public record BusinessSummary(
            long totalShipments,
            long delivered,
            long inFlight,
            long delayed,
            long failed,
            double onTimeRatePercent,
            double averageDeliveryHours,
            Map<String, Long> statusBreakdown,
            List<CountByLabel> topDestinations,
            List<TrendPoint> trend
    ) {}

    public record AdminSummary(
            long totalUsers,
            long totalShipments,
            long activeShipments,
            long delivered,
            long delayed,
            long pendingPodVerifications,
            double onTimeRatePercent,
            Map<String, Long> usersByRole,
            Map<String, Long> statusBreakdown,
            List<CountByLabel> operatorPerformance,
            List<TrendPoint> trend
    ) {}

    public record RoutePerformance(
            String trackingNumber,
            String driverName,
            double distanceKm,
            Integer plannedMinutes,
            Long actualMinutes,
            boolean delayed
    ) {}
}
