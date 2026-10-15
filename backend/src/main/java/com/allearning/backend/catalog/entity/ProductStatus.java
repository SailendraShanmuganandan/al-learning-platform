package com.allearning.backend.catalog.entity;

public enum ProductStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED;

    /**
     * FR-PROD-07: Valid status transitions:
     * DRAFT -> PUBLISHED
     * PUBLISHED -> ARCHIVED
     * DRAFT -> ARCHIVED
     * ARCHIVED -> DRAFT
     * Any other transition returns 409 Conflict.
     */
    public boolean canTransitionTo(ProductStatus next) {
        if (next == null) {
            return false;
        }
        if (this == DRAFT && (next == PUBLISHED || next == ARCHIVED)) {
            return true;
        }
        if (this == PUBLISHED && next == ARCHIVED) {
            return true;
        }
        if (this == ARCHIVED && next == DRAFT) {
            return true;
        }
        return false;
    }
}
