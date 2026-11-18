package com.allearning.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentOptionDto(
        Long optionId,
        String text,
        int order
) {}

