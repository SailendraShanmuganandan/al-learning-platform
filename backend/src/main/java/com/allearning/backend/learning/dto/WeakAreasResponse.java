package com.allearning.backend.learning.dto;

import java.util.List;

public record WeakAreasResponse(
        List<WeakAreaItemDto> weakAreas
) {
    public List<WeakAreaItemDto> items() { return weakAreas; }
}
