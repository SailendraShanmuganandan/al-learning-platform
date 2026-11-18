package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.catalog.dto.PublicProductSummaryResponse;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.service.VideoOwnershipService;
import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.entity.*;
import com.allearning.backend.learning.repository.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecommendationService {

    private final AnalyticsService analyticsService;
    private final MistakeService mistakeService;
    private final AssessmentService assessmentService;
    private final CatalogService catalogService;
    private final ObjectProvider<VideoOwnershipService> videoOwnershipServiceProvider;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final DiagnosticAssessmentRepository diagnosticAssessmentRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final StudySessionRepository studySessionRepository;
    private final ChapterValidationService chapterValidationService;
    private final SubjectLookupService subjectLookupService;
    private final QuestionEligibility questionEligibility;
    private final LearningUserLookupService userLookupService;
    private final LearningProperties learningProperties;
    private final RecommendationStrategy recommendationStrategy;

    public RecommendationService(
            AnalyticsService analyticsService,
            MistakeService mistakeService,
            AssessmentService assessmentService,
            CatalogService catalogService,
            ObjectProvider<VideoOwnershipService> videoOwnershipServiceProvider,
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            DiagnosticAssessmentRepository diagnosticAssessmentRepository,
            QuizAnswerRepository quizAnswerRepository,
            StudySessionRepository studySessionRepository,
            ChapterValidationService chapterValidationService,
            SubjectLookupService subjectLookupService,
            QuestionEligibility questionEligibility,
            LearningUserLookupService userLookupService,
            LearningProperties learningProperties,
            RecommendationStrategy recommendationStrategy
    ) {
        this.analyticsService = analyticsService;
        this.mistakeService = mistakeService;
        this.assessmentService = assessmentService;
        this.catalogService = catalogService;
        this.videoOwnershipServiceProvider = videoOwnershipServiceProvider;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.diagnosticAssessmentRepository = diagnosticAssessmentRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.studySessionRepository = studySessionRepository;
        this.chapterValidationService = chapterValidationService;
        this.subjectLookupService = subjectLookupService;
        this.questionEligibility = questionEligibility;
        this.userLookupService = userLookupService;
        this.learningProperties = learningProperties;
        this.recommendationStrategy = recommendationStrategy;
    }

    @Transactional(readOnly = true)
    public Recommendation getNextRecommendation(Long userId) {
        RecommendationContext ctx = buildContext(userId);
        return recommendationStrategy.recommend(ctx);
    }

    public LearningOverviewResponse getOverview(Long userId) {
        RecommendationContext ctx = buildContext(userId);
        Recommendation recommendation = recommendationStrategy.recommend(ctx);

        // Onboarding
        OnboardingStatusResponse onboarding = new OnboardingStatusResponse(
                ctx.offerInitialDiagnostic(),
                ctx.onboardingReason()
        );

        // Progress
        ProgressSummary progress = analyticsService.getProgress(userId, null).overall();

        // Weak areas
        List<ChapterPerformanceDto> weakChapters = ctx.chapterPerformances().stream()
                .filter(p -> p.level() == PerformanceLevel.WEAK)
                .sorted(Comparator.comparingDouble(p -> p.accuracy() != null ? p.accuracy() : 0.0))
                .limit(5)
                .toList();

        WeakAreasResponse allWeakAreas = analyticsService.getWeakAreas(userId);
        List<LearningOverviewResponse.WeakTopicItemDto> weakTopics = allWeakAreas.items().stream()
                .filter(item -> "TOPIC".equals(item.type()))
                .limit(5)
                .map(item -> new LearningOverviewResponse.WeakTopicItemDto(
                        item.id(),
                        item.name(),
                        item.chapterId(),
                        item.chapterName(),
                        item.answerCount(),
                        item.accuracy()
                ))
                .toList();

        LearningOverviewResponse.WeakAreasOverview weakAreasOverview = new LearningOverviewResponse.WeakAreasOverview(
                weakChapters,
                weakTopics
        );

        // Mistakes
        MistakeSummaryResponse mistakeSummary = ctx.mistakeSummary();
        LearningOverviewResponse.MistakesOverview mistakesOverview = new LearningOverviewResponse.MistakesOverview(
                mistakeSummary != null ? mistakeSummary.totalActive() : 0L,
                mistakeSummary != null ? mistakeSummary.totalPracticeable() : 0L,
                mistakeSummary != null ? mistakeSummary.totalMastered() : 0L
        );

        // Recent Activity (last 5 COMPLETED of any type)
        List<RecentActivityDto> recentActivities = fetchRecentActivities(userId, 5);

        // Active Study Session
        ActiveStudySessionSummaryDto activeSessionDto = null;
        List<StudySession> sessions = studySessionRepository.findByUserIdOrderByStartedAtDesc(userId);
        OffsetDateTime now = OffsetDateTime.now();
        for (StudySession s : sessions) {
            if (s.getStatus() == StudySessionStatus.ACTIVE) {
                OffsetDateTime endsAt = s.getStartedAt().plusMinutes(s.getDurationMinutes());
                OffsetDateTime graceEndsAt = endsAt.plusMinutes(learningProperties.getStudySessionGraceMinutes());
                if (now.isAfter(graceEndsAt)) {
                    s.setStatus(StudySessionStatus.ABANDONED);
                    studySessionRepository.save(s);
                } else {
                    activeSessionDto = new ActiveStudySessionSummaryDto(s.getSessionId(), endsAt);
                    break;
                }
            }
        }

        return new LearningOverviewResponse(
                onboarding,
                progress,
                weakAreasOverview,
                mistakesOverview,
                recommendation,
                recentActivities,
                activeSessionDto,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<RecommendationCandidate> getAdviceCandidates(Long userId) {
        RecommendationContext ctx = buildContext(userId);
        List<RecommendationCandidate> candidates = new ArrayList<>();

        // 1. Published products linked to WEAK chapters
        for (var p : ctx.chapterPerformances()) {
            if (p.level() == PerformanceLevel.WEAK) {
                List<LearningResourceInfo> resources = ctx.resourcesByChapter().getOrDefault(p.chapterId(), List.of());
                for (var r : resources) {
                    RecommendationTarget target = new RecommendationTarget(
                            "PRODUCT",
                            p.chapterId(),
                            p.chapterName(),
                            p.subjectId(),
                            null,
                            r.productId(),
                            r.slug(),
                            r.productType(),
                            r.owned(),
                            ctx.weakTopicsByChapter().get(p.chapterId())
                    );
                    candidates.add(new RecommendationCandidate(
                            RecommendationType.REVIEW_RESOURCE,
                            "Review " + r.name(),
                            "WEAK_RESOURCE",
                            target
                    ));
                }

                // Chapter quiz of weak chapters
                Long quizId = ctx.availableQuizIdByChapter().get(p.chapterId());
                if (quizId != null) {
                    RecommendationTarget target = new RecommendationTarget(
                            "QUIZ",
                            p.chapterId(),
                            p.chapterName(),
                            p.subjectId(),
                            quizId,
                            null,
                            null,
                            null,
                            null,
                            ctx.weakTopicsByChapter().get(p.chapterId())
                    );
                    candidates.add(new RecommendationCandidate(
                            RecommendationType.TAKE_CHAPTER_QUIZ,
                            "Retake " + p.chapterName() + " Quiz",
                            "WEAK_QUIZ",
                            target
                    ));
                }
            }
        }

        // 2. Mistake practice
        if (ctx.mistakeSummary() != null && ctx.mistakeSummary().totalPracticeable() > 0) {
            candidates.add(new RecommendationCandidate(
                    RecommendationType.PRACTICE_MISTAKES,
                    "Practice Mistakes",
                    "OVERALL_MISTAKES",
                    RecommendationTarget.ofKind("MISTAKES")
            ));
        }

        // 3. Next chapter
        for (var c : ctx.syllabusChapters()) {
            if (c.participates()) {
                ChapterPerformanceDto p = ctx.chapterPerformances().stream()
                        .filter(cp -> cp.chapterId().equals(c.chapterId()))
                        .findFirst()
                        .orElse(null);
                if (p == null || p.level() == PerformanceLevel.INSUFFICIENT_DATA) {
                    Long quizId = ctx.availableQuizIdByChapter().get(c.chapterId());
                    RecommendationTarget target = (quizId != null)
                            ? new RecommendationTarget("QUIZ", c.chapterId(), c.chapterName(), c.subjectId(), quizId, null, null, null, null, null)
                            : new RecommendationTarget("CHAPTER", c.chapterId(), c.chapterName(), c.subjectId(), null, null, null, null, null, null);
                    candidates.add(new RecommendationCandidate(
                            RecommendationType.NEXT_CHAPTER,
                            "Next: " + c.chapterName(),
                            "NEXT_CHAPTER",
                            target
                    ));
                    break;
                }
            }
        }

        return candidates;
    }

    private RecommendationContext buildContext(Long userId) {
        boolean emailVerified = userLookupService.isEmailVerified(userId);
        OnboardingStatusResponse onboardingStatus = assessmentService.getOnboardingStatus(userId);

        List<ChapterPerformanceDto> performances = analyticsService.getChapterPerformances(userId, null);

        // Weak topics map
        WeakAreasResponse weakAreas = analyticsService.getWeakAreas(userId);
        Map<Long, List<String>> weakTopicsByChapter = new HashMap<>();
        for (var item : weakAreas.items()) {
            if ("TOPIC".equals(item.type()) && item.chapterId() != null) {
                weakTopicsByChapter.computeIfAbsent(item.chapterId(), k -> new ArrayList<>()).add(item.name());
            }
        }

        // Mistakes summary
        MistakeSummaryResponse mistakeSummary = mistakeService.getMistakesSummary(userId);
        Map<Long, Integer> activePracticeableMistakesByChapter = new HashMap<>();
        if (mistakeSummary != null && mistakeSummary.chapterBreakdown() != null) {
            for (var cb : mistakeSummary.chapterBreakdown()) {
                activePracticeableMistakesByChapter.put(cb.chapterId(),
                        Math.toIntExact(cb.practiceableCount()));
            }
        }

        // Syllabus chapters in subject order, then chapter_order
        List<RecommendationContext.ChapterOrderInfo> syllabusChapters = new ArrayList<>();
        List<Long> activeSubIds = subjectLookupService.getActiveSubjectIds();
        var subjects = subjectLookupService.getSubjectsInfo(activeSubIds).values().stream()
                .filter(SubjectLookupService.SubjectBasicInfo::isActive)
                .sorted(Comparator.comparing(SubjectLookupService.SubjectBasicInfo::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<Long, Long> availableQuizIdByChapter = new HashMap<>();
        Map<Long, List<LearningResourceInfo>> resourcesByChapter = new HashMap<>();

        VideoOwnershipService videoOwnershipService = videoOwnershipServiceProvider.getIfAvailable();

        for (var subj : subjects) {
            List<Long> chapIds = chapterValidationService.getActiveChapterIdsBySubject(subj.subjectId());
            if (chapIds.isEmpty()) continue;

            var summaries = new java.util.ArrayList<>(
                    chapterValidationService.getPublicChapterSummaries(chapIds)
            );
            summaries.sort(Comparator.comparing(ChapterValidationService.PublicChapterSummary::chapterOrder, Comparator.nullsLast(Integer::compareTo)));

            for (var cSum : summaries) {
                Long cid = cSum.chapterId();
                boolean participates = questionEligibility.isChapterParticipating(cid);
                syllabusChapters.add(new RecommendationContext.ChapterOrderInfo(
                        cid,
                        cSum.name(),
                        cSum.chapterOrder(),
                        subj.subjectId(),
                        subj.name(),
                        participates
                ));

                // Check published quiz
                var optQuiz = quizRepository.findByChapterId(cid);
                if (optQuiz.isPresent() && optQuiz.get().getStatus() == QuizStatus.PUBLISHED && participates) {
                    availableQuizIdByChapter.put(cid, optQuiz.get().getQuizId());
                }

                // Check published products
                try {
                    List<PublicProductSummaryResponse> products = catalogService.getPublicProductsForChapter(cid);
                    if (products != null && !products.isEmpty()) {
                        List<LearningResourceInfo> rList = new ArrayList<>();
                        for (var prod : products) {
                            boolean owned = false;
                            if (videoOwnershipService != null && prod.type() != null && "VIDEO".equalsIgnoreCase(prod.type().name())) {
                                owned = videoOwnershipService.userOwnsVideo(userId, prod.id());
                            }
                            rList.add(new LearningResourceInfo(
                                    prod.id(),
                                    prod.name(),
                                    prod.slug(),
                                    prod.type() != null ? prod.type().name() : null,
                                    cid,
                                    owned
                            ));
                        }
                        resourcesByChapter.put(cid, rList);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Most recent answer times by chapter
        Map<Long, OffsetDateTime> mostRecentAnswerByChapter = new HashMap<>();
        List<Object[]> gradedAnswers = quizAnswerRepository.findAllUserGradedAnswers(userId);
        for (Object[] row : gradedAnswers) {
            Long cId = ((Number) row[0]).longValue();
            if (!mostRecentAnswerByChapter.containsKey(cId)) {
                Object tsObj = row[3];
                if (tsObj instanceof OffsetDateTime odt) {
                    mostRecentAnswerByChapter.put(cId, odt);
                } else if (tsObj instanceof java.sql.Timestamp ts) {
                    mostRecentAnswerByChapter.put(cId, ts.toInstant().atOffset(java.time.ZoneOffset.UTC));
                }
            }
        }

        return new RecommendationContext(
                userId,
                emailVerified,
                onboardingStatus.offerInitialDiagnostic(),
                onboardingStatus.reason(),
                performances,
                weakTopicsByChapter,
                mistakeSummary,
                activePracticeableMistakesByChapter,
                resourcesByChapter,
                availableQuizIdByChapter,
                mostRecentAnswerByChapter,
                syllabusChapters,
                learningProperties.getRecommendationVeryWeakAccuracy(),
                learningProperties.getRecommendationMinMistakesForPractice()
        );
    }

    private List<RecentActivityDto> fetchRecentActivities(Long userId, int limit) {
        List<RecentActivityDto> activities = new ArrayList<>();

        // Completed quiz attempts
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(userId, PageRequest.of(0, limit));
        for (QuizAttempt att : attempts) {
            String title = "Quiz Attempt";
            String actType = att.getAttemptType() != null ? att.getAttemptType().name() : "CHAPTER_QUIZ";

            if (att.getAttemptType() == QuizAttemptType.CHAPTER_QUIZ && att.getQuizId() != null) {
                var qOpt = quizRepository.findById(att.getQuizId());
                if (qOpt.isPresent()) {
                    title = qOpt.get().getTitle();
                }
            } else if (att.getAttemptType() == QuizAttemptType.MISTAKE_PRACTICE) {
                title = "Mistake Practice";
            } else if (att.getAttemptType() == QuizAttemptType.STUDY_SESSION) {
                title = "Study Session";
            }

            int score = att.getScore() != null ? att.getScore() : 0;
            int total = att.getTotalMarks() > 0 ? att.getTotalMarks() : 1;
            int pct = (int) Math.round(score * 100.0 / total);

            activities.add(new RecentActivityDto(
                    actType,
                    att.getAttemptId(),
                    title,
                    score,
                    total,
                    pct,
                    att.getCompletedAt()
            ));
        }

        // Completed assessments
        List<DiagnosticAssessment> assessments = diagnosticAssessmentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, AssessmentStatus.COMPLETED, PageRequest.of(0, limit)
        ).getContent();
        for (DiagnosticAssessment da : assessments) {
            String title = da.getAssessmentType() == AssessmentType.INITIAL_DIAGNOSTIC
                    ? "Initial Diagnostic Assessment"
                    : "Overall Syllabus Quiz";
            int score = da.getScore() != null ? da.getScore() : 0;
            int total = da.getTotalMarks() > 0 ? da.getTotalMarks() : 1;
            int pct = (int) Math.round(score * 100.0 / total);

            activities.add(new RecentActivityDto(
                    da.getAssessmentType().name(),
                    da.getAssessmentId(),
                    title,
                    score,
                    total,
                    pct,
                    da.getCompletedAt() != null ? da.getCompletedAt() : da.getCreatedAt()
            ));
        }

        activities.sort(Comparator.comparing(
                RecentActivityDto::completedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        if (activities.size() > limit) {
            return activities.subList(0, limit);
        }
        return activities;
    }
}
