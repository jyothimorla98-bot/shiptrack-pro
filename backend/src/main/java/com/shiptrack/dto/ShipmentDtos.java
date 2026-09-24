package com.shiptrack.dto;

import com.shiptrack.model.Address;
import com.shiptrack.model.GeoPoint;
import com.shiptrack.model.PackageDetails;
import com.shiptrack.model.Shipment;
import com.shiptrack.model.ShipmentStatus;
import com.shiptrack.model.TrackingEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class ShipmentDtos {

    private ShipmentDtos() {}

    public record CreateShipmentRequest(
            @Valid @NotNull Address sender,
            @Valid @NotNull Address receiver,
            @Valid @NotNull PackageDetails packageDetails,
            String serviceType,
            String notes
    ) {}

    public record UpdateShipmentRequest(
            Address sender,
            Address receiver,
            PackageDetails packageDetails,
            String serviceType,
            String notes
    ) {}

    public record StatusUpdateRequest(
            @NotNull ShipmentStatus status,
            String description,
            Double latitude,
            Double longitude,
            String locationLabel
    ) {}

    public record LocationUpdateRequest(
            @NotNull Double latitude,
            @NotNull Double longitude,
            String label
    ) {}

    public record AssignDriverRequest(@NotNull String driverId) {}

    public record CancelShipmentRequest(String reason) {}

    public record ShipmentResponse(
            String id,
            String trackingNumber,
            String ownerId,
            String ownerEmail,
            Address sender,
            Address receiver,
            PackageDetails packageDetails,
            ShipmentStatus status,
            List<ShipmentStatus> allowedNextStatuses,
            String serviceType,
            GeoPoint currentLocation,
            String assignedDriverId,
            String assignedDriverName,
            String routeId,
            String podId,
            Double distanceKm,
            Instant pickedUpAt,
            Instant estimatedDeliveryAt,
            Instant originalEstimatedDeliveryAt,
            Instant actualDeliveryAt,
            boolean delayed,
            long delayMinutes,
            String cancellationReason,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ShipmentResponse from(Shipment s) {
            return new ShipmentResponse(s.getId(), s.getTrackingNumber(), s.getOwnerId(), s.getOwnerEmail(),
                    s.getSender(), s.getReceiver(), s.getPackageDetails(), s.getStatus(),
                    s.getStatus().allowedNext(), s.getServiceType(), s.getCurrentLocation(),
                    s.getAssignedDriverId(), s.getAssignedDriverName(), s.getRouteId(), s.getPodId(),
                    s.getDistanceKm(), s.getPickedUpAt(), s.getEstimatedDeliveryAt(),
                    s.getOriginalEstimatedDeliveryAt(), s.getActualDeliveryAt(), s.isDelayed(),
                    s.getDelayMinutes(), s.getCancellationReason(), s.getNotes(),
                    s.getCreatedAt(), s.getUpdatedAt());
        }
    }

    /** What a customer sees on the public tracking page: no internal ids, no sender contact data. */
    public record PublicTrackingResponse(
            String trackingNumber,
            ShipmentStatus status,
            String origin,
            String destination,
            String serviceType,
            GeoPoint currentLocation,
            Instant estimatedDeliveryAt,
            Instant actualDeliveryAt,
            boolean delayed,
            long delayMinutes,
            List<TrackingEvent> timeline
    ) {}

    public record EtaResponse(
            String trackingNumber,
            Instant estimatedDeliveryAt,
            Instant originalEstimatedDeliveryAt,
            long remainingMinutes,
            double remainingDistanceKm,
            boolean delayed,
            long delayMinutes,
            double confidence,
            String explanation
    ) {}
}
