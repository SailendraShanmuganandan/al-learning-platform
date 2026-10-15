package com.allearning.backend.commerce.service;

import java.time.OffsetDateTime;

public interface OrderExpiryService {

    /**
     * Finds and expires pending orders whose {@code expires_at} is before the given timestamp.
     * Releases any reserved stock for physical note lines.
     *
     * @param now the reference timestamp for expiry
     * @return count of orders expired
     */
    int expireOrdersAt(OffsetDateTime now);
}
