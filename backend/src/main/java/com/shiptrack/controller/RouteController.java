package com.shiptrack.controller;

import com.shiptrack.dto.RouteDtos;
import com.shiptrack.model.GeoPoint;
import com.shiptrack.model.RoutePlan;
import com.shiptrack.model.Shipment;
import com.shiptrack.security.UserPrincipal;
import com.shiptrack.service.RouteService;
import com.shiptrack.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;
    private final ShipmentService shipmentService;

    public RouteController(RouteService routeService, ShipmentService shipmentService) {
        this.routeService = routeService;
        this.shipmentService = shipmentService;
    }

    @GetMapping("/shipment/{shipmentId}")
    public RoutePlan forShipment(@PathVariable String shipmentId,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        shipmentService.getAccessible(shipmentId, principal);
        return routeService.getByShipment(shipmentId);
    }

    @PostMapping("/shipment/{shipmentId}/plan")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR')")
    public RoutePlan plan(@PathVariable String shipmentId,
                          @RequestBody RouteDtos.PlanRouteRequest request) {
        Shipment shipment = shipmentService.getById(shipmentId);
        return routeService.replan(shipment, request);
    }

    @PostMapping("/shipment/{shipmentId}/optimize")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR')")
    public RoutePlan optimize(@PathVariable String shipmentId) {
        return routeService.optimize(shipmentId);
    }

    @PostMapping("/shipment/{shipmentId}/progress")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR')")
    public RoutePlan progress(@PathVariable String shipmentId,
                              @Valid @RequestBody RouteDtos.RouteProgressRequest request) {
        return routeService.recordProgress(shipmentId,
                GeoPoint.of(request.latitude(), request.longitude(), request.label()));
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR','SUPPORT_AGENT')")
    public List<RoutePlan> forDriver(@PathVariable String driverId) {
        return routeService.forDriver(driverId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR','SUPPORT_AGENT')")
    public List<RoutePlan> all() {
        return routeService.all();
    }
}
