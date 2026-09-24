package com.shiptrack.service;

import com.shiptrack.dto.ShipmentDtos;
import com.shiptrack.exception.ApiException;
import com.shiptrack.model.*;
import com.shiptrack.repository.ShipmentRepository;
import com.shiptrack.repository.TrackingEventRepository;
import com.shiptrack.repository.UserRepository;
import com.shiptrack.security.UserPrincipal;
import com.shiptrack.util.TrackingNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final UserRepository userRepository;
    private final EtaService etaService;
    private final RouteService routeService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           TrackingEventRepository trackingEventRepository,
                           UserRepository userRepository,
                           EtaService etaService,
                           RouteService routeService,
                           NotificationService notificationService,
                           SimpMessagingTemplate messagingTemplate) {
        this.shipmentRepository = shipmentRepository;
        this.trackingEventRepository = trackingEventRepository;
        this.userRepository = userRepository;
        this.etaService = etaService;
        this.routeService = routeService;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    // ---------------------------------------------------------------- create

    public Shipment create(ShipmentDtos.CreateShipmentRequest request, UserPrincipal principal) {
        String trackingNumber;
        do {
            trackingNumber = TrackingNumberGenerator.next();
        } while (shipmentRepository.existsByTrackingNumber(trackingNumber));

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .ownerId(principal.getId())
                .ownerEmail(principal.getUsername())
                .sender(request.sender())
                .receiver(request.receiver())
                .packageDetails(request.packageDetails())
                .serviceType(request.serviceType() == null ? "STANDARD" : request.serviceType().toUpperCase())
                .status(ShipmentStatus.CREATED)
                .notes(request.notes())
                .build();

        if (request.sender() != null && request.sender().getLatitude() != null) {
            shipment.setCurrentLocation(GeoPoint.of(request.sender().getLatitude(),
                    request.sender().getLongitude(), request.sender().getCity()));
        }

        etaService.refresh(shipment);
        Shipment saved = shipmentRepository.save(shipment);

        RoutePlan plan = routeService.planFor(saved);
        saved.setRouteId(plan.getId());
        saved.setDistanceKm(plan.getDistanceKm());
        saved = shipmentRepository.save(saved);

        recordEvent(saved, ShipmentStatus.CREATED,
                "Shipment booked and awaiting pickup", saved.getCurrentLocation(), principal.getUsername());

        notificationService.send(saved.getOwnerId(), NotificationType.SHIPMENT_CREATED,
                "Shipment " + saved.getTrackingNumber() + " created",
                "Your shipment to " + saved.getReceiver().shortText() + " is booked. We will let you know when it is picked up.",
                saved);

        return saved;
    }

    // ------------------------------------------------------------------ read

    public Shipment getById(String id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Shipment not found"));
    }

    public Shipment getByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber.trim().toUpperCase())
                .orElseThrow(() -> ApiException.notFound("No shipment matches tracking number " + trackingNumber));
    }

    public Shipment getAccessible(String id, UserPrincipal principal) {
        Shipment shipment = getById(id);
        assertCanView(shipment, principal);
        return shipment;
    }

    public void assertCanView(Shipment shipment, UserPrincipal principal) {
        if (principal.isStaff()) return;
        boolean owner = shipment.getOwnerId().equals(principal.getId());
        boolean recipient = shipment.getReceiver() != null
                && principal.getUsername().equalsIgnoreCase(shipment.getReceiver().getEmail());
        if (!owner && !recipient) {
            throw ApiException.forbidden("This shipment belongs to another account");
        }
    }

    public void assertCanEdit(Shipment shipment, UserPrincipal principal) {
        if (principal.isStaff()) return;
        if (!shipment.getOwnerId().equals(principal.getId())) {
            throw ApiException.forbidden("Only the sender or an operator can change this shipment");
        }
    }

    /** Customers and business clients see their own book; staff see everything. */
    public Page<Shipment> list(UserPrincipal principal, ShipmentStatus status, Pageable pageable) {
        if (principal.isStaff()) {
            return status == null
                    ? shipmentRepository.findAll(pageable)
                    : shipmentRepository.findByStatus(status, pageable);
        }
        return status == null
                ? shipmentRepository.findByOwnerId(principal.getId(), pageable)
                : shipmentRepository.findByOwnerIdAndStatus(principal.getId(), status, pageable);
    }

    public List<Shipment> activeShipments() {
        return shipmentRepository.findByStatusIn(List.of(
                ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY));
    }

    public List<TrackingEvent> timeline(String shipmentId) {
        return trackingEventRepository.findByShipmentIdOrderByOccurredAtAsc(shipmentId);
    }

    // ---------------------------------------------------------------- update

    public Shipment update(String id, ShipmentDtos.UpdateShipmentRequest request, UserPrincipal principal) {
        Shipment shipment = getById(id);
        assertCanEdit(shipment, principal);

        if (shipment.getStatus() != ShipmentStatus.CREATED && !principal.isStaff()) {
            throw ApiException.badRequest("Details can only be changed before the shipment is picked up");
        }

        if (request.sender() != null) shipment.setSender(request.sender());
        if (request.receiver() != null) shipment.setReceiver(request.receiver());
        if (request.packageDetails() != null) shipment.setPackageDetails(request.packageDetails());
        if (request.serviceType() != null) shipment.setServiceType(request.serviceType().toUpperCase());
        if (request.notes() != null) shipment.setNotes(request.notes());

        etaService.refresh(shipment);
        Shipment saved = shipmentRepository.save(shipment);
        routeService.planFor(saved);
        broadcast(saved);
        return saved;
    }

    public Shipment updateStatus(String id, ShipmentDtos.StatusUpdateRequest request, UserPrincipal principal) {
        Shipment shipment = getById(id);
        if (!principal.isStaff()) {
            throw ApiException.forbidden("Only logistics staff can move a shipment through the delivery stages");
        }

        ShipmentStatus current = shipment.getStatus();
        ShipmentStatus next = request.status();
        if (current == next) {
            throw ApiException.badRequest("Shipment is already " + readable(next));
        }
        if (!current.allowedNext().contains(next)) {
            throw ApiException.badRequest("A shipment that is " + readable(current)
                    + " cannot move to " + readable(next));
        }

        shipment.setStatus(next);
        Instant now = Instant.now();

        GeoPoint location = shipment.getCurrentLocation();
        if (request.latitude() != null && request.longitude() != null) {
            location = GeoPoint.of(request.latitude(), request.longitude(), request.locationLabel());
            shipment.setCurrentLocation(location);
        }

        switch (next) {
            case PICKED_UP -> shipment.setPickedUpAt(now);
            case DELIVERED -> {
                shipment.setActualDeliveryAt(now);
                shipment.setEstimatedDeliveryAt(now);
                if (shipment.getReceiver() != null && shipment.getReceiver().getLatitude() != null) {
                    shipment.setCurrentLocation(GeoPoint.of(shipment.getReceiver().getLatitude(),
                            shipment.getReceiver().getLongitude(), shipment.getReceiver().getCity()));
                }
                routeService.complete(shipment.getId());
            }
            default -> { }
        }

        if (!next.isTerminal()) {
            etaService.refresh(shipment);
        }
        shipment.setUpdatedAt(now);
        Shipment saved = shipmentRepository.save(shipment);

        String description = request.description() != null && !request.description().isBlank()
                ? request.description()
                : defaultDescription(next, saved);
        recordEvent(saved, next, description, location, principal.getUsername());

        notifyStatus(saved, next);
        broadcast(saved);
        return saved;
    }

    public Shipment updateLocation(String id, ShipmentDtos.LocationUpdateRequest request, UserPrincipal principal) {
        Shipment shipment = getById(id);
        if (!principal.isStaff()) {
            throw ApiException.forbidden("Only logistics staff can post live positions");
        }

        GeoPoint point = GeoPoint.of(request.latitude(), request.longitude(), request.label());
        shipment.setCurrentLocation(point);

        boolean wasDelayed = shipment.isDelayed();
        etaService.refresh(shipment);
        Shipment saved = shipmentRepository.save(shipment);

        routeService.recordProgress(saved.getId(), point);

        if (!wasDelayed && saved.isDelayed()) {
            notificationService.send(saved.getOwnerId(), NotificationType.DELAY_WARNING,
                    "Shipment " + saved.getTrackingNumber() + " is running late",
                    "The new estimate is about " + saved.getDelayMinutes() + " minutes past the original promise.",
                    saved);
        }

        broadcast(saved);
        messagingTemplate.convertAndSend("/topic/fleet", ShipmentDtos.ShipmentResponse.from(saved));
        return saved;
    }

    public Shipment assignDriver(String id, String driverId, UserPrincipal principal) {
        if (!principal.isStaff()) {
            throw ApiException.forbidden("Only logistics staff can assign drivers");
        }
        Shipment shipment = getById(id);
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> ApiException.notFound("Driver not found"));
        if (driver.getRole() != Role.LOGISTICS_OPERATOR) {
            throw ApiException.badRequest("Only logistics operators can be assigned to a shipment");
        }

        shipment.setAssignedDriverId(driver.getId());
        shipment.setAssignedDriverName(driver.getFullName());
        shipment.setUpdatedAt(Instant.now());
        Shipment saved = shipmentRepository.save(shipment);

        routeService.replan(saved, new com.shiptrack.dto.RouteDtos.PlanRouteRequest(null, driver.getId(), null));

        recordEvent(saved, saved.getStatus(), driver.getFullName() + " assigned to this shipment",
                saved.getCurrentLocation(), principal.getUsername());
        broadcast(saved);
        return saved;
    }

    public Shipment cancel(String id, String reason, UserPrincipal principal) {
        Shipment shipment = getById(id);
        assertCanEdit(shipment, principal);

        if (shipment.getStatus().isTerminal()) {
            throw ApiException.badRequest("A shipment that is " + readable(shipment.getStatus()) + " cannot be cancelled");
        }
        if (shipment.getStatus() == ShipmentStatus.OUT_FOR_DELIVERY && !principal.isStaff()) {
            throw ApiException.badRequest("Contact support to stop a shipment that is already out for delivery");
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipment.setCancellationReason(reason);
        shipment.setUpdatedAt(Instant.now());
        Shipment saved = shipmentRepository.save(shipment);

        recordEvent(saved, ShipmentStatus.CANCELLED,
                reason == null || reason.isBlank() ? "Shipment cancelled" : "Cancelled: " + reason,
                saved.getCurrentLocation(), principal.getUsername());

        notificationService.send(saved.getOwnerId(), NotificationType.SYSTEM,
                "Shipment " + saved.getTrackingNumber() + " cancelled",
                reason == null || reason.isBlank() ? "The shipment was cancelled." : reason, saved);

        broadcast(saved);
        return saved;
    }

    public void delete(String id) {
        Shipment shipment = getById(id);
        trackingEventRepository.deleteByShipmentId(id);
        routeService.deleteForShipment(id);
        shipmentRepository.delete(shipment);
    }

    // ----------------------------------------------------------- tracking log

    public TrackingEvent recordEvent(Shipment shipment, ShipmentStatus status,
                                     String description, GeoPoint location, String actor) {
        TrackingEvent event = TrackingEvent.builder()
                .shipmentId(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .status(status)
                .description(description)
                .location(location)
                .recordedBy(actor)
                .occurredAt(Instant.now())
                .build();
        TrackingEvent saved = trackingEventRepository.save(event);
        messagingTemplate.convertAndSend("/topic/shipments/" + shipment.getTrackingNumber() + "/events", saved);
        return saved;
    }

    private void broadcast(Shipment shipment) {
        messagingTemplate.convertAndSend("/topic/shipments/" + shipment.getTrackingNumber(),
                ShipmentDtos.ShipmentResponse.from(shipment));
    }

    private void notifyStatus(Shipment shipment, ShipmentStatus status) {
        NotificationType type = switch (status) {
            case OUT_FOR_DELIVERY -> NotificationType.OUT_FOR_DELIVERY;
            case DELIVERED -> NotificationType.DELIVERED;
            case FAILED_DELIVERY -> NotificationType.DELIVERY_FAILED;
            default -> NotificationType.STATUS_UPDATE;
        };
        notificationService.send(shipment.getOwnerId(), type,
                "Shipment " + shipment.getTrackingNumber() + " is " + readable(status),
                defaultDescription(status, shipment), shipment);
    }

    private String defaultDescription(ShipmentStatus status, Shipment shipment) {
        String city = shipment.getCurrentLocation() != null && shipment.getCurrentLocation().getLabel() != null
                ? shipment.getCurrentLocation().getLabel()
                : (shipment.getReceiver() != null ? shipment.getReceiver().getCity() : "the network");
        return switch (status) {
            case CREATED -> "Shipment booked and awaiting pickup";
            case PICKED_UP -> "Package collected from the sender";
            case IN_TRANSIT -> "Moving through " + city;
            case OUT_FOR_DELIVERY -> "With the delivery driver in " + city;
            case DELIVERED -> "Delivered to " + (shipment.getReceiver() != null
                    ? shipment.getReceiver().getContactName() : "the recipient");
            case FAILED_DELIVERY -> "Delivery attempt did not succeed, a new attempt will be scheduled";
            case CANCELLED -> "Shipment cancelled";
        };
    }

    public static String readable(ShipmentStatus status) {
        return status.name().toLowerCase().replace('_', ' ');
    }

    public Shipment save(Shipment shipment) {
        return shipmentRepository.save(shipment);
    }
}
