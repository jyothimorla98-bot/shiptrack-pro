package com.shiptrack.controller;

import com.shiptrack.dto.CommonDtos;
import com.shiptrack.dto.ShipmentDtos;
import com.shiptrack.model.Shipment;
import com.shiptrack.model.ShipmentStatus;
import com.shiptrack.model.TrackingEvent;
import com.shiptrack.security.UserPrincipal;
import com.shiptrack.service.EtaService;
import com.shiptrack.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final EtaService etaService;

    public ShipmentController(ShipmentService shipmentService, EtaService etaService) {
        this.shipmentService = shipmentService;
        this.etaService = etaService;
    }

    @PostMapping
    public ResponseEntity<ShipmentDtos.ShipmentResponse> create(
            @Valid @RequestBody ShipmentDtos.CreateShipmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Shipment shipment = shipmentService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ShipmentDtos.ShipmentResponse.from(shipment));
    }

    @GetMapping
    public CommonDtos.PageResponse<ShipmentDtos.ShipmentResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Shipment> result = shipmentService.list(principal, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return CommonDtos.PageResponse.of(result,
                result.getContent().stream().map(ShipmentDtos.ShipmentResponse::from).toList());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR','SUPPORT_AGENT')")
    public List<ShipmentDtos.ShipmentResponse> active() {
        return shipmentService.activeShipments().stream().map(ShipmentDtos.ShipmentResponse::from).toList();
    }

    @GetMapping("/delayed")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR','SUPPORT_AGENT')")
    public List<ShipmentDtos.ShipmentResponse> delayed() {
        return etaService.delayRiskList().stream().map(ShipmentDtos.ShipmentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ShipmentDtos.ShipmentResponse get(@PathVariable String id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ShipmentDtos.ShipmentResponse.from(shipmentService.getAccessible(id, principal));
    }

    @GetMapping("/{id}/timeline")
    public List<TrackingEvent> timeline(@PathVariable String id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        shipmentService.getAccessible(id, principal);
        return shipmentService.timeline(id);
    }

    @GetMapping("/{id}/eta")
    public ShipmentDtos.EtaResponse eta(@PathVariable String id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        Shipment shipment = shipmentService.getAccessible(id, principal);
        return etaService.describe(etaService.refresh(shipment));
    }

    @PutMapping("/{id}")
    public ShipmentDtos.ShipmentResponse update(@PathVariable String id,
                                                @Valid @RequestBody ShipmentDtos.UpdateShipmentRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ShipmentDtos.ShipmentResponse.from(shipmentService.update(id, request, principal));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR','SUPPORT_AGENT')")
    public ShipmentDtos.ShipmentResponse updateStatus(@PathVariable String id,
                                                      @Valid @RequestBody ShipmentDtos.StatusUpdateRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ShipmentDtos.ShipmentResponse.from(shipmentService.updateStatus(id, request, principal));
    }

    @PatchMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR')")
    public ShipmentDtos.ShipmentResponse updateLocation(@PathVariable String id,
                                                        @Valid @RequestBody ShipmentDtos.LocationUpdateRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ShipmentDtos.ShipmentResponse.from(shipmentService.updateLocation(id, request, principal));
    }

    @PatchMapping("/{id}/driver")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR')")
    public ShipmentDtos.ShipmentResponse assignDriver(@PathVariable String id,
                                                      @Valid @RequestBody ShipmentDtos.AssignDriverRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ShipmentDtos.ShipmentResponse.from(shipmentService.assignDriver(id, request.driverId(), principal));
    }

    @PostMapping("/{id}/cancel")
    public ShipmentDtos.ShipmentResponse cancel(@PathVariable String id,
                                                @RequestBody(required = false) ShipmentDtos.CancelShipmentRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        String reason = request == null ? null : request.reason();
        return ShipmentDtos.ShipmentResponse.from(shipmentService.cancel(id, reason, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CommonDtos.ApiMessage delete(@PathVariable String id) {
        shipmentService.delete(id);
        return new CommonDtos.ApiMessage("Shipment removed");
    }
}
