package com.shiptrack.repository;

import com.shiptrack.model.TrackingEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface TrackingEventRepository extends MongoRepository<TrackingEvent, String> {
    List<TrackingEvent> findByShipmentIdOrderByOccurredAtAsc(String shipmentId);
    List<TrackingEvent> findByShipmentIdOrderByOccurredAtDesc(String shipmentId);
    List<TrackingEvent> findByTrackingNumberOrderByOccurredAtAsc(String trackingNumber);
    List<TrackingEvent> findByOccurredAtBetween(Instant from, Instant to);
    void deleteByShipmentId(String shipmentId);
}
