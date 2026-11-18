package com.allearning.backend.commerce.dto;

public record CommercePendingActionsDto(
        long ordersAwaitingFulfillment,
        long paymentsNeedingReview
) {
}
