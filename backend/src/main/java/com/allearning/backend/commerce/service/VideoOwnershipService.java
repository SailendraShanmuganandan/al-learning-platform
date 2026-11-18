package com.allearning.backend.commerce.service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service contract for determining whether a user owns a video course
 * and listing owned video courses with purchase dates.
 */
public interface VideoOwnershipService {

    /**
     * Checks if the specified user already owns access to the video course product.
     *
     * @param userId    the student user ID
     * @param productId the video product ID
     * @return true if the user owns the video course, false otherwise
     */
    boolean userOwnsVideo(Long userId, Long productId);

    /**
     * Item representing an owned video product and its purchase timestamp.
     */
    record OwnedVideoPurchase(
            Long productId,
            OffsetDateTime purchasedAt
    ) {}

    /**
     * Returns the de-duplicated list of video product IDs owned by the given user
     * along with their purchase dates, sorted by purchase date descending.
     * Only orders in PAID status are considered owned.
     *
     * @param userId the student user ID
     * @return de-duplicated list of owned video purchases
     */
    List<OwnedVideoPurchase> getOwnedVideos(Long userId);
}
