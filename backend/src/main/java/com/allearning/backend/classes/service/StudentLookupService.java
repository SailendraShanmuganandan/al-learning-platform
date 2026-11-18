package com.allearning.backend.classes.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service interface that the classes module calls to obtain basic student
 * details for a set of user IDs or to search user IDs by text, without importing
 * user/auth JPA entities directly (modular monolith rule).
 */
public interface StudentLookupService {

    /**
     * Returns a map of userId -> StudentSummaryDto for the given user IDs.
     */
    Map<Long, StudentSummaryDto> getStudentSummaries(Set<Long> userIds);

    /**
     * Finds user IDs whose name or email matches the search query (case-insensitive).
     */
    List<Long> findUserIdsByQuery(String query);

    /**
     * Looks up user ID by email address (case-insensitive).
     */
    Long findUserIdByEmail(String email);
}
