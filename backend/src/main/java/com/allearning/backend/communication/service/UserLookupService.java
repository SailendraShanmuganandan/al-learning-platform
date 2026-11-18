package com.allearning.backend.communication.service;

import java.util.Map;
import java.util.Set;

/**
 * Service interface that the communication module calls to obtain basic user
 * display names (full names) for a set of user IDs, without importing user/auth JPA entities.
 * <p>
 * Modular-monolith boundary: the implementation lives in the auth module.
 */
public interface UserLookupService {

    /**
     * Returns a map of userId -> display name (fullName) for the given user IDs.
     */
    Map<Long, String> getUserDisplayNames(Set<Long> userIds);

    /**
     * Looks up user ID by email address (case-insensitive).
     */
    Long findUserIdByEmail(String email);
}
