package com.allearning.backend.academic.repository;

import com.allearning.backend.academic.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Chapter}.
 * Order-conflict detection is done in service code before persisting,
 * so the DB UNIQUE constraint is a last-resort safety net only.
 */
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    /** All ACTIVE chapters for a subject, sorted by chapter_order. */
    List<Chapter> findBySubject_SubjectIdAndStatusOrderByChapterOrderAsc(
            Long subjectId, String status);

    /** All chapters for a subject (admin view), sorted by chapter_order. */
    List<Chapter> findBySubject_SubjectIdOrderByChapterOrderAsc(Long subjectId);

    /**
     * Returns the highest chapter_order in use for the given subject,
     * or {@link Optional#empty()} if the subject has no chapters yet.
     */
    @Query("SELECT MAX(c.chapterOrder) FROM Chapter c WHERE c.subject.subjectId = :subjectId")
    Optional<Integer> findMaxOrderBySubjectId(@Param("subjectId") Long subjectId);

    /**
     * Returns {@code true} if the given order is already taken within a subject
     * (regardless of chapter status, to preserve position uniqueness).
     */
    @Query("SELECT COUNT(c) > 0 FROM Chapter c " +
           "WHERE c.subject.subjectId = :subjectId AND c.chapterOrder = :order")
    boolean existsBySubjectIdAndOrder(
            @Param("subjectId") Long subjectId, @Param("order") Integer order);

    /**
     * Same as above but excludes the chapter being updated (for PATCH operations).
     */
    @Query("SELECT COUNT(c) > 0 FROM Chapter c " +
           "WHERE c.subject.subjectId = :subjectId AND c.chapterOrder = :order " +
           "AND c.chapterId <> :excludeId")
    boolean existsBySubjectIdAndOrderExcluding(
            @Param("subjectId") Long subjectId,
            @Param("order") Integer order,
            @Param("excludeId") Long excludeId);

    /**
     * Counts how many of the given chapter IDs exist in the database.
     */
    long countByChapterIdIn(java.util.Collection<Long> chapterIds);

    /**
     * Returns the existing chapter IDs among the requested set.
     */
    @Query("SELECT c.chapterId FROM Chapter c WHERE c.chapterId IN :chapterIds")
    List<Long> findExistingIds(@Param("chapterIds") java.util.Collection<Long> chapterIds);

    /**
     * Returns chapters that are ACTIVE and whose subject is also ACTIVE,
     * filtered to the given ID set.
     */
    @Query("SELECT c FROM Chapter c JOIN FETCH c.subject s " +
           "WHERE c.chapterId IN :chapterIds AND c.status = 'ACTIVE' AND s.status = 'ACTIVE'")
    List<Chapter> findPublicByChapterIdIn(@Param("chapterIds") java.util.Collection<Long> chapterIds);

    /**
     * Returns ACTIVE chapters of an ACTIVE subject (for public chapter-products lookup).
     */
    @Query("SELECT c FROM Chapter c JOIN c.subject s " +
           "WHERE s.subjectId = :subjectId AND c.status = 'ACTIVE' AND s.status = 'ACTIVE'")
    List<Chapter> findPublicBySubjectId(@Param("subjectId") Long subjectId);

    /**
     * Returns an ACTIVE chapter whose subject is also ACTIVE, or empty.
     */
    @Query("SELECT c FROM Chapter c JOIN c.subject s " +
           "WHERE c.chapterId = :chapterId AND c.status = 'ACTIVE' AND s.status = 'ACTIVE'")
    Optional<Chapter> findPublicById(@Param("chapterId") Long chapterId);
}
