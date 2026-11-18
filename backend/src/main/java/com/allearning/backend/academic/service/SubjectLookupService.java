package com.allearning.backend.academic.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface exposed by the academic module to other bounded contexts
 * (e.g. classes) to check subject visibility and basic metadata without exposing
 * internal academic entities or repositories directly (modular monolith rule).
 */
public interface SubjectLookupService {

    Optional<SubjectBasicInfo> getSubjectInfo(Long subjectId);

    boolean isSubjectActive(Long subjectId);

    List<Long> getActiveSubjectIds();

    Map<Long, SubjectBasicInfo> getSubjectsInfo(Collection<Long> subjectIds);

    record SubjectBasicInfo(Long subjectId, String name, String status) {
        public boolean isActive() {
            return "ACTIVE".equalsIgnoreCase(status);
        }
    }
}
