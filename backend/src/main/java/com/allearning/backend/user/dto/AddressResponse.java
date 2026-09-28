package com.allearning.backend.user.dto;

import com.allearning.backend.user.entity.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * Outbound DTO for address responses. Never exposes internal implementation details.
 */
public record AddressResponse(
        Long addressId,
        Long userId,
        String recipientName,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String district,
        String postalCode,
        @JsonProperty("isDefault") boolean isDefault,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(
                a.getAddressId(),
                a.getUserId(),
                a.getRecipientName(),
                a.getPhone(),
                a.getAddressLine1(),
                a.getAddressLine2(),
                a.getCity(),
                a.getDistrict(),
                a.getPostalCode(),
                a.isDefault(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
