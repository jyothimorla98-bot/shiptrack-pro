package com.shiptrack.repository;

import com.shiptrack.model.Shipment;
import com.shiptrack.model.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends MongoRepository<Shipment, String> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    boolean existsByTrackingNumber(String trackingNumber);

    Page<Shipment> findByOwnerId(String ownerId, Pageable pageable);
    Page<Shipment> findByOwnerIdAndStatus(String ownerId, ShipmentStatus status, Pageable pageable);
    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);

    List<Shipment> findByOwnerId(String ownerId);
    List<Shipment> findByStatus(ShipmentStatus status);
    List<Shipment> findByStatusIn(List<ShipmentStatus> statuses);
    List<Shipment> findByAssignedDriverId(String driverId);
    List<Shipment> findByCreatedAtBetween(Instant from, Instant to);
    List<Shipment> findByDelayedTrue();

    long countByStatus(ShipmentStatus status);
    long countByDelayedTrue();
    long countByOwnerId(String ownerId);
    long countByOwnerIdAndStatus(String ownerId, ShipmentStatus status);
    long countByCreatedAtBetween(Instant from, Instant to);
}
