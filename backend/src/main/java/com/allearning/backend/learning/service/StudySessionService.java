package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.catalog.dto.PublicProductSummaryResponse;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.service.VideoOwnershipService;
import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.entity.*;
import com.allearning.backend.learning.event.GradedAnswersEvent;
import com.allearning.backend.learning.event.GradedSourceType;
import com.allearning.backend.learning.exception.LearningConflictException;
import com.allearning.backend.learning.exception.NotEnoughQuestionsException;
import com.allearning.backend.learning.repository.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudySessionService implements StudySessionSubmissionGuard {

    private final StudySessionRepository studySessionRepository;
    private final StudySessionItemRepository studySessionItemRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final ChapterTopicRepository chapterTopicRepository;
    private final MistakeRecordRepository mistakeRecordRepository;
    private final QuizAttemptService quizAttemptService;
    private final AnalyticsService analyticsService;
    private final QuestionEligibility questionEligibility;
    private final QuestionSamplingService questionSamplingService;
    private final ChapterValidationService chapterValidationService;
    private final CatalogService catalogService;
    private final ObjectProvider<VideoOwnershipService> videoOwnershipServiceProvider;
    private final LearningProperties learningProperties;

    public StudySessionService(
            StudySessionRepository studySessionRepository,
            StudySessionItemRepository studySessionItemRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuestionRepository questionRepository,
            QuizRepository quizRepository,
            ChapterTopicRepository chapterTopicRepository,
            MistakeRecordRepository mistakeRecordRepository,
            QuizAttemptService quizAttemptService,
            AnalyticsService analyticsService,
            QuestionEligibility questionEligibility,
            QuestionSamplingService questionSamplingService,
            ChapterValidationService chapterValidationService,
            CatalogService catalogService,
            ObjectProvider<VideoOwnershipService> videoOwnershipServiceProvider,
            LearningProperties learningProperties
    ) {
        this.studySessionRepository = studySessionRepository;
        this.studySessionItemRepository = studySessionItemRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
        this.chapterTopicRepository = chapterTopicRepository;
        this.mistakeRecordRepository = mistakeRecordRepository;
        this.quizAttemptService = quizAttemptService;
        this.analyticsService = analyticsService;
        this.questionEligibility = questionEligibility;
        this.questionSamplingService = questionSamplingService;
        this.chapterValidationService = chapterValidationService;
        this.catalogService = catalogService;
        this.videoOwnershipServiceProvider = videoOwnershipServiceProvider;
        this.learningProperties = learningProperties;
    }

    public OffsetDateTime graceDeadline(StudySession session) {
        return session.getStartedAt().plusMinutes(
                session.getDurationMinutes() + learningProperties.getStudySessionGraceMinutes());
    }

    public StudySessionStatus effectiveStatus(StudySession session, OffsetDateTime now) {
        if (session.getStatus() == StudySessionStatus.ACTIVE && now.isAfter(graceDeadline(session))) {
            return StudySessionStatus.ABANDONED;
        }
        return session.getStatus();
    }

    @Override
    public void assertOpen(Long studySessionId) {
        StudySession session = studySessionRepository.findById(studySessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Study session not found: " + studySessionId));

        if (session.getStatus() != StudySessionStatus.ACTIVE) {
            throw new LearningConflictException("STUDY_SESSION_ENDED", "Study session has ended");
        }

        OffsetDateTime deadline = session.getStartedAt().plusMinutes(
                session.getDurationMinutes() + learningProperties.getStudySessionGraceMinutes()
        );
        if (OffsetDateTime.now().isAfter(deadline)) {
            session.setStatus(StudySessionStatus.ABANDONED);
            studySessionRepository.save(session);
            throw new LearningConflictException("STUDY_SESSION_ENDED", "Study session has ended");
        }
    }

    public StudySessionResponse startOrCreateSession(Long userId, StudySessionCreateRequest request) {
        int duration = request.durationMinutes() != null ? request.durationMinutes() : 10;
        if (duration != 10 && duration != 20 && duration != 30 && duration != 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be 10, 20, 30, or 60 minutes");
        }

        // Check if student already has an ACTIVE session
        Optional<StudySession> activeOpt = studySessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, StudySessionStatus.ACTIVE);
        if (activeOpt.isPresent()) {
            StudySession active = activeOpt.get();
            OffsetDateTime graceEnd = active.getStartedAt().plusMinutes(
                    active.getDurationMinutes() + learningProperties.getStudySessionGraceMinutes()
            );
            if (OffsetDateTime.now().isAfter(graceEnd)) {
                active.setStatus(StudySessionStatus.ABANDONED);
                studySessionRepository.save(active);
            } else {
                return toResponse(active, true);
            }
        }

        // Target question count
        int targetQuestionCount = learningProperties.getStudySessionQuestionCounts().getOrDefault(duration, 5);

        // Scope chapters
        List<Long> scopeChapterIds;
        if (request.chapterId() != null) {
            scopeChapterIds = List.of(request.chapterId());
        } else if (request.subjectId() != null) {
            scopeChapterIds = chapterValidationService.getActiveChapterIdsBySubject(request.subjectId());
        } else {
            scopeChapterIds = chapterValidationService.getActiveChapterIds();
        }

        // Filter to participating chapters
        List<Long> participatingScope = scopeChapterIds.stream()
                .filter(questionEligibility::isChapterParticipating)
                .toList();

        // Load performances to detect WEAK and DEVELOPING chapters
        List<ChapterPerformanceDto> performances = analyticsService.getChapterPerformances(userId, request.subjectId());
        Map<Long, PerformanceLevel> chapterLevels = new HashMap<>();
        for (var p : performances) {
            if (p.level() != null) {
                chapterLevels.put(p.chapterId(), p.level());
            }
        }

        List<Long> weakChaptersInScope = participatingScope.stream()
                .filter(cid -> chapterLevels.get(cid) == PerformanceLevel.WEAK)
                .toList();

        List<Long> devChaptersInScope = participatingScope.stream()
                .filter(cid -> chapterLevels.get(cid) == PerformanceLevel.DEVELOPING)
                .toList();

        List<Long> otherChaptersInScope = participatingScope.stream()
                .filter(cid -> !weakChaptersInScope.contains(cid) && !devChaptersInScope.contains(cid))
                .toList();

        // Check if there is data
        List<MistakeRecord> userActiveMistakes = mistakeRecordRepository.findByUserIdAndStatusOrderByLastAnsweredAtDesc(userId, MistakeStatus.ACTIVE);
        boolean hasData = !weakChaptersInScope.isEmpty() || !userActiveMistakes.isEmpty();

        List<Long> chosenQuestionIds = new ArrayList<>();
        Set<Long> mistakeQuestionIds = new HashSet<>();

        if (hasData) {
            // About 40% mistake questions
            int targetMistakes = (int) Math.round(targetQuestionCount * 0.40);
            if (targetMistakes > 0 && !userActiveMistakes.isEmpty()) {
                // Filter mistakes to scope and eligible questions
                List<MistakeRecord> scopeMistakes = new ArrayList<>();
                for (MistakeRecord m : userActiveMistakes) {
                    Question q = questionRepository.findById(m.getQuestionId()).orElse(null);
                    if (q != null && questionEligibility.isQuestionEligible(q)) {
                        if (request.language() == null || request.language().equalsIgnoreCase(q.getLanguage())) {
                            Long qChapId = q.getQuiz() != null ? q.getQuiz().getChapterId() : null;
                            if (qChapId == null) {
                                // Find from quiz repository
                                qChapId = quizRepository.findById(q.getQuizId()).map(Quiz::getChapterId).orElse(null);
                            }
                            if (qChapId != null && participatingScope.contains(qChapId)) {
                                scopeMistakes.add(m);
                            }
                        }
                    }
                }

                // Prioritise WEAK chapters first, then longest-unanswered (smallest lastAnsweredAt)
                scopeMistakes.sort((a, b) -> {
                    Question qa = questionRepository.findById(a.getQuestionId()).orElse(null);
                    Question qb = questionRepository.findById(b.getQuestionId()).orElse(null);
                    Long cA = qa != null && qa.getQuiz() != null ? qa.getQuiz().getChapterId() : 0L;
                    Long cB = qb != null && qb.getQuiz() != null ? qb.getQuiz().getChapterId() : 0L;

                    boolean aWeak = weakChaptersInScope.contains(cA);
                    boolean bWeak = weakChaptersInScope.contains(cB);
                    if (aWeak != bWeak) return aWeak ? -1 : 1;

                    // Longest-unanswered: nulls first, then older dates first
                    if (a.getLastAnsweredAt() == null && b.getLastAnsweredAt() == null) return Long.compare(a.getQuestionId(), b.getQuestionId());
                    if (a.getLastAnsweredAt() == null) return -1;
                    if (b.getLastAnsweredAt() == null) return 1;
                    return a.getLastAnsweredAt().compareTo(b.getLastAnsweredAt());
                });

                for (MistakeRecord m : scopeMistakes) {
                    if (chosenQuestionIds.size() >= targetMistakes) break;
                    if (!chosenQuestionIds.contains(m.getQuestionId())) {
                        chosenQuestionIds.add(m.getQuestionId());
                        mistakeQuestionIds.add(m.getQuestionId());
                    }
                }
            }

            // Fill remainder with PRACTICE questions: WEAK -> DEVELOPING -> other participating
            int neededPractice = targetQuestionCount - chosenQuestionIds.size();
            if (neededPractice > 0) {
                neededPractice = drawPracticeQuestions(neededPractice, weakChaptersInScope, userId, request.language(), chosenQuestionIds);
            }
            if (neededPractice > 0) {
                neededPractice = drawPracticeQuestions(neededPractice, devChaptersInScope, userId, request.language(), chosenQuestionIds);
            }
            if (neededPractice > 0) {
                neededPractice = drawPracticeQuestions(neededPractice, otherChaptersInScope, userId, request.language(), chosenQuestionIds);
            }
        } else {
            // No data: all PRACTICE spread across participating chapters in scope
            drawPracticeQuestions(targetQuestionCount, participatingScope, userId, request.language(), chosenQuestionIds);
        }

        if (chosenQuestionIds.isEmpty()) {
            throw new NotEnoughQuestionsException(targetQuestionCount, 0, request.chapterId(), "No eligible questions available for study session");
        }

        // Fetch question entities
        List<Question> questions = questionRepository.findByQuestionIdIn(chosenQuestionIds);
        Map<Long, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getQuestionId, q -> q));
        List<Question> orderedQuestions = new ArrayList<>();
        for (Long qId : chosenQuestionIds) {
            Question q = qMap.get(qId);
            if (q != null) orderedQuestions.add(q);
        }

        // Create StudySession entity
        StudySession session = new StudySession();
        session.setUserId(userId);
        session.setDurationMinutes(duration);
        session.setStatus(StudySessionStatus.ACTIVE);
        session.setStartedAt(OffsetDateTime.now());
        StudySession savedSession = studySessionRepository.save(session);

        // Build items list
        List<StudySessionItem> items = new ArrayList<>();
        int itemOrder = 1;

        // Resource review item at start if first WEAK chapter in scope has a published resource
        if (!weakChaptersInScope.isEmpty()) {
            Long firstWeakChapId = weakChaptersInScope.get(0);
            try {
                List<PublicProductSummaryResponse> prods = catalogService.getPublicProductsForChapter(firstWeakChapId);
                if (prods != null && !prods.isEmpty()) {
                    PublicProductSummaryResponse preferred = pickPreferredProduct(prods, userId);
                    if (preferred != null) {
                        StudySessionItem resItem = new StudySessionItem();
                        resItem.setSessionId(savedSession.getSessionId());
                        resItem.setItemType(StudySessionItemType.RESOURCE_REVIEW);
                        resItem.setReferenceId(preferred.id());
                        resItem.setItemOrder(itemOrder++);
                        items.add(resItem);
                    }
                }
            } catch (Exception ignored) {}
        }

        for (Question q : orderedQuestions) {
            StudySessionItem it = new StudySessionItem();
            it.setSessionId(savedSession.getSessionId());
            it.setItemType(mistakeQuestionIds.contains(q.getQuestionId()) ? StudySessionItemType.MISTAKE_REVIEW : StudySessionItemType.PRACTICE);
            it.setReferenceId(q.getQuestionId());
            it.setItemOrder(itemOrder++);
            items.add(it);
        }

        studySessionItemRepository.saveAll(items);

        // Create the STUDY_SESSION attempt
        quizAttemptService.createAttempt(
                userId,
                QuizAttemptType.STUDY_SESSION,
                null,
                savedSession.getSessionId(),
                orderedQuestions
        );

        return toResponse(savedSession, false);
    }

    private int drawPracticeQuestions(
            int needed,
            List<Long> chapters,
            Long userId,
            String language,
            List<Long> chosenQuestionIds
    ) {
        if (needed <= 0 || chapters.isEmpty()) return needed;

        // Round robin across chapters in the tier
        boolean foundAny = true;
        while (needed > 0 && foundAny) {
            foundAny = false;
            for (Long cId : chapters) {
                if (needed <= 0) break;
                List<Question> sampled = questionSamplingService.sampleForChapter(userId, cId, language, 1);
                for (Question q : sampled) {
                    if (!chosenQuestionIds.contains(q.getQuestionId())) {
                        chosenQuestionIds.add(q.getQuestionId());
                        needed--;
                        foundAny = true;
                        break;
                    }
                }
            }
        }
        return needed;
    }

    private PublicProductSummaryResponse pickPreferredProduct(List<PublicProductSummaryResponse> prods, Long userId) {
        VideoOwnershipService ownershipService = videoOwnershipServiceProvider.getIfAvailable();
        for (var p : prods) {
            if (p.type() != null && "VIDEO".equalsIgnoreCase(p.type().name())) {
                if (ownershipService != null && ownershipService.userOwnsVideo(userId, p.id())) {
                    return p;
                }
            }
        }
        for (var p : prods) {
            if (p.type() != null && "PHYSICAL_NOTE".equalsIgnoreCase(p.type().name())) {
                return p;
            }
        }
        return prods.get(0);
    }

    public StudySessionResponse getSession(Long userId, Long sessionId) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Study session not found: " + sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Study session not found: " + sessionId);
        }

        // Effective status check
        if (session.getStatus() == StudySessionStatus.ACTIVE) {
            OffsetDateTime deadline = session.getStartedAt().plusMinutes(
                    session.getDurationMinutes() + learningProperties.getStudySessionGraceMinutes()
            );
            if (OffsetDateTime.now().isAfter(deadline)) {
                session.setStatus(StudySessionStatus.ABANDONED);
                studySessionRepository.save(session);
            }
        }

        return toResponse(session, null);
    }

    @Transactional(readOnly = true)
    public Page<StudySessionSummaryDto> getMyStudySessions(Long userId, StudySessionStatus status, Pageable pageable) {
        Page<StudySession> page = (status != null)
                ? studySessionRepository.findByUserIdAndStatusOrderByStartedAtDesc(userId, status, pageable)
                : studySessionRepository.findByUserIdOrderByStartedAtDesc(userId, pageable);

        return page.map(s -> {
            Integer score = null;
            Integer total = null;
            var attOpt = quizAttemptRepository.findFirstByStudySessionId(s.getSessionId());
            if (attOpt.isPresent() && attOpt.get().getCompletedAt() != null) {
                score = attOpt.get().getScore();
                total = attOpt.get().getTotalMarks();
            }
            return new StudySessionSummaryDto(
                    s.getSessionId(),
                    s.getDurationMinutes(),
                    s.getStatus(),
                    s.getStartedAt(),
                    s.getCompletedAt(),
                    score,
                    total
            );
        });
    }

    public StudySessionItemResponse completeResourceItem(Long userId, Long sessionId, Long itemId) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Study session not found: " + sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Study session not found: " + sessionId);
        }

        if (session.getStatus() != StudySessionStatus.ACTIVE) {
            throw new LearningConflictException("STUDY_SESSION_ENDED", "Session is not active");
        }

        StudySessionItem item = studySessionItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + itemId));

        if (!item.getSessionId().equals(sessionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item does not belong to session: " + itemId);
        }

        if (item.getItemType() != StudySessionItemType.RESOURCE_REVIEW) {
            throw new LearningConflictException("INVALID_ITEM_TYPE", "Only RESOURCE_REVIEW items can be completed");
        }

        item.setCompletedAt(OffsetDateTime.now());
        StudySessionItem saved = studySessionItemRepository.save(item);

        return toItemResponse(saved);
    }

    public StudySessionResponse abandonSession(Long userId, Long sessionId) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Study session not found: " + sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Study session not found: " + sessionId);
        }

        if (session.getStatus() == StudySessionStatus.ACTIVE) {
            session.setStatus(StudySessionStatus.ABANDONED);
            studySessionRepository.save(session);
        }

        return toResponse(session, false);
    }

    @EventListener
    public void onGradedAnswers(GradedAnswersEvent event) {
        if (event.sourceType() == GradedSourceType.STUDY_SESSION && event.studySessionId() != null) {
            StudySession session = studySessionRepository.findById(event.studySessionId()).orElse(null);
            if (session != null && session.getStatus() == StudySessionStatus.ACTIVE) {
                session.setStatus(StudySessionStatus.COMPLETED);
                session.setCompletedAt(event.completedAt());
                studySessionRepository.save(session);

                List<StudySessionItem> items = studySessionItemRepository.findBySessionIdOrderByItemOrderAsc(session.getSessionId());
                for (StudySessionItem it : items) {
                    if (it.getItemType() != StudySessionItemType.RESOURCE_REVIEW && it.getCompletedAt() == null) {
                        it.setCompletedAt(event.completedAt());
                    }
                }
                studySessionItemRepository.saveAll(items);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.learning.study-session-cleanup-interval:600000}")
    public void cleanupExpiredSessions() {
        OffsetDateTime now = OffsetDateTime.now();
        List<StudySession> activeSessions = studySessionRepository.findByStatus(StudySessionStatus.ACTIVE);
        for (StudySession s : activeSessions) {
            OffsetDateTime deadline = s.getStartedAt().plusMinutes(
                    s.getDurationMinutes() + learningProperties.getStudySessionGraceMinutes()
            );
            if (now.isAfter(deadline)) {
                s.setStatus(StudySessionStatus.ABANDONED);
                studySessionRepository.save(s);
            }
        }
    }

    private StudySessionResponse toResponse(StudySession s, Boolean resumed) {
        OffsetDateTime endsAt = s.getStartedAt().plusMinutes(s.getDurationMinutes());
        OffsetDateTime now = OffsetDateTime.now();
        long remaining = Math.max(0, Duration.between(now, endsAt).toSeconds());
        if (s.getStatus() != StudySessionStatus.ACTIVE) {
            remaining = 0;
        }

        List<StudySessionItem> items = studySessionItemRepository.findBySessionIdOrderByItemOrderAsc(s.getSessionId());
        List<StudySessionItemResponse> itemResponses = items.stream().map(this::toItemResponse).toList();

        // Linked attempt
        var attOpt = quizAttemptRepository.findFirstByStudySessionId(s.getSessionId());
        Long attemptId = attOpt.map(QuizAttempt::getAttemptId).orElse(null);

        int questionCount = 0;
        for (var it : items) {
            if (it.getItemType() != StudySessionItemType.RESOURCE_REVIEW) {
                questionCount++;
            }
        }

        StudySessionResponse.StudySessionResultDto resultDto = null;
        if (attOpt.isPresent() && attOpt.get().getCompletedAt() != null) {
            int score = attOpt.get().getScore() != null ? attOpt.get().getScore() : 0;
            int total = attOpt.get().getTotalMarks() > 0 ? attOpt.get().getTotalMarks() : 1;
            int pct = (int) Math.round(score * 100.0 / total);
            resultDto = new StudySessionResponse.StudySessionResultDto(score, total, pct);
        }

        return new StudySessionResponse(
                s.getSessionId(),
                s.getDurationMinutes(),
                s.getStatus(),
                resumed,
                s.getStartedAt(),
                endsAt,
                remaining,
                attemptId,
                questionCount,
                itemResponses,
                resultDto
        );
    }

    private StudySessionItemResponse toItemResponse(StudySessionItem item) {
        boolean completed = item.getCompletedAt() != null;
        String chapterName = null;
        String topicName = null;
        StudySessionItemResponse.ProductResourceDto productDto = null;

        if (item.getItemType() == StudySessionItemType.RESOURCE_REVIEW && item.getReferenceId() != null) {
            try {
                var pOpt = catalogService.findById(item.getReferenceId());
                if (pOpt.isPresent()) {
                    var p = pOpt.get();
                    productDto = new StudySessionItemResponse.ProductResourceDto(
                            p.id(),
                            p.name(),
                            p.slug(),
                            p.type() != null ? p.type().name() : null
                    );
                }
            } catch (Exception ignored) {}
        } else if (item.getReferenceId() != null) {
            // Question item: NEVER include question text, options, or answers!
            // Only chapterName and topicName
            Question q = questionRepository.findById(item.getReferenceId()).orElse(null);
            if (q != null) {
                if (q.getTopicId() != null) {
                    topicName = chapterTopicRepository.findById(q.getTopicId())
                            .map(ChapterTopic::getName)
                            .orElse(null);
                }
                Long cid = q.getQuiz() != null ? q.getQuiz().getChapterId() : null;
                if (cid == null) {
                    cid = quizRepository.findById(q.getQuizId()).map(Quiz::getChapterId).orElse(null);
                }
                if (cid != null) {
                    chapterName = chapterValidationService.getChapterSummary(cid)
                            .map(ChapterValidationService.ChapterSummary::name)
                            .orElse("Chapter " + cid);
                }
            }
        }

        return new StudySessionItemResponse(
                item.getItemId(),
                item.getItemOrder(),
                item.getItemType(),
                completed,
                item.getCompletedAt(),
                chapterName,
                topicName,
                productDto
        );
    }
}
