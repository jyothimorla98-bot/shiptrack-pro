package com.shiptrack.controller;

import com.shiptrack.dto.CommonDtos;
import com.shiptrack.dto.PodDtos;
import com.shiptrack.model.ProofOfDelivery;
import com.shiptrack.security.UserPrincipal;
import com.shiptrack.service.FileStorageService;
import com.shiptrack.service.PodService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pod")
public class PodController {

    private final PodService podService;
    private final FileStorageService fileStorageService;

    public PodController(PodService podService, FileStorageService fileStorageService) {
        this.podService = podService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/shipment/{shipmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR')")
    public ResponseEntity<ProofOfDelivery> capture(@PathVariable String shipmentId,
                                                   @Valid @RequestBody PodDtos.CreatePodRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(podService.capture(shipmentId, request, principal));
    }

    @GetMapping("/shipment/{shipmentId}")
    public ProofOfDelivery byShipment(@PathVariable String shipmentId,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return podService.getByShipment(shipmentId, principal);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    public CommonDtos.PageResponse<ProofOfDelivery> pending(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        Page<ProofOfDelivery> result = podService.pendingVerification(PageRequest.of(page, size));
        return CommonDtos.PageResponse.of(result, result.getContent());
    }

    @PostMapping("/{podId}/verify")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    public ProofOfDelivery verify(@PathVariable String podId,
                                  @RequestBody PodDtos.VerifyPodRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return podService.verify(podId, request, principal);
    }

    @PostMapping("/{podId}/photo")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR')")
    public ProofOfDelivery addPhoto(@PathVariable String podId,
                                    @RequestParam("file") MultipartFile file,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        String url = fileStorageService.store(file);
        return podService.addPhoto(podId, url, principal);
    }
}
