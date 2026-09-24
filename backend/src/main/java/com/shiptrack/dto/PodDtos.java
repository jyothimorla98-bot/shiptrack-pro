package com.shiptrack.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class PodDtos {

    private PodDtos() {}

    public record CreatePodRequest(
            @NotBlank String receivedByName,
            String relationship,
            String signatureImage,
            List<String> photoUrls,
            String notes,
            Double latitude,
            Double longitude
    ) {}

    public record VerifyPodRequest(boolean approved, String rejectionReason) {}
}
