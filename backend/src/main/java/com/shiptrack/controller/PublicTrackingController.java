package com.shiptrack.controller;

import com.shiptrack.dto.ShipmentDtos;
import com.shiptrack.model.Shipment;
import com.shiptrack.model.TrackingEvent;
import com.shiptrack.service.EtaService;
import com.shiptrack.service.ShipmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tracking for anyone holding a tracking number, with contact details left out. */
@RestController
@RequestMapping("/api/public")
public class PublicTrackingController {

    private final ShipmentService shipmentService;
    private final EtaService etaService;

    public PublicTrackingController(ShipmentService shipmentService, EtaService etaService) {
        this.shipmentService = shipmentService;
        this.etaService = etaService;
    }

    @GetMapping("/track/{trackingNumber}")
    public ShipmentDtos.PublicTrackingResponse track(@PathVariable String trackingNumber) {
        Shipment shipment = shipmentService.getByTrackingNumber(trackingNumber);
        List<TrackingEvent> timeline = shipmentService.timeline(shipment.getId());

        return new ShipmentDtos.PublicTrackingResponse(
                shipment.getTrackingNumber(),
                shipment.getStatus(),
                shipment.getSender() == null ? null : shipment.getSender().shortText(),
                shipment.getReceiver() == null ? null : shipment.getReceiver().shortText(),
                shipment.getServiceType(),
                shipment.getCurrentLocation(),
                shipment.getEstimatedDeliveryAt(),
                shipment.getActualDeliveryAt(),
                shipment.isDelayed(),
                shipment.getDelayMinutes(),
                timeline);
    }

    @GetMapping("/track/{trackingNumber}/eta")
    public ShipmentDtos.EtaResponse eta(@PathVariable String trackingNumber) {
        Shipment shipment = shipmentService.getByTrackingNumber(trackingNumber);
        return etaService.describe(shipment);
    }
}
