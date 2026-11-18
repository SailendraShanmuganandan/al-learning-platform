package com.allearning.backend.classes.service;

/**
 * Plain DTO transferring basic student identity data across modular boundaries
 * without exposing User entities to the classes package.
 */
public record StudentSummaryDto(
        Long userId,
        String fullName,
        String email,
        String phone
) {}
