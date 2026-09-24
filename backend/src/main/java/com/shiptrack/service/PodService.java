package com.shiptrack.service;

import com.shiptrack.dto.PodDtos;
import com.shiptrack.dto.ShipmentDtos;
import com.shiptrack.exception.ApiException;
import com.shiptrack.model.*;
import com.shiptrack.repository.ProofOfDeliveryRepository;
import com.shiptrack.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PodService {

    private final ProofOfDeliveryRepository podRepository;
    private final ShipmentService shipmentService;
    private final NotificationService notificationService;

    public PodService(ProofOfDeliveryRepository podRepository,
                      ShipmentService shipmentService,
                      NotificationService notificationService) {
        this.podRepository = podRepository;
        this.shipmentService = shipmentService;
        this.notificationService = notificationService;
    }

    /** Captures the delivery evidence and moves the shipment to delivered in one step. */
    public ProofOfDelivery capture(String shipmentId, PodDtos.CreatePodRequest request, UserPrincipal principal) {
        if (!principal.isStaff()) {
            throw ApiException.forbidden("Only the delivery team can record proof of delivery");
        }

        Shipment shipment = shipmentService.getById(shipmentId);
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw ApiException.badRequest("This shipment was cancelled");
        }
        if (podRepository.findByShipmentId(shipmentId).isPresent()) {
            throw ApiException.conflict("Proof of delivery has already been recorded for this shipment");
        }
        if (shipment.getPackageDetails() != null
                && shipment.getPackageDetails().isRequiresSignature()
                && (request.signatureImage() == null || request.signatureImage().isBlank())) {
            throw ApiException.badRequest("This package needs a signature before it can be closed out");
        }

        GeoPoint location = request.latitude() != null && request.longitude() != null
                ? GeoPoint.of(request.latitude(), request.longitude(),
                    shipment.getReceiver() != null ? shipment.getReceiver().getCity() : null)
                : shipment.getCurrentLocation();

        ProofOfDelivery pod = ProofOfDelivery.builder()
                .shipmentId(shipmentId)
                .trackingNumber(shipment.getTrackingNumber())
                .receivedByName(request.receivedByName())
                .relationship(request.relationship())
                .signatureImage(request.signatureImage())
                .photoUrls(request.photoUrls() == null ? new ArrayList<>() : new ArrayList<>(request.photoUrls()))
                .notes(request.notes())
                .deliveryLocation(location)
                .capturedBy(principal.getUsername())
                .deliveredAt(Instant.now())
                .build();

        ProofOfDelivery saved = podRepository.save(pod);

        shipment.setPodId(saved.getId());
        shipmentService.save(shipment);

        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            shipmentService.updateStatus(shipmentId, new ShipmentDtos.StatusUpdateRequest(
                    ShipmentStatus.DELIVERED,
                    "Delivered to " + request.receivedByName(),
                    location != null ? location.getLatitude() : null,
                    location != null ? location.getLongitude() : null,
                    location != null ? location.getLabel() : null), principal);
        }

        notificationService.send(shipment.getOwnerId(), NotificationType.POD_UPLOADED,
                "Proof of delivery ready for " + shipment.getTrackingNumber(),
                "Signed for by " + request.receivedByName() + ". Open the shipment to view the signature and photos.",
                shipment);

        return saved;
    }

    public ProofOfDelivery verify(String podId, PodDtos.VerifyPodRequest request, UserPrincipal principal) {
        if (!principal.isStaff()) {
            throw ApiException.forbidden("Only support staff can verify proof of delivery");
        }
        ProofOfDelivery pod = getById(podId);

        if (request.approved()) {
            pod.setVerified(true);
            pod.setRejectionReason(null);
        } else {
            pod.setVerified(false);
            pod.setRejectionReason(request.rejectionReason() == null || request.rejectionReason().isBlank()
                    ? "Evidence was not clear enough" : request.rejectionReason());
        }
        pod.setVerifiedBy(principal.getUsername());
        pod.setVerifiedAt(Instant.now());
        return podRepository.save(pod);
    }

    public ProofOfDelivery addPhoto(String podId, String url, UserPrincipal principal) {
        if (!principal.isStaff()) {
            throw ApiException.forbidden("Only the delivery team can attach delivery photos");
        }
        ProofOfDelivery pod = getById(podId);
        if (pod.getPhotoUrls() == null) pod.setPhotoUrls(new ArrayList<>());
        pod.getPhotoUrls().add(url);
        return podRepository.save(pod);
    }

    public ProofOfDelivery getById(String id) {
        return podRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Proof of delivery not found"));
    }

    public ProofOfDelivery getByShipment(String shipmentId, UserPrincipal principal) {
        Shipment shipment = shipmentService.getById(shipmentId);
        shipmentService.assertCanView(shipment, principal);
        return podRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> ApiException.notFound("No proof of delivery has been recorded yet"));
    }

    public Page<ProofOfDelivery> pendingVerification(Pageable pageable) {
        return podRepository.findByVerified(false, pageable);
    }

    public List<ProofOfDelivery> all() {
        return podRepository.findAll();
    }

    public long pendingCount() {
        return podRepository.countByVerified(false);
    }
}
