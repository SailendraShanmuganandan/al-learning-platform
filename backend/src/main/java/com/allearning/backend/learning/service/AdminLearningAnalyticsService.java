package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.entity.*;
import com.allearning.backend.learning.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminLearningAnalyticsService {

    private final QuizAnswerRepository quizAnswerRepository;
    private final DiagnosticAssessmentAnswerRepository diagnosticAssessmentAnswerRepository;
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final ChapterTopicRepository chapterTopicRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final DiagnosticAssessmentRepository diagnosticAssessmentRepository;
    private final PerformanceCalculator performanceCalculator;
    private final QuestionEligibility questionEligibility;
    private final ChapterValidationService chapterValidationService;
    private final SubjectLookupService subjectLookupService;
    private final AnalyticsService analyticsService;
    private final MistakeService mistakeService;
    private final LearningUserLookupService userLookupService;
    private final LearningProperties learningProperties;

    public AdminLearningAnalyticsService(
            QuizAnswerRepository quizAnswerRepository,
            DiagnosticAssessmentAnswerRepository diagnosticAssessmentAnswerRepository,
            QuestionRepository questionRepository,
            QuizRepository quizRepository,
            ChapterTopicRepository chapterTopicRepository,
            QuizAttemptRepository quizAttemptRepository,
            DiagnosticAssessmentRepository diagnosticAssessmentRepository,
            PerformanceCalculator performanceCalculator,
            QuestionEligibility questionEligibility,
            ChapterValidationService chapterValidationService,
            SubjectLookupService subjectLookupService,
            AnalyticsService analyticsService,
            MistakeService mistakeService,
            LearningUserLookupService userLookupService,
            LearningProperties learningProperties
    ) {
        this.quizAnswerRepository = quizAnswerRepository;
        this.diagnosticAssessmentAnswerRepository = diagnosticAssessmentAnswerRepository;
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
        this.chapterTopicRepository = chapterTopicRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.diagnosticAssessmentRepository = diagnosticAssessmentRepository;
        this.performanceCalculator = performanceCalculator;
        this.questionEligibility = questionEligibility;
        this.chapterValidationService = chapterValidationService;
        this.subjectLookupService = subjectLookupService;
        this.analyticsService = analyticsService;
        this.mistakeService = mistakeService;
        this.userLookupService = userLookupService;
        this.learningProperties = learningProperties;
    }

    public AdminLearningOverviewResponse getOverview(Long subjectId) {
        int cap = learningProperties.getAdminAnalyticsMaxStudents();
        List<Long> activeStudentIds = userLookupService.getActiveStudentIds(cap);

        if (activeStudentIds.isEmpty()) {
            return new AdminLearningOverviewResponse(0, 0, List.of());
        }

        // Scope chapters
        List<Long> activeChapterIds;
        if (subjectId != null) {
            activeChapterIds = chapterValidationService.getActiveChapterIdsBySubject(subjectId);
        } else {
            activeChapterIds = chapterValidationService.getActiveChapterIds();
        }

        // Filter to participating chapters
        List<Long> participatingChapters = activeChapterIds.stream()
                .filter(questionEligibility::isChapterParticipating)
                .toList();

        if (participatingChapters.isEmpty()) {
            return new AdminLearningOverviewResponse(0, 0, List.of());
        }

        var chapterSummaries = chapterValidationService.getPublicChapterSummaries(participatingChapters);
        Map<Long, ChapterValidationService.PublicChapterSummary> summaryMap = chapterSummaries.stream()
                .collect(Collectors.toMap(ChapterValidationService.PublicChapterSummary::chapterId, s -> s));

        // Load graded answers for all active students
        List<Object[]> rows = quizAnswerRepository.findGradedAnswersForUsers(activeStudentIds);

        // Map: chapterId -> (userId -> list of answers)
        Map<Long, Map<Long, List<Boolean>>> chapterUserAnswers = new HashMap<>();
        Map<Long, Long> chapterTotalAnswers = new HashMap<>();
        Set<Long> studentsWithAnyData = new HashSet<>();
        long totalGradedAnswers = 0;

        for (Object[] row : rows) {
            Long uId = ((Number) row[0]).longValue();
            Long cId = ((Number) row[1]).longValue();
            Boolean isCorrect = (Boolean) row[3];

            studentsWithAnyData.add(uId);
            totalGradedAnswers++;

            if (participatingChapters.contains(cId)) {
                chapterUserAnswers.computeIfAbsent(cId, k -> new HashMap<>())
                        .computeIfAbsent(uId, k -> new ArrayList<>())
                        .add(isCorrect);
                chapterTotalAnswers.put(cId, chapterTotalAnswers.getOrDefault(cId, 0L) + 1);
            }
        }

        List<AdminChapterPerformanceSummaryDto> chapterDtos = new ArrayList<>();

        for (Long cId : participatingChapters) {
            var cSummary = summaryMap.get(cId);
            String cName = cSummary != null ? cSummary.name() : "Chapter " + cId;
            Long sId = cSummary != null ? cSummary.subjectId() : null;
            String sName = cSummary != null ? cSummary.subjectName() : null;

            Map<Long, List<Boolean>> userAnswers = chapterUserAnswers.getOrDefault(cId, Map.of());
            int studentsWithData = 0;
            int weakCount = 0;
            int devCount = 0;
            int strongCount = 0;
            double accuracySum = 0.0;

            for (var entry : userAnswers.entrySet()) {
                List<Boolean> answers = entry.getValue();
                // Take up to 20 most recent
                List<Boolean> window = answers.size() > 20 ? answers.subList(0, 20) : answers;
                var perf = performanceCalculator.calculate(
                        window,
                        PerformanceScope.CHAPTER,
                        learningProperties
                );

                if (perf.level() != PerformanceLevel.INSUFFICIENT_DATA) {
                    studentsWithData++;
                    if (perf.accuracy() != null) {
                        accuracySum += perf.accuracy();
                    }
                    if (perf.level() == PerformanceLevel.WEAK) weakCount++;
                    else if (perf.level() == PerformanceLevel.DEVELOPING) devCount++;
                    else if (perf.level() == PerformanceLevel.STRONG) strongCount++;
                }
            }

            Double weakPercent = studentsWithData > 0 ? (weakCount * 100.0 / studentsWithData) : null;
            Double avgAccuracy = studentsWithData > 0 ? (accuracySum / studentsWithData) : null;
            long totalAnswers = chapterTotalAnswers.getOrDefault(cId, 0L);

            chapterDtos.add(new AdminChapterPerformanceSummaryDto(
                    cId,
                    cName,
                    sId,
                    sName,
                    studentsWithData,
                    weakCount,
                    devCount,
                    strongCount,
                    weakPercent,
                    avgAccuracy,
                    totalAnswers
            ));
        }

        // Sort: weakPercent desc (nulls last), then chapter order
        chapterDtos.sort((a, b) -> {
            if (a.weakPercent() == null && b.weakPercent() == null) {
                Integer ordA = summaryMap.containsKey(a.chapterId()) ? summaryMap.get(a.chapterId()).chapterOrder() : 0;
                Integer ordB = summaryMap.containsKey(b.chapterId()) ? summaryMap.get(b.chapterId()).chapterOrder() : 0;
                return Integer.compare(ordA != null ? ordA : 0, ordB != null ? ordB : 0);
            }
            if (a.weakPercent() == null) return 1;
            if (b.weakPercent() == null) return -1;
            int cmp = Double.compare(b.weakPercent(), a.weakPercent()); // Descending
            if (cmp != 0) return cmp;
            Integer ordA = summaryMap.containsKey(a.chapterId()) ? summaryMap.get(a.chapterId()).chapterOrder() : 0;
            Integer ordB = summaryMap.containsKey(b.chapterId()) ? summaryMap.get(b.chapterId()).chapterOrder() : 0;
            return Integer.compare(ordA != null ? ordA : 0, ordB != null ? ordB : 0);
        });

        return new AdminLearningOverviewResponse(
                studentsWithAnyData.size(),
                totalGradedAnswers,
                chapterDtos
        );
    }

    public AdminChapterAnalyticsDetailResponse getChapterDetail(Long chapterId) {
        var optSummary = chapterValidationService.getChapterSummary(chapterId);
        if (optSummary.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found: " + chapterId);
        }

        String chapterName = optSummary.get().name();
        Long subjectId = optSummary.get().subjectId();
        String subjectName = subjectId != null
                ? subjectLookupService.getSubjectInfo(subjectId).map(SubjectLookupService.SubjectBasicInfo::name).orElse(null)
                : null;

        // Topics
        List<ChapterTopic> topics = chapterTopicRepository.findByChapterIdAndStatusOrderByNameAsc(chapterId, TopicStatus.ACTIVE);
        Map<Long, ChapterTopic> topicMap = topics.stream().collect(Collectors.toMap(ChapterTopic::getTopicId, t -> t));

        int cap = learningProperties.getAdminAnalyticsMaxStudents();
        List<Long> activeStudentIds = userLookupService.getActiveStudentIds(cap);

        // Load answers for this chapter
        List<Object[]> rows = quizAnswerRepository.findGradedAnswersForUsers(activeStudentIds);

        // Map: topicId -> (userId -> list of answers)
        Map<Long, Map<Long, List<Boolean>>> topicUserAnswers = new HashMap<>();
        for (Object[] row : rows) {
            Long uId = ((Number) row[0]).longValue();
            Long cId = ((Number) row[1]).longValue();
            if (!chapterId.equals(cId)) continue;

            Object topicObj = row[2];
            if (topicObj != null) {
                Long tId = ((Number) topicObj).longValue();
                Boolean isCorrect = (Boolean) row[3];
                topicUserAnswers.computeIfAbsent(tId, k -> new HashMap<>())
                        .computeIfAbsent(uId, k -> new ArrayList<>())
                        .add(isCorrect);
            }
        }

        List<AdminTopicPerformanceSummaryDto> topicDtos = new ArrayList<>();
        for (ChapterTopic topic : topics) {
            Long tId = topic.getTopicId();
            Map<Long, List<Boolean>> userAns = topicUserAnswers.getOrDefault(tId, Map.of());

            int studentsWithData = 0;
            int weakCount = 0;
            double accSum = 0.0;

            for (var entry : userAns.entrySet()) {
                List<Boolean> answers = entry.getValue();
                List<Boolean> window = answers.size() > 20 ? answers.subList(0, 20) : answers;
                var perf = performanceCalculator.calculate(
                        window,
                        answers.size(),
                        PerformanceScope.TOPIC,
                        learningProperties
                );

                if (perf.level() != PerformanceLevel.INSUFFICIENT_DATA) {
                    studentsWithData++;
                    if (perf.accuracy() != null) {
                        accSum += perf.accuracy();
                    }
                    if (perf.level() == PerformanceLevel.WEAK) {
                        weakCount++;
                    }
                }
            }

            Double avgAcc = studentsWithData > 0 ? (accSum / studentsWithData) : null;
            topicDtos.add(new AdminTopicPerformanceSummaryDto(
                    tId,
                    topic.getName(),
                    studentsWithData,
                    weakCount,
                    avgAcc
            ));
        }

        // Hardest questions: top 10 questions with answerCount >= 5, lowest correctRate
        List<Question> questions = questionRepository.findAllByChapterId(chapterId);
        List<Long> qIds = questions.stream().map(Question::getQuestionId).toList();

        Map<Long, long[]> statsMap = new HashMap<>(); // qId -> [total, correct]
        if (!qIds.isEmpty()) {
            List<QuestionAnswerStats> quizStats = quizAnswerRepository.getCompletedStatsForQuestions(qIds);
            for (var s : quizStats) {
                long[] c = statsMap.computeIfAbsent(s.questionId(), k -> new long[2]);
                c[0] += s.answerCount();
                c[1] += s.correctCount();
            }

            List<QuestionAnswerStats> daStats = diagnosticAssessmentAnswerRepository.getCompletedStatsForQuestions(qIds);
            for (var s : daStats) {
                long[] c = statsMap.computeIfAbsent(s.questionId(), k -> new long[2]);
                c[0] += s.answerCount();
                c[1] += s.correctCount();
            }
        }

        List<AdminHardestQuestionDto> hardestCandidates = new ArrayList<>();
        for (Question q : questions) {
            long[] counts = statsMap.get(q.getQuestionId());
            long total = counts != null ? counts[0] : 0L;
            if (total >= 5) {
                long correct = counts[1];
                double rate = (double) correct / total;
                String truncatedText = q.getQuestionText();
                if (truncatedText != null && truncatedText.length() > 120) {
                    truncatedText = truncatedText.substring(0, 120);
                }
                hardestCandidates.add(new AdminHardestQuestionDto(
                        q.getQuestionId(),
                        truncatedText,
                        q.getStatus(),
                        total,
                        rate
                ));
            }
        }

        hardestCandidates.sort(Comparator.comparingDouble(AdminHardestQuestionDto::correctRate));
        List<AdminHardestQuestionDto> topHardest = hardestCandidates.stream().limit(10).toList();

        return new AdminChapterAnalyticsDetailResponse(
                chapterId,
                chapterName,
                subjectId,
                subjectName,
                topicDtos,
                topHardest
        );
    }

    public AdminStudentLearningDetailResponse getStudentDetail(Long studentUserId) {
        var profileOpt = userLookupService.getStudentProfile(studentUserId);
        if (profileOpt.isEmpty() || !profileOpt.get().isStudent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentUserId);
        }

        var profile = profileOpt.get();
        List<ChapterPerformanceDto> chapterPerformances = analyticsService.getChapterPerformances(studentUserId, null);
        WeakAreasResponse weakAreas = analyticsService.getWeakAreas(studentUserId);
        MistakeSummaryResponse mistakeSummary = mistakeService.getMistakesSummary(studentUserId);

        // Fetch 10 most recent completed activities
        List<RecentActivityDto> activities = new ArrayList<>();

        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(studentUserId, PageRequest.of(0, 10));
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

        List<DiagnosticAssessment> assessments = diagnosticAssessmentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                studentUserId, AssessmentStatus.COMPLETED, PageRequest.of(0, 10)
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

        activities.sort(Comparator.comparing(RecentActivityDto::completedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        if (activities.size() > 10) {
            activities = activities.subList(0, 10);
        }

        return new AdminStudentLearningDetailResponse(
                profile.userId(),
                profile.fullName(),
                profile.email(),
                chapterPerformances,
                weakAreas,
                mistakeSummary,
                activities
        );
    }
}
