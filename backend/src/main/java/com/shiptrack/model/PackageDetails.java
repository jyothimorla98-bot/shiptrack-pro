package com.shiptrack.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageDetails {
    private String description;
    private Integer quantity;
    private Double weightKg;
    private Double lengthCm;
    private Double widthCm;
    private Double heightCm;
    private Double declaredValue;
    private boolean fragile;
    private boolean requiresSignature;

    public double volumetricWeight() {
        if (lengthCm == null || widthCm == null || heightCm == null) return 0d;
        return (lengthCm * widthCm * heightCm) / 5000d;
    }
}
