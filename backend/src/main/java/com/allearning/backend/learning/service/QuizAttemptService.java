package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.entity.*;
import com.allearning.backend.learning.event.GradedAnswersEvent;
import com.allearning.backend.learning.event.GradedSourceType;
import com.allearning.backend.learning.exception.LearningConflictException;
import com.allearning.backend.learning.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ChapterTopicRepository chapterTopicRepository;
    private final QuestionEligibility questionEligibility;
    private final QuestionSamplingService questionSamplingService;
    private final AnswerGrader answerGrader;
    private final ChapterValidationService chapterValidationService;
    private final SubjectLookupService subjectLookupService;
    private final LearningProperties learningProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final QuestionReportRepository questionReportRepository;
    private final org.springframework.beans.factory.ObjectProvider<StudySessionSubmissionGuard> studySessionSubmissionGuardProvider;

    public QuizAttemptService(
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizAnswerRepository quizAnswerRepository,
            ChapterTopicRepository chapterTopicRepository,
            QuestionEligibility questionEligibility,
            QuestionSamplingService questionSamplingService,
            AnswerGrader answerGrader,
            ChapterValidationService chapterValidationService,
            SubjectLookupService subjectLookupService,
            LearningProperties learningProperties,
            ApplicationEventPublisher eventPublisher,
            QuestionReportRepository questionReportRepository,
            org.springframework.beans.factory.ObjectProvider<StudySessionSubmissionGuard> studySessionSubmissionGuardProvider
    ) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.chapterTopicRepository = chapterTopicRepository;
        this.questionEligibility = questionEligibility;
        this.questionSamplingService = questionSamplingService;
        this.answerGrader = answerGrader;
        this.chapterValidationService = chapterValidationService;
        this.subjectLookupService = subjectLookupService;
        this.learningProperties = learningProperties;
        this.eventPublisher = eventPublisher;
        this.questionReportRepository = questionReportRepository;
        this.studySessionSubmissionGuardProvider = studySessionSubmissionGuardProvider;
    }

    @Transactional(readOnly = true)
    public QuizAvailabilityResponse getQuizAvailability(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found: " + quizId));

        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz is not published: " + quizId);
        }

        Long chapterId = quiz.getChapterId();
        if (!chapterValidationService.isChapterActive(chapterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter is not active: " + chapterId);
        }

        String chapterName = chapterValidationService.getChapterSummary(chapterId)
                .map(ChapterValidationService.ChapterSummary::name)
                .orElse("Chapter " + chapterId);

        Map<String, Long> langCounts = questionEligibility.eligibleLanguageCounts(chapterId);
        List<LanguageCountDto> languages = langCounts.entrySet().stream()
                .map(e -> new LanguageCountDto(e.getKey(), e.getValue()))
                .toList();

        long defaultLangEligible = questionEligibility.countEligible(chapterId, "EN");
        boolean available = defaultLangEligible >= learningProperties.getMinEligibleQuestions();
        String unavailableReason = available ? null : "NOT_ENOUGH_QUESTIONS";

        return new QuizAvailabilityResponse(
                quiz.getQuizId(),
                chapterId,
                chapterName,
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getStatus(),
                available,
                unavailableReason,
                learningProperties.getMinEligibleQuestions(),
                learningProperties.getAttemptQuestionCount(),
                languages,
                "EN",
                true,
                false
        );
    }

    @Transactional(readOnly = true)
    public QuizAvailabilityResponse getChapterQuizAvailability(Long chapterId) {
        if (!chapterValidationService.isChapterActive(chapterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter is not active or does not exist: " + chapterId);
        }

        String chapterName = chapterValidationService.getChapterSummary(chapterId)
                .map(ChapterValidationService.ChapterSummary::name)
                .orElse("Chapter " + chapterId);

        Optional<Quiz> optQuiz = quizRepository.findByChapterId(chapterId);
        if (optQuiz.isEmpty() || optQuiz.get().getStatus() != QuizStatus.PUBLISHED) {
            return new QuizAvailabilityResponse(
                    optQuiz.map(Quiz::getQuizId).orElse(null),
                    chapterId,
                    chapterName,
                    null,
                    null,
                    optQuiz.map(Quiz::getStatus).orElse(null),
                    false,
                    "NO_QUIZ",
                    learningProperties.getMinEligibleQuestions(),
                    learningProperties.getAttemptQuestionCount(),
                    List.of(),
                    "EN",
                    false,
                    false
            );
        }

        return getQuizAvailability(optQuiz.get().getQuizId());
    }

    public AttemptDetailResponse startChapterQuizAttempt(Long userId, Long quizId, String language) {
        // 1. Resume existing incomplete attempt if present
        Optional<QuizAttempt> existingOpt = quizAttemptRepository.findFirstByUserIdAndQuizIdAndAttemptTypeAndCompletedAtIsNull(
                userId, quizId, QuizAttemptType.CHAPTER_QUIZ
        );
        if (existingOpt.isPresent()) {
            return buildAttemptDetail(existingOpt.get(), true);
        }

        // 2. Validate quiz and chapter active
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found: " + quizId));

        if (quiz.getStatus() != QuizStatus.PUBLISHED || !chapterValidationService.isChapterActive(quiz.getChapterId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz or chapter is not active");
        }

        Long chapterId = quiz.getChapterId();
        int requestedCount = learningProperties.getAttemptQuestionCount();

        // 3. Sample questions
        List<Question> sampled = questionSamplingService.sampleForChapter(userId, chapterId, language, requestedCount);

        try {
            QuizAttempt attempt = new QuizAttempt();
            attempt.setUserId(userId);
            attempt.setAttemptType(QuizAttemptType.CHAPTER_QUIZ);
            attempt.setQuizId(quizId);
            attempt.setTotalMarks(sampled.size());
            attempt.setStartedAt(OffsetDateTime.now());
            QuizAttempt savedAttempt = quizAttemptRepository.saveAndFlush(attempt);

            int order = 1;
            for (Question q : sampled) {
                QuizAnswer qa = new QuizAnswer();
                qa.setAttemptId(savedAttempt.getAttemptId());
                qa.setQuestionId(q.getQuestionId());
                qa.setQuestionOrder(order++);
                qa.setSelectedOptionId(null);
                qa.setIsCorrect(null);
                quizAnswerRepository.save(qa);
            }
            quizAnswerRepository.flush();

            return buildAttemptDetail(savedAttempt, false);
        } catch (DataIntegrityViolationException ex) {
            // Race condition: another request created incomplete attempt in parallel
            Optional<QuizAttempt> resumed = quizAttemptRepository.findFirstByUserIdAndQuizIdAndAttemptTypeAndCompletedAtIsNull(
                    userId, quizId, QuizAttemptType.CHAPTER_QUIZ
            );
            if (resumed.isPresent()) {
                return buildAttemptDetail(resumed.get(), true);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public AttemptDetailResponse getAttemptDetail(Long userId, Long attemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId);
        }

        return buildAttemptDetail(attempt, false);
    }

    public AttemptDetailResponse submitAttempt(Long userId, Long attemptId, SubmitAnswersRequest request) {
        QuizAttempt attempt = quizAttemptRepository.findByIdWithLock(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId));

        if (!attempt.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found: " + attemptId);
        }

        if (attempt.getCompletedAt() != null) {
            throw new LearningConflictException("ATTEMPT_ALREADY_SUBMITTED", "This attempt has already been submitted");
        }

        if (attempt.getStudySessionId() != null && studySessionSubmissionGuardProvider != null) {
            studySessionSubmissionGuardProvider.ifAvailable(guard -> guard.assertOpen(attempt.getStudySessionId()));
        }

        List<QuizAnswer> answerRows = quizAnswerRepository.findByAttemptIdOrderByQuestionOrderAsc(attemptId);
        Set<Long> qIds = answerRows.stream().map(QuizAnswer::getQuestionId).collect(Collectors.toSet());
        List<Question> questions = questionRepository.findByQuestionIdIn(qIds);
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Set<Long> quizIds = questions.stream().map(Question::getQuizId).collect(Collectors.toSet());
        Map<Long, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getQuizId, qz -> qz));

        List<AnswerGrader.ServedQuestionForGrading> servedForGrading = new ArrayList<>();
        for (QuizAnswer row : answerRows) {
            Question q = questionMap.get(row.getQuestionId());
            if (q == null) continue;

            Set<Long> optIds = q.getOptions().stream().map(QuestionOption::getOptionId).collect(Collectors.toSet());
            Long correctOptId = q.getOptions().stream()
                    .filter(QuestionOption::isCorrect)
                    .map(QuestionOption::getOptionId)
                    .findFirst()
                    .orElse(null);

            Quiz qz = quizMap.get(q.getQuizId());
            Long chapterId = qz != null ? qz.getChapterId() : null;

            servedForGrading.add(new AnswerGrader.ServedQuestionForGrading(
                    q.getQuestionId(),
                    chapterId,
                    q.getTopicId(),
                    optIds,
                    correctOptId,
                    q.getExplanation()
            ));
        }

        AnswerGrader.GradedResult graded = answerGrader.grade(servedForGrading, request.answers());

        // Update stored answers
        List<GradedAnswersEvent.GradedAnswerItem> eventItems = new ArrayList<>();
        for (QuizAnswer row : answerRows) {
            AnswerGrader.GradedAnswerDetail detail = graded.answersByQuestionId().get(row.getQuestionId());
            if (detail != null) {
                row.setSelectedOptionId(detail.selectedOptionId());
                row.setIsCorrect(detail.isCorrect());
                quizAnswerRepository.save(row);

                Question q = questionMap.get(row.getQuestionId());
                Quiz qz = q != null ? quizMap.get(q.getQuizId()) : null;
                Long chapterId = qz != null ? qz.getChapterId() : null;

                eventItems.add(new GradedAnswersEvent.GradedAnswerItem(
                        row.getQuestionId(),
                        chapterId,
                        q != null ? q.getTopicId() : null,
                        detail.isCorrect()
                ));
            }
        }

        attempt.setScore(graded.score());
        attempt.setCompletedAt(OffsetDateTime.now());
        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        // Publish synchronous GradedAnswersEvent
        GradedSourceType sourceType = switch (savedAttempt.getAttemptType()) {
            case CHAPTER_QUIZ -> GradedSourceType.CHAPTER_QUIZ;
            case MISTAKE_PRACTICE -> GradedSourceType.MISTAKE_PRACTICE;
            case STUDY_SESSION -> GradedSourceType.STUDY_SESSION;
        };

        eventPublisher.publishEvent(new GradedAnswersEvent(
                userId,
                sourceType,
                savedAttempt.getAttemptId(),
                savedAttempt.getStudySessionId(),
                savedAttempt.getCompletedAt(),
                eventItems
        ));

        return buildAttemptDetail(savedAttempt, false);
    }

    @Transactional(readOnly = true)
    public MyAttemptsResponse getMyAttempts(Long userId, Long quizId, Pageable pageable) {
        Page<QuizAttempt> page = quizAttemptRepository.findByUserIdAndQuizIdAndAttemptTypeAndCompletedAtIsNotNullOrderByStartedAtDesc(
                userId, quizId, QuizAttemptType.CHAPTER_QUIZ, pageable
        );

        List<QuizAttempt> allCompleted = quizAttemptRepository.findByUserIdAndQuizIdAndAttemptTypeAndCompletedAtIsNotNullOrderByStartedAtDesc(
                userId, quizId, QuizAttemptType.CHAPTER_QUIZ
        );

        List<AttemptSummaryDto> items = page.getContent().stream().map(a -> {
            int score = a.getScore() != null ? a.getScore() : 0;
            double pct = a.getTotalMarks() > 0 ? (Math.round((score * 100.0 / a.getTotalMarks()) * 10.0) / 10.0) : 0.0;
            return new AttemptSummaryDto(
                    a.getAttemptId(),
                    score,
                    a.getTotalMarks(),
                    pct,
                    a.getStartedAt(),
                    a.getCompletedAt()
            );
        }).toList();

        long count = allCompleted.size();
        Double best = null;
        Double latest = null;
        Double avg = null;

        if (count > 0) {
            double sum = 0;
            double max = 0;
            for (QuizAttempt a : allCompleted) {
                int s = a.getScore() != null ? a.getScore() : 0;
                double p = a.getTotalMarks() > 0 ? (s * 100.0 / a.getTotalMarks()) : 0.0;
                if (p > max) max = p;
                sum += p;
            }
            best = Math.round(max * 10.0) / 10.0;
            avg = Math.round((sum / count) * 10.0) / 10.0;

            QuizAttempt newest = allCompleted.get(0);
            int ns = newest.getScore() != null ? newest.getScore() : 0;
            latest = newest.getTotalMarks() > 0 ? (Math.round((ns * 100.0 / newest.getTotalMarks()) * 10.0) / 10.0) : 0.0;
        }

        MyAttemptsSummary summary = new MyAttemptsSummary(count, best, latest, avg);
        return new MyAttemptsResponse(new PageImpl<>(items, pageable, page.getTotalElements()), summary);
    }

    /**
     * Programmatic helper for creating attempts from other services (e.g. study sessions).
     */
    public AttemptDetailResponse createAttempt(
            Long userId,
            QuizAttemptType attemptType,
            Long quizId,
            Long studySessionId,
            List<Question> questions
    ) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(userId);
        attempt.setAttemptType(attemptType);
        attempt.setQuizId(quizId);
        attempt.setStudySessionId(studySessionId);
        attempt.setTotalMarks(questions.size());
        attempt.setStartedAt(OffsetDateTime.now());
        QuizAttempt saved = quizAttemptRepository.save(attempt);

        int order = 1;
        for (Question q : questions) {
            QuizAnswer qa = new QuizAnswer();
            qa.setAttemptId(saved.getAttemptId());
            qa.setQuestionId(q.getQuestionId());
            qa.setQuestionOrder(order++);
            qa.setSelectedOptionId(null);
            qa.setIsCorrect(null);
            quizAnswerRepository.save(qa);
        }

        return buildAttemptDetail(saved, false);
    }

    private AttemptDetailResponse buildAttemptDetail(QuizAttempt attempt, boolean resumed) {
        boolean completed = attempt.getCompletedAt() != null;
        String status = completed ? "COMPLETED" : "IN_PROGRESS";

        List<QuizAnswer> answers = quizAnswerRepository.findByAttemptIdOrderByQuestionOrderAsc(attempt.getAttemptId());
        Set<Long> qIds = answers.stream().map(QuizAnswer::getQuestionId).collect(Collectors.toSet());
        List<Question> questions = questionRepository.findByQuestionIdIn(qIds);
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Set<Long> quizIds = questions.stream().map(Question::getQuizId).collect(Collectors.toSet());
        Map<Long, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getQuizId, qz -> qz));

        Set<Long> topicIds = questions.stream().map(Question::getTopicId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> topicNames = chapterTopicRepository.findByTopicIdIn(topicIds).stream()
                .collect(Collectors.toMap(ChapterTopic::getTopicId, ChapterTopic::getName));

        Long attemptChapterId = null;
        if (attempt.getQuizId() != null) {
            Quiz qz = quizRepository.findById(attempt.getQuizId()).orElse(null);
            if (qz != null) attemptChapterId = qz.getChapterId();
        }

        String language = "EN";
        if (!questions.isEmpty()) {
            language = questions.get(0).getLanguage();
        }

        if (!completed) {
            // IN_PROGRESS: serve StudentQuestionDto with options in stored option_order, NO answers, NO selectedOptionId
            List<StudentQuestionDto> studentQuestions = new ArrayList<>();
            for (QuizAnswer ans : answers) {
                Question q = questionMap.get(ans.getQuestionId());
                if (q == null) continue;

                Quiz qz = quizMap.get(q.getQuizId());
                Long qChapId = qz != null ? qz.getChapterId() : null;
                String cName = qChapId != null
                        ? chapterValidationService.getChapterSummary(qChapId).map(ChapterValidationService.ChapterSummary::name).orElse("Chapter " + qChapId)
                        : null;

                List<StudentOptionDto> opts = q.getOptions().stream()
                        .map(o -> new StudentOptionDto(o.getOptionId(), o.getOptionText(), o.getOptionOrder()))
                        .toList();

                studentQuestions.add(new StudentQuestionDto(
                        q.getQuestionId(),
                        ans.getQuestionOrder(),
                        q.getQuestionText(),
                        q.getDifficulty(),
                        q.getTopicId(),
                        q.getTopicId() != null ? topicNames.get(q.getTopicId()) : null,
                        qChapId,
                        cName,
                        opts
                ));
            }

            return new AttemptDetailResponse(
                    attempt.getAttemptId(),
                    attempt.getAttemptType(),
                    attempt.getQuizId(),
                    attemptChapterId,
                    status,
                    language,
                    null,
                    attempt.getTotalMarks(),
                    null,
                    attempt.getStartedAt(),
                    null,
                    resumed,
                    null,
                    studentQuestions
            );
        } else {
            // COMPLETED: serve ResultQuestionDto with selectedOptionId, correctOptionId, isCorrect, explanation
            List<ResultQuestionDto> resultQuestions = new ArrayList<>();
            Map<Long, int[]> chapterBreakdownCounts = new LinkedHashMap<>(); // chapterId -> [correct, total]

            Set<Long> reportedQuestionIds = (questionReportRepository != null && !qIds.isEmpty())
                    ? questionReportRepository.findReportedQuestionIdsByUserIdAndQuestionIds(attempt.getUserId(), qIds)
                    : Collections.emptySet();

            for (QuizAnswer ans : answers) {
                Question q = questionMap.get(ans.getQuestionId());
                if (q == null) continue;

                Quiz qz = quizMap.get(q.getQuizId());
                Long qChapId = qz != null ? qz.getChapterId() : null;
                String cName = qChapId != null
                        ? chapterValidationService.getChapterSummary(qChapId).map(ChapterValidationService.ChapterSummary::name).orElse("Chapter " + qChapId)
                        : null;

                Long correctOptId = q.getOptions().stream()
                        .filter(QuestionOption::isCorrect)
                        .map(QuestionOption::getOptionId)
                        .findFirst()
                        .orElse(null);

                List<StudentOptionDto> opts = q.getOptions().stream()
                        .map(o -> new StudentOptionDto(o.getOptionId(), o.getOptionText(), o.getOptionOrder()))
                        .toList();

                boolean isReported = reportedQuestionIds.contains(q.getQuestionId());

                resultQuestions.add(new ResultQuestionDto(
                        q.getQuestionId(),
                        ans.getQuestionOrder(),
                        q.getQuestionText(),
                        q.getDifficulty(),
                        q.getTopicId(),
                        q.getTopicId() != null ? topicNames.get(q.getTopicId()) : null,
                        qChapId,
                        cName,
                        opts,
                        ans.getSelectedOptionId(),
                        correctOptId,
                        ans.getIsCorrect(),
                        q.getExplanation(),
                        isReported
                ));

                if (qChapId != null) {
                    int[] c = chapterBreakdownCounts.computeIfAbsent(qChapId, k -> new int[2]);
                    c[1]++; // total
                    if (Boolean.TRUE.equals(ans.getIsCorrect())) {
                        c[0]++; // correct
                    }
                }
            }

            List<ChapterBreakdown> chapterBreakdowns = new ArrayList<>();
            for (Map.Entry<Long, int[]> entry : chapterBreakdownCounts.entrySet()) {
                Long cid = entry.getKey();
                int[] counts = entry.getValue();
                String cName = chapterValidationService.getChapterSummary(cid)
                        .map(ChapterValidationService.ChapterSummary::name)
                        .orElse("Chapter " + cid);
                chapterBreakdowns.add(new ChapterBreakdown(cid, cName, counts[0], counts[1]));
            }

            int score = attempt.getScore() != null ? attempt.getScore() : 0;
            double pct = attempt.getTotalMarks() > 0 ? (Math.round((score * 100.0 / attempt.getTotalMarks()) * 10.0) / 10.0) : 0.0;

            return new AttemptDetailResponse(
                    attempt.getAttemptId(),
                    attempt.getAttemptType(),
                    attempt.getQuizId(),
                    attemptChapterId,
                    status,
                    language,
                    score,
                    attempt.getTotalMarks(),
                    pct,
                    attempt.getStartedAt(),
                    attempt.getCompletedAt(),
                    null,
                    chapterBreakdowns,
                    resultQuestions
            );
        }
    }
}

