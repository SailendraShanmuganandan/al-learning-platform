package com.allearning.backend.learning.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface LearningUserLookupService {
    Long findUserIdByEmail(String email);
    Map<Long, String> getUserDisplayNames(Set<Long> userIds);
    boolean isEmailVerified(Long userId);
    boolean isEmailVerifiedByEmail(String email);

    record StudentProfileInfo(
            Long userId,
            String fullName,
            String email,
            boolean isStudent
    ) {}

    Optional<StudentProfileInfo> getStudentProfile(Long userId);

    List<Long> getActiveStudentIds(int maxCount);
}
