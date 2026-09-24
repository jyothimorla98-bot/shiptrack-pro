package com.shiptrack.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @NotBlank private String contactName;
    private String contactPhone;
    private String email;
    @NotBlank private String addressLine1;
    private String addressLine2;
    @NotBlank private String city;
    private String state;
    private String postalCode;
    @NotBlank private String country;
    private Double latitude;
    private Double longitude;

    public String shortText() {
        return city + (state != null && !state.isBlank() ? ", " + state : "") + ", " + country;
    }
}
