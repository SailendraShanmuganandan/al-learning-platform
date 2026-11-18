package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.entity.*;
import com.allearning.backend.learning.event.GradedAnswersEvent;
import com.allearning.backend.learning.exception.LearningConflictException;
import com.allearning.backend.learning.repository.*;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@Service
@Transactional
public class MistakeService {

    public static final int MASTERY_STREAK = 3;

    private final MistakeRecordRepository mistakeRecordRepository;
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final ChapterTopicRepository chapterTopicRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuestionEligibility questionEligibility;
    private final ChapterValidationService chapterValidationService;
    private final SubjectLookupService subjectLookupService;
    private final LearningProperties learningProperties;
    private final RandomGenerator randomGenerator;

    public MistakeService(
            MistakeRecordRepository mistakeRecordRepository,
            QuestionRepository questionRepository,
            QuizRepository quizRepository,
            ChapterTopicRepository chapterTopicRepository,
            QuizAttemptRepository quizAttemptRepository,
            QuizAnswerRepository quizAnswerRepository,
            QuestionEligibility questionEligibility,
            ChapterValidationService chapterValidationService,
            SubjectLookupService subjectLookupService,
            LearningProperties learningProperties,
            RandomGenerator randomGenerator
    ) {
        this.mistakeRecordRepository = mistakeRecordRepository;
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
        this.chapterTopicRepository = chapterTopicRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.questionEligibility = questionEligibility;
        this.chapterValidationService = chapterValidationService;
        this.subjectLookupService = subjectLookupService;
        this.learningProperties = learningProperties;
        this.randomGenerator = randomGenerator;
    }

    @EventListener
    public void onGradedAnswers(GradedAnswersEvent event) {
        if (event == null || event.answers() == null || event.answers().isEmpty()) {
            return;
        }

        Long userId = event.userId();
        OffsetDateTime now = event.completedAt() != null ? event.completedAt() : OffsetDateTime.now();

        for (GradedAnswersEvent.GradedAnswerItem item : event.answers()) {
            Long questionId = item.questionId();
            Optional<MistakeRecord> opt = mistakeRecordRepository.findByUserIdAndQuestionId(userId, questionId);

            if (!item.isCorrect()) {
                // Incorrect answer
                if (opt.isEmpty()) {
                    MistakeRecord record = new MistakeRecord(userId, questionId);
                    record.setStatus(MistakeStatus.ACTIVE);
                    record.setCorrectStreak(0);
                    record.setLastAnsweredAt(now);
                    mistakeRecordRepository.save(record);
                } else {
                    MistakeRecord record = opt.get();
                    record.setStatus(MistakeStatus.ACTIVE);
                    record.setCorrectStreak(0);
                    record.setMasteredAt(null);
                    record.setLastAnsweredAt(now);
                    mistakeRecordRepository.save(record);
                }
            } else {
                // Correct answer
                if (opt.isPresent()) {
                    MistakeRecord record = opt.get();
                    if (record.getStatus() == MistakeStatus.ACTIVE) {
                        int newStreak = record.getCorrectStreak() + 1;
                        record.setCorrectStreak(newStreak);
                        record.setLastAnsweredAt(now);
                        if (newStreak >= MASTERY_STREAK) {
                            record.setStatus(MistakeStatus.MASTERED);
                            record.setMasteredAt(now);
                        }
                        mistakeRecordRepository.save(record);
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<MistakeItemResponse> getMistakes(
            Long userId,
            String statusStr,
            Long subjectId,
            Long chapterId,
            Pageable pageable
    ) {
        MistakeStatus filterStatus = null;
        if (statusStr != null && !statusStr.equalsIgnoreCase("ALL")) {
            try {
                filterStatus = MistakeStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        } else if (statusStr == null) {
            filterStatus = MistakeStatus.ACTIVE;
        }

        List<MistakeRecord> allRecords = mistakeRecordRepository.findByUserIdOrderByLastAnsweredAtDesc(userId);
        if (allRecords.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Set<Long> qIds = allRecords.stream().map(MistakeRecord::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionRepository.findByQuestionIdIn(qIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Set<Long> quizIds = questionMap.values().stream().map(Question::getQuizId).collect(Collectors.toSet());
        Map<Long, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getQuizId, qz -> qz));

        Set<Long> topicIds = questionMap.values().stream().map(Question::getTopicId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> topicNames = chapterTopicRepository.findByTopicIdIn(topicIds).stream()
                .collect(Collectors.toMap(ChapterTopic::getTopicId, ChapterTopic::getName));

        Set<Long> chapterIdsInSubject = null;
        if (subjectId != null) {
            chapterIdsInSubject = new HashSet<>(chapterValidationService.getActiveChapterIdsBySubject(subjectId));
        }

        List<MistakeItemResponse> matched = new ArrayList<>();
        for (MistakeRecord record : allRecords) {
            if (filterStatus != null && record.getStatus() != filterStatus) {
                continue;
            }

            Question q = questionMap.get(record.getQuestionId());
            if (q == null) continue;

            // Exclude ARCHIVED questions from ACTIVE lists (FR-MIST-10)
            if (record.getStatus() == MistakeStatus.ACTIVE && q.getStatus() == QuestionStatus.ARCHIVED) {
                continue;
            }

            Quiz qz = quizMap.get(q.getQuizId());
            if (qz == null) continue;

            Long qChapterId = qz.getChapterId();
            if (chapterId != null && !chapterId.equals(qChapterId)) {
                continue;
            }
            if (chapterIdsInSubject != null && !chapterIdsInSubject.contains(qChapterId)) {
                continue;
            }

            String chapterName = "Chapter " + qChapterId;
            Long qSubjectId = null;
            String subjectName = null;
            var optSummary = chapterValidationService.getChapterSummary(qChapterId);
            if (optSummary.isPresent()) {
                chapterName = optSummary.get().name();
                qSubjectId = optSummary.get().subjectId();
                if (qSubjectId != null) {
                    var optSubject = subjectLookupService.getSubjectInfo(qSubjectId);
                    if (optSubject.isPresent()) {
                        subjectName = optSubject.get().name();
                    }
                }
            }

            String topicName = q.getTopicId() != null ? topicNames.get(q.getTopicId()) : null;

            matched.add(new MistakeItemResponse(
                    q.getQuestionId(),
                    q.getQuestionText(),
                    qChapterId,
                    chapterName,
                    qSubjectId,
                    subjectName,
                    topicName,
                    record.getStatus(),
                    record.getCorrectStreak(),
                    record.getLastAnsweredAt()
            ));
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), matched.size());
        List<MistakeItemResponse> pageContent = (start <= end) ? matched.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, matched.size());
    }

    @Transactional(readOnly = true)
    public MistakeSummaryResponse getMistakesSummary(Long userId) {
        List<MistakeRecord> records = mistakeRecordRepository.findByUserId(userId);
        if (records.isEmpty()) {
            return new MistakeSummaryResponse(0, 0, 0, List.of());
        }

        Set<Long> qIds = records.stream().map(MistakeRecord::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionRepository.findByQuestionIdIn(qIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Set<Long> quizIds = questionMap.values().stream().map(Question::getQuizId).collect(Collectors.toSet());
        Map<Long, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getQuizId, qz -> qz));

        Set<Long> activeEligibleIds = new HashSet<>(questionEligibility.filterEligible(qIds));

        long totalActive = 0;
        long totalPracticeable = 0;
        long totalMastered = 0;

        // Group by chapter
        Map<Long, long[]> chapterCounts = new LinkedHashMap<>(); // chapterId -> [activeCount, practiceableCount]

        for (MistakeRecord record : records) {
            Question q = questionMap.get(record.getQuestionId());
            if (q == null) continue;

            Quiz qz = quizMap.get(q.getQuizId());
            Long chapterId = (qz != null) ? qz.getChapterId() : null;

            if (record.getStatus() == MistakeStatus.MASTERED) {
                totalMastered++;
            } else if (record.getStatus() == MistakeStatus.ACTIVE) {
                if (q.getStatus() != QuestionStatus.ARCHIVED) {
                    totalActive++;
                    boolean isPracticeable = activeEligibleIds.contains(q.getQuestionId());
                    if (isPracticeable) {
                        totalPracticeable++;
                    }

                    if (chapterId != null) {
                        long[] counts = chapterCounts.computeIfAbsent(chapterId, k -> new long[2]);
                        counts[0]++; // active
                        if (isPracticeable) {
                            counts[1]++; // practiceable
                        }
                    }
                }
            }
        }

        List<ChapterMistakeSummary> chapterSummaries = new ArrayList<>();
        for (Map.Entry<Long, long[]> entry : chapterCounts.entrySet()) {
            Long chapterId = entry.getKey();
            long[] counts = entry.getValue();

            String chapterName = "Chapter " + chapterId;
            Long subjectId = null;
            String subjectName = null;
            var optSummary = chapterValidationService.getChapterSummary(chapterId);
            if (optSummary.isPresent()) {
                chapterName = optSummary.get().name();
                subjectId = optSummary.get().subjectId();
                if (subjectId != null) {
                    var optSubject = subjectLookupService.getSubjectInfo(subjectId);
                    if (optSubject.isPresent()) {
                        subjectName = optSubject.get().name();
                    }
                }
            }

            chapterSummaries.add(new ChapterMistakeSummary(
                    chapterId,
                    chapterName,
                    subjectId,
                    subjectName,
                    counts[0],
                    counts[1]
            ));
        }

        return new MistakeSummaryResponse(totalActive, totalPracticeable, totalMastered, chapterSummaries);
    }

    public AttemptDetailResponse createMistakePractice(Long userId, MistakePracticeRequest request) {
        List<MistakeRecord> activeMistakes = mistakeRecordRepository.findByUserIdAndStatusOrderByLastAnsweredAtDesc(userId, MistakeStatus.ACTIVE);
        if (activeMistakes.isEmpty()) {
            throw new LearningConflictException("NO_MISTAKES_TO_PRACTICE", "No active mistakes found to practice");
        }

        Set<Long> qIds = activeMistakes.stream().map(MistakeRecord::getQuestionId).collect(Collectors.toSet());
        List<Question> questions = questionRepository.findByQuestionIdIn(qIds);

        Set<Long> quizIds = questions.stream().map(Question::getQuizId).collect(Collectors.toSet());
        Map<Long, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getQuizId, qz -> qz));

        Set<Long> chapterIdsInSubject = null;
        if (request != null && request.subjectId() != null) {
            chapterIdsInSubject = new HashSet<>(chapterValidationService.getActiveChapterIdsBySubject(request.subjectId()));
        }

        List<Question> candidates = new ArrayList<>();
        for (Question q : questions) {
            if (q.getStatus() == QuestionStatus.ARCHIVED) {
                continue;
            }

            Quiz qz = quizMap.get(q.getQuizId());
            if (qz == null) continue;
            Long qChapterId = qz.getChapterId();

            if (request != null && request.chapterId() != null && !request.chapterId().equals(qChapterId)) {
                continue;
            }
            if (chapterIdsInSubject != null && !chapterIdsInSubject.contains(qChapterId)) {
                continue;
            }

            if (request != null && request.language() != null && !request.language().isBlank()) {
                if (!request.language().trim().equalsIgnoreCase(q.getLanguage())) {
                    continue;
                }
            }

            candidates.add(q);
        }

        // Filter for eligible questions
        List<Long> candidateIds = candidates.stream().map(Question::getQuestionId).toList();
        List<Long> eligibleIds = questionEligibility.filterEligible(candidateIds);
        Set<Long> eligibleSet = new HashSet<>(eligibleIds);

        List<Question> practiceable = candidates.stream()
                .filter(q -> eligibleSet.contains(q.getQuestionId()))
                .toList();

        if (practiceable.isEmpty()) {
            throw new LearningConflictException("NO_MISTAKES_TO_PRACTICE", "No eligible mistakes found to practice");
        }

        int requestedCount = (request != null && request.count() != null && request.count() > 0)
                ? request.count()
                : learningProperties.getAttemptQuestionCount();

        int targetCount = Math.min(requestedCount, practiceable.size());

        List<Question> shuffled = new ArrayList<>(practiceable);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = randomGenerator.nextInt(i + 1);
            Question temp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, temp);
        }

        List<Question> selectedQuestions = shuffled.subList(0, targetCount);

        // Create attempt row
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(userId);
        attempt.setAttemptType(QuizAttemptType.MISTAKE_PRACTICE);
        attempt.setQuizId(null);
        attempt.setTotalMarks(selectedQuestions.size());
        attempt.setStartedAt(OffsetDateTime.now());
        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        // Create quiz_answers rows
        int order = 1;
        List<StudentQuestionDto> studentQuestions = new ArrayList<>();
        Set<Long> topicIds = selectedQuestions.stream().map(Question::getTopicId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> topicNames = chapterTopicRepository.findByTopicIdIn(topicIds).stream()
                .collect(Collectors.toMap(ChapterTopic::getTopicId, ChapterTopic::getName));

        for (Question q : selectedQuestions) {
            QuizAnswer qa = new QuizAnswer();
            qa.setAttemptId(savedAttempt.getAttemptId());
            qa.setQuestionId(q.getQuestionId());
            qa.setQuestionOrder(order);
            qa.setSelectedOptionId(null);
            qa.setIsCorrect(null);
            quizAnswerRepository.save(qa);

            Quiz qz = quizMap.get(q.getQuizId());
            Long qChapterId = qz != null ? qz.getChapterId() : null;
            String chapterName = qChapterId != null
                    ? chapterValidationService.getChapterSummary(qChapterId).map(ChapterValidationService.ChapterSummary::name).orElse("Chapter " + qChapterId)
                    : null;

            List<StudentOptionDto> optionDtos = q.getOptions().stream()
                    .map(o -> new StudentOptionDto(o.getOptionId(), o.getOptionText(), o.getOptionOrder()))
                    .toList();

            studentQuestions.add(new StudentQuestionDto(
                    q.getQuestionId(),
                    order,
                    q.getQuestionText(),
                    q.getDifficulty(),
                    q.getTopicId(),
                    q.getTopicId() != null ? topicNames.get(q.getTopicId()) : null,
                    qChapterId,
                    chapterName,
                    optionDtos
            ));
            order++;
        }

        return new AttemptDetailResponse(
                savedAttempt.getAttemptId(),
                savedAttempt.getAttemptType(),
                null,
                null,
                "IN_PROGRESS",
                (request != null && request.language() != null) ? request.language().toUpperCase() : "EN",
                null,
                savedAttempt.getTotalMarks(),
                null,
                savedAttempt.getStartedAt(),
                null,
                false,
                null,
                studentQuestions
        );
    }
}

