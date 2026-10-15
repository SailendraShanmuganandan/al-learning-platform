package com.allearning.backend.academic.service;

import java.util.Collection;
import java.util.List;

/**
 * Service interface exposed by the academic module to other bounded contexts
 * (e.g. catalog) to validate chapter existence without exposing academic entities
 * or repositories directly (modular monolith rule).
 */
public interface ChapterValidationService {

    /**
     * Checks whether all given chapter IDs exist.
     *
     * @param chapterIds collection of chapter IDs to validate
     * @return true if all IDs exist in the database (or if empty); false otherwise
     */
    boolean doAllChaptersExist(Collection<Long> chapterIds);

    /**
     * Finds and returns the subset of the given chapter IDs that do NOT exist in the database.
     *
     * @param chapterIds collection of chapter IDs to check
     * @return list of nonexistent chapter IDs
     */
    List<Long> findMissingChapterIds(Collection<Long> chapterIds);

    /**
     * Returns lightweight summaries for the given chapter IDs, but only for
     * chapters that are ACTIVE and whose owning subject is also ACTIVE.
     * IDs that are missing, INACTIVE, or belong to INACTIVE subjects are silently omitted.
     *
     * @param chapterIds chapter IDs to look up
     * @return list of public chapter summaries (id, name, chapterOrder, subjectId, subjectName)
     */
    List<PublicChapterSummary> getPublicChapterSummaries(Collection<Long> chapterIds);

    /**
     * Returns all chapter IDs that are publicly visible for the given subject.
     * "Publicly visible" means the chapter is ACTIVE and the subject is ACTIVE.
     * Returns an empty list if the subject is INACTIVE or does not exist.
     *
     * @param subjectId subject to query
     * @return list of publicly visible chapter IDs
     */
    List<Long> getPublicChapterIdsForSubject(Long subjectId);

    /**
     * Returns {@code true} if the given chapter is publicly visible
     * (ACTIVE and its subject is ACTIVE).
     */
    boolean isPubliclyVisible(Long chapterId);

    /**
     * Lightweight chapter summary transferred across module boundaries.
     */
    record PublicChapterSummary(
            Long chapterId,
            String name,
            Integer chapterOrder,
            Long subjectId,
            String subjectName
    ) {}
}
