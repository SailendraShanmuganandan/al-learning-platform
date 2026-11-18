package com.allearning.backend.commerce.service;

/**
 * Plain DTO carrying minimal customer details for use within the commerce domain.
 * <p>
 * Modular-monolith rule: Commerce must NOT reference user/auth JPA entities directly.
 * This record is the only data shape that crosses the module boundary.
 */
public record CustomerSummaryDto(
        Long userId,
        String fullName,
        String email,
        String phone
) {
}
