package com.allearning.backend.academic.service;

import com.allearning.backend.academic.dto.*;
import com.allearning.backend.academic.entity.Chapter;
import com.allearning.backend.academic.entity.Subject;
import com.allearning.backend.academic.repository.ChapterRepository;
import com.allearning.backend.academic.repository.SubjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Application service for chapter management.
 * <p>
 * Rules:
 * <ul>
 *   <li>Public consumers only see ACTIVE chapters of ACTIVE subjects.</li>
 *   <li>Admins see all chapters for any subject regardless of subject/chapter status.</li>
 *   <li>chapter_order is unique within a subject, regardless of chapter status.</li>
 *   <li>Omitting chapter_order on create auto-appends after the highest existing order.</li>
 *   <li>An occupied order returns 409 — no silent shifting.</li>
 *   <li>subject_id cannot be changed after creation.</li>
 *   <li>No hard delete — deactivation is the removal mechanism.</li>
 * </ul>
 */
@Service
@Transactional
public class ChapterService implements ChapterValidationService {

    private static final Logger log = LoggerFactory.getLogger(ChapterService.class);

    private final ChapterRepository chapterRepository;
    private final SubjectRepository subjectRepository;

    public ChapterService(ChapterRepository chapterRepository,
                          SubjectRepository subjectRepository) {
        this.chapterRepository = chapterRepository;
        this.subjectRepository = subjectRepository;
    }

    // -------------------------------------------------------------------------
    // Public operations (students / anonymous)
    // -------------------------------------------------------------------------

    /**
     * Returns ACTIVE chapters for an ACTIVE subject, sorted by chapter_order.
     * Returns 404 if the subject is missing or INACTIVE.
     */
    @Transactional(readOnly = true)
    public List<ChapterResponse> listActiveForSubject(Long subjectId) {
        Subject subject = requireActiveSubject(subjectId);
        return chapterRepository
                .findBySubject_SubjectIdAndStatusOrderByChapterOrderAsc(subject.getSubjectId(), "ACTIVE")
                .stream()
                .map(ChapterResponse::from)
                .toList();
    }

    /**
     * Returns a single ACTIVE chapter.
     * Returns 404 if the chapter is INACTIVE, missing, or belongs to an INACTIVE subject.
     */
    @Transactional(readOnly = true)
    public ChapterResponse getActiveById(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        if (!"ACTIVE".equals(chapter.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found");
        }
        // Also 404 if the owning subject is inactive
        if (!"ACTIVE".equals(chapter.getSubject().getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found");
        }
        return ChapterResponse.from(chapter);
    }

    // -------------------------------------------------------------------------
    // Admin operations
    // -------------------------------------------------------------------------

    /**
     * Returns all chapters (including INACTIVE) for a subject, sorted by chapter_order.
     * Subject can be INACTIVE — admin view shows everything.
     */
    @Transactional(readOnly = true)
    public List<ChapterResponse> listAllForSubject(Long subjectId) {
        requireSubjectExists(subjectId);
        return chapterRepository
                .findBySubject_SubjectIdOrderByChapterOrderAsc(subjectId)
                .stream()
                .map(ChapterResponse::from)
                .toList();
    }

    /**
     * Creates a new ACTIVE chapter.
     * If {@code chapterOrder} is omitted, appends after the current highest order.
     * Returns 409 if the given order is already taken within the subject.
     * Returns 404 if the subject does not exist.
     */
    public ChapterResponse create(ChapterCreateRequest request) {
        Subject subject = requireSubjectExists(request.subjectId());

        int order;
        if (request.chapterOrder() == null) {
            // Auto-append: next after current maximum
            order = chapterRepository.findMaxOrderBySubjectId(subject.getSubjectId())
                    .map(max -> max + 1)
                    .orElse(1);
        } else {
            order = request.chapterOrder();
            if (chapterRepository.existsBySubjectIdAndOrder(subject.getSubjectId(), order)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Chapter order " + order + " is already taken in this subject. " +
                        "Choose a different order or omit it to auto-append.");
            }
        }

        Chapter chapter = new Chapter(subject, request.name().trim(), order, request.description());
        Chapter saved = chapterRepository.save(chapter);
        log.info("Chapter created: id={} subjectId={} order={} name='{}'",
                saved.getChapterId(), subject.getSubjectId(), order, saved.getName());
        return ChapterResponse.from(saved);
    }

    /**
     * Updates name, description, and/or chapter_order of an existing chapter.
     * Returns 404 if the chapter does not exist.
     * Returns 409 if the new order is already taken by another chapter in the same subject.
     * subject_id cannot be changed — it is ignored if supplied at the DTO level.
     */
    public ChapterResponse update(Long chapterId, ChapterUpdateRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));

        chapter.setName(request.name().trim());
        chapter.setDescription(request.description());

        if (request.chapterOrder() != null) {
            int newOrder = request.chapterOrder();
            if (Integer.compare(newOrder, chapter.getChapterOrder()) != 0) {
                // Only check conflict if the order actually changes
                if (chapterRepository.existsBySubjectIdAndOrderExcluding(
                        chapter.getSubject().getSubjectId(), newOrder, chapterId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Chapter order " + newOrder + " is already taken in this subject. " +
                            "Choose a different order number.");
                }
                chapter.setChapterOrder(newOrder);
            }
        }

        return ChapterResponse.from(chapterRepository.save(chapter));
    }

    /**
     * Sets the status of a chapter to ACTIVE or INACTIVE.
     * Returns 404 if the chapter does not exist.
     */
    public ChapterResponse setStatus(Long chapterId, String status) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        chapter.setStatus(status);
        return ChapterResponse.from(chapterRepository.save(chapter));
    }

    /**
     * Returns all chapters across all subjects for admin view, ordered by subject and order.
     */
    @Transactional(readOnly = true)
    public List<ChapterResponse> listAll() {
        return chapterRepository.findAll(
                org.springframework.data.domain.Sort.by("subject.subjectId").and(
                        org.springframework.data.domain.Sort.by("chapterOrder")
                )
        ).stream().map(ChapterResponse::from).toList();
    }

    // -------------------------------------------------------------------------
    // ChapterValidationService implementation (modular monolith contract)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean doAllChaptersExist(java.util.Collection<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return true;
        }
        long uniqueCount = chapterIds.stream().filter(java.util.Objects::nonNull).distinct().count();
        long foundCount = chapterRepository.countByChapterIdIn(chapterIds);
        return foundCount == uniqueCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findMissingChapterIds(java.util.Collection<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return List.of();
        }
        List<Long> existingIds = chapterRepository.findExistingIds(chapterIds);
        return chapterIds.stream()
                .filter(id -> id != null && !existingIds.contains(id))
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterValidationService.PublicChapterSummary> getPublicChapterSummaries(
            java.util.Collection<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return List.of();
        }
        return chapterRepository.findPublicByChapterIdIn(chapterIds)
                .stream()
                .map(c -> new ChapterValidationService.PublicChapterSummary(
                        c.getChapterId(),
                        c.getName(),
                        c.getChapterOrder(),
                        c.getSubject().getSubjectId(),
                        c.getSubject().getName()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getPublicChapterIdsForSubject(Long subjectId) {
        return chapterRepository.findPublicBySubjectId(subjectId)
                .stream()
                .map(Chapter::getChapterId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPubliclyVisible(Long chapterId) {
        return chapterRepository.findPublicById(chapterId).isPresent();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Returns the subject if it is ACTIVE; throws 404 otherwise. */
    private Subject requireActiveSubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        if (!"ACTIVE".equals(subject.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found");
        }
        return subject;
    }

    /** Returns the subject regardless of status (admin); throws 404 if missing. */
    private Subject requireSubjectExists(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
    }
}
