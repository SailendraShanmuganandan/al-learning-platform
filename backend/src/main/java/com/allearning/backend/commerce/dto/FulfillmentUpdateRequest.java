package com.allearning.backend.commerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for PATCH /api/admin/orders/{id}/fulfillment-status.
 */
public record FulfillmentUpdateRequest(
        @NotBlank
        String status,
        @Size(max = 500)
        String trackingNote
) {
    @JsonProperty("trackingNote")
    public String getTrackingNoteCamel() { return trackingNote; }
}
