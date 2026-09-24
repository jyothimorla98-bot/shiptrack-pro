package com.shiptrack.repository;

import com.shiptrack.model.ProofOfDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProofOfDeliveryRepository extends MongoRepository<ProofOfDelivery, String> {
    Optional<ProofOfDelivery> findByShipmentId(String shipmentId);
    Optional<ProofOfDelivery> findByTrackingNumber(String trackingNumber);
    Page<ProofOfDelivery> findByVerified(boolean verified, Pageable pageable);
    List<ProofOfDelivery> findByVerified(boolean verified);
    long countByVerified(boolean verified);
}
