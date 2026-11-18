package com.allearning.backend.communication.dto;

public record ContactSuccessResponse(
        boolean success,
        String message
) {}
