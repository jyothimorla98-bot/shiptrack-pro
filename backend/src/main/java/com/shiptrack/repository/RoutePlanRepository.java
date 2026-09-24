package com.shiptrack.repository;

import com.shiptrack.model.RoutePlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RoutePlanRepository extends MongoRepository<RoutePlan, String> {
    Optional<RoutePlan> findByShipmentId(String shipmentId);
    List<RoutePlan> findByDriverId(String driverId);
    List<RoutePlan> findByStatus(String status);
    void deleteByShipmentId(String shipmentId);
}
