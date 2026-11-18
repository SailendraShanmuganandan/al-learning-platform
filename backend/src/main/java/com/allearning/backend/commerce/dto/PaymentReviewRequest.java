package com.allearning.backend.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for payment review actions (resolve-review and refund).
 */
public record PaymentReviewRequest(
        @NotBlank(message = "A note is required.")
        @Size(max = 1000)
        String note
) {
}
