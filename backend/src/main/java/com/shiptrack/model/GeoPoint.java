package com.shiptrack.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoPoint {
    private double latitude;
    private double longitude;
    private String label;

    public static GeoPoint of(double lat, double lng, String label) {
        return GeoPoint.builder().latitude(lat).longitude(lng).label(label).build();
    }
}
