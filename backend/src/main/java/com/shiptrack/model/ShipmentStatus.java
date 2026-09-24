package com.shiptrack.model;

import java.util.List;

public enum ShipmentStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED_DELIVERY,
    CANCELLED;

    /** Statuses a shipment may legally move to from this one. */
    public List<ShipmentStatus> allowedNext() {
        return switch (this) {
            case CREATED -> List.of(PICKED_UP, CANCELLED);
            case PICKED_UP -> List.of(IN_TRANSIT, FAILED_DELIVERY, CANCELLED);
            case IN_TRANSIT -> List.of(OUT_FOR_DELIVERY, FAILED_DELIVERY, CANCELLED);
            case OUT_FOR_DELIVERY -> List.of(DELIVERED, FAILED_DELIVERY);
            case FAILED_DELIVERY -> List.of(OUT_FOR_DELIVERY, IN_TRANSIT, CANCELLED);
            case DELIVERED, CANCELLED -> List.of();
        };
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
