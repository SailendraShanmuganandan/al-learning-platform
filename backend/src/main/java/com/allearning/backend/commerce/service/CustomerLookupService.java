package com.allearning.backend.commerce.service;

import java.util.Map;
import java.util.Set;

/**
 * Service interface that the commerce module calls to obtain basic customer
 * information for a set of user IDs, without importing user/auth JPA entities.
 * <p>
 * Modular-monolith boundary: the implementation lives in the auth module and
 * returns plain {@link CustomerSummaryDto} records. Commerce MUST NOT call
 * UserRepository or User JPA entities directly.
 */
public interface CustomerLookupService {

    /**
     * Returns a map of userId → CustomerSummaryDto for all found user IDs.
     * Missing user IDs (deleted accounts, etc.) are simply absent from the map.
     *
     * @param userIds set of user IDs to look up
     * @return map of userId → summary; never null
     */
    Map<Long, CustomerSummaryDto> getCustomerSummaries(Set<Long> userIds);

    /**
     * Looks up a single user by their email address (case-insensitive).
     * Used by admin order filters to translate email search into a userId.
     *
     * @param email lower-case email address
     * @return the user ID, or null if not found
     */
    Long findUserIdByEmail(String email);
}
