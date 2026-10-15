package com.allearning.backend.commerce.service;

/**
 * Service contract for determining whether a user owns a video course.
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
}
