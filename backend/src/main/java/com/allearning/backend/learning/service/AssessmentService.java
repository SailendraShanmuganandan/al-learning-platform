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
import com.allearning.backend.security.EmailVerificationGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssessmentService {

    private final DiagnosticAssessmentRepository diagnosticAssessmentRepository;
    private final DiagnosticAssessmentAnswerRepository diagnosticAssessmentAnswerRepository;
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final ChapterTopicRepository chapterTopicRepository;
    private final QuestionEligibility questionEligibility;
    private final QuestionSamplingService questionSamplingService;
    private final AnswerGrader answerGrader;
    private final ChapterValidationService chapterValidationService;
    private final SubjectLookupService subjectLookupService;
    private final LearningProperties learningProperties;
    private final EmailVerificationGuard emailVerificationGuard;
    private final LearningUserLookupService userLookupService;
    private final ApplicationEventPublisher eventPublisher;
    private final QuestionReportRepository questionReportRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssessmentService(
            DiagnosticAssessmentRepository diagnosticAssessmentRepository,
            DiagnosticAssessmentAnswerRepository diagnosticAssessmentAnswerRepository,
            QuestionRepository questionRepository,
            QuizRepository quizRepository,
            ChapterTopicRepository chapterTopicRepository,
            QuestionEligibility questionEligibility,
            QuestionSamplingService questionSamplingService,
            AnswerGrader answerGrader,
            ChapterValidationService chapterValidationService,
            SubjectLookupService subjectLookupService,
            LearningProperties learningProperties,
            EmailVerificationGuard emailVerificationGuard,
            LearningUserLookupService userLookupService,
            ApplicationEventPublisher eventPublisher,
            QuestionReportRepository questionReportRepository
    ) {
        this.diagnosticAssessmentRepository = diagnosticAssessmentRepository;
        this.diagnosticAssessmentAnswerRepository = diagnosticAssessmentAnswerRepository;
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
        this.chapterTopicRepository = chapterTopicRepository;
        this.questionEligibility = questionEligibility;
        this.questionSamplingService = questionSamplingService;
        this.answerGrader = answerGrader;
        this.chapterValidationService = chapterValidationService;
        this.subjectLookupService = subjectLookupService;
        this.learningProperties = learningProperties;
        this.emailVerificationGuard = emailVerificationGuard;
        this.userLookupService = userLookupService;
        this.eventPublisher = eventPublisher;
        this.questionReportRepository = questionReportRepository;
    }

    public AssessmentDetailResponse createAssessment(Long userId, AssessmentCreateRequest request) {
        // Enforce verified email -> 403 EMAIL_NOT_VERIFIED
        emailVerificationGuard.checkCurrentAuthenticationVerified();

        if (request.assessmentType() == AssessmentType.OVERALL_QUIZ && request.scopeType() != AssessmentScopeType.ALL_SUBJECTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OVERALL_QUIZ must use ALL_SUBJECTS scope");
        }

        List<Long> targetChapterIds = switch (request.scopeType()) {
            case CHAPTERS -> {
                if (request.chapterIds() == null || request.chapterIds().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chapterIds cannot be empty for CHAPTERS scope");
                }
                for (Long cid : request.chapterIds()) {
                    if (!chapterValidationService.isChapterActive(cid)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chapter " + cid + " is not active or does not exist");
                    }
                }
                yield request.chapterIds();
            }
            case SUBJECT -> {
                if (request.subjectId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subjectId cannot be null for SUBJECT scope");
                }
                List<Long> cids = chapterValidationService.getActiveChapterIdsBySubject(request.subjectId());
                if (cids.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active chapters found for subject " + request.subjectId());
                }
                yield cids;
            }
            case ALL_SUBJECTS -> {
                List<Long> cids = chapterValidationService.getActiveChapterIds();
                if (cids.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active chapters found in system");
                }
                yield cids;
            }
        };

        // Sample questions
        List<Question> sampled = questionSamplingService.sampleForAssessment(
                userId,
                targetChapterIds,
                request.language(),
                learningProperties.getAssessmentQuestionsPerChapter(),
                learningProperties.getAssessmentMaxQuestions()
        );

        String scopeRef;
        try {
            scopeRef = objectMapper.writeValueAsString(request.scopeType() == AssessmentScopeType.SUBJECT ? request.subjectId() : targetChapterIds);
        } catch (JsonProcessingException e) {
            scopeRef = "[]";
        }

        DiagnosticAssessment assessment = new DiagnosticAssessment();
        assessment.setUserId(userId);
        assessment.setAssessmentType(request.assessmentType());
        assessment.setScopeType(request.scopeType());
        assessment.setScopeReference(scopeRef);
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        assessment.setTotalMarks(sampled.size());
        assessment.setStartedAt(OffsetDateTime.now());
        DiagnosticAssessment saved = diagnosticAssessmentRepository.save(assessment);

        int order = 1;
        for (Question q : sampled) {
            DiagnosticAssessmentAnswer daa = new DiagnosticAssessmentAnswer();
            daa.setAssessmentId(saved.getAssessmentId());
            daa.setQuestionId(q.getQuestionId());
            daa.setQuestionOrder(order++);
            daa.setSelectedOptionId(null);
            daa.setIsCorrect(null);
            diagnosticAssessmentAnswerRepository.save(daa);
        }

        return buildAssessmentDetail(saved);
    }

    @Transactional(readOnly = true)
    public AssessmentDetailResponse getAssessmentDetail(Long userId, Long assessmentId) {
        DiagnosticAssessment assessment = diagnosticAssessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found: " + assessmentId));

        if (!assessment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found: " + assessmentId);
        }

        return buildAssessmentDetail(assessment);
    }

    public AssessmentDetailResponse submitAssessment(Long userId, Long assessmentId, SubmitAnswersRequest request) {
        DiagnosticAssessment assessment = diagnosticAssessmentRepository.findByIdWithLock(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found: " + assessmentId));

        if (!assessment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found: " + assessmentId);
        }

        if (assessment.getStatus() == AssessmentStatus.COMPLETED || assessment.getCompletedAt() != null) {
            throw new LearningConflictException("ASSESSMENT_ALREADY_SUBMITTED", "This assessment has already been submitted");
        }

        List<DiagnosticAssessmentAnswer> answerRows = diagnosticAssessmentAnswerRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessmentId);
        Set<Long> qIds = answerRows.stream().map(DiagnosticAssessmentAnswer::getQuestionId).collect(Collectors.toSet());
        List<Question> questions = questionRepository.findByQuestionIdIn(qIds);
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Set<Long> quizIds = questions.stream().map(Question::getQuizId).collect(Collectors.toSet());
        Map<Long, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getQuizId, qz -> qz));

        List<AnswerGrader.ServedQuestionForGrading> servedForGrading = new ArrayList<>();
        for (DiagnosticAssessmentAnswer row : answerRows) {
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

        List<GradedAnswersEvent.GradedAnswerItem> eventItems = new ArrayList<>();
        for (DiagnosticAssessmentAnswer row : answerRows) {
            AnswerGrader.GradedAnswerDetail detail = graded.answersByQuestionId().get(row.getQuestionId());
            if (detail != null) {
                row.setSelectedOptionId(detail.selectedOptionId());
                row.setIsCorrect(detail.isCorrect());
                diagnosticAssessmentAnswerRepository.save(row);

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

        assessment.setStatus(AssessmentStatus.COMPLETED);
        assessment.setScore(graded.score());
        assessment.setCompletedAt(OffsetDateTime.now());
        DiagnosticAssessment saved = diagnosticAssessmentRepository.save(assessment);

        GradedSourceType sourceType = (saved.getAssessmentType() == AssessmentType.INITIAL_DIAGNOSTIC)
                ? GradedSourceType.INITIAL_DIAGNOSTIC
                : GradedSourceType.OVERALL_QUIZ;

        eventPublisher.publishEvent(new GradedAnswersEvent(
                userId,
                sourceType,
                saved.getAssessmentId(),
                null,
                saved.getCompletedAt(),
                eventItems
        ));

        return buildAssessmentDetail(saved);
    }

    @Transactional(readOnly = true)
    public Page<AssessmentDetailResponse> getMyAssessments(
            Long userId,
            AssessmentType type,
            AssessmentStatus status,
            Pageable pageable
    ) {
        Specification<DiagnosticAssessment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            if (type != null) {
                predicates.add(cb.equal(root.get("assessmentType"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DiagnosticAssessment> page = diagnosticAssessmentRepository.findAll(spec, pageable);
        List<AssessmentDetailResponse> items = page.getContent().stream()
                .map(this::buildAssessmentDetail)
                .toList();

        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatus(Long userId) {
        boolean verified = userLookupService.isEmailVerified(userId);
        if (!verified) {
            return new OnboardingStatusResponse(false, "EMAIL_NOT_VERIFIED");
        }

        boolean hasCompleted = diagnosticAssessmentRepository.existsByUserIdAndStatus(userId, AssessmentStatus.COMPLETED)
                || diagnosticAssessmentRepository.existsByUserIdAndCompletedAtIsNotNull(userId);
        if (hasCompleted) {
            return new OnboardingStatusResponse(false, "ALREADY_COMPLETED");
        }

        List<Long> activeChapters = chapterValidationService.getActiveChapterIds();
        boolean hasParticipating = false;
        for (Long cid : activeChapters) {
            if (questionEligibility.countEligible(cid, null) >= learningProperties.getMinEligibleQuestions()) {
                hasParticipating = true;
                break;
            }
        }

        if (!hasParticipating) {
            return new OnboardingStatusResponse(false, "NO_PARTICIPATING_CHAPTERS");
        }

        return new OnboardingStatusResponse(true, "ELIGIBLE");
    }

    @Transactional(readOnly = true)
    public List<SubjectAvailableChaptersDto> getAvailableChapters() {
        List<Long> activeSubjectIds = subjectLookupService.getActiveSubjectIds();
        List<SubjectAvailableChaptersDto> result = new ArrayList<>();

        for (Long subjectId : activeSubjectIds) {
            var optSubject = subjectLookupService.getSubjectInfo(subjectId);
            String subjectName = optSubject.map(SubjectLookupService.SubjectBasicInfo::name).orElse("Subject " + subjectId);

            List<Long> activeChapters = chapterValidationService.getActiveChapterIdsBySubject(subjectId);
            List<AvailableChapterDto> chapterDtos = new ArrayList<>();

            for (Long cid : activeChapters) {
                long eligibleCount = questionEligibility.countEligible(cid, null);
                if (eligibleCount >= learningProperties.getMinEligibleQuestions()) {
                    var sum = chapterValidationService.getChapterSummary(cid);
                    String name = sum.map(ChapterValidationService.ChapterSummary::name).orElse("Chapter " + cid);
                    chapterDtos.add(new AvailableChapterDto(cid, name, null, eligibleCount));
                }
            }

            if (!chapterDtos.isEmpty()) {
                result.add(new SubjectAvailableChaptersDto(subjectId, subjectName, chapterDtos));
            }
        }

        return result;
    }

    private AssessmentDetailResponse buildAssessmentDetail(DiagnosticAssessment assessment) {
        boolean completed = assessment.getStatus() == AssessmentStatus.COMPLETED || assessment.getCompletedAt() != null;
        String statusStr = completed ? "COMPLETED" : "IN_PROGRESS";

        List<DiagnosticAssessmentAnswer> answers = diagnosticAssessmentAnswerRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessment.getAssessmentId());
        Set<Long> qIds = answers.stream().map(DiagnosticAssessmentAnswer::getQuestionId).collect(Collectors.toSet());
        List<Question> questions = questionRepository.findByQuestionIdIn(qIds);
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Set<Long> quizIds = questions.stream().map(Question::getQuizId).collect(Collectors.toSet());
        Map<Long, Quiz> quizMap = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getQuizId, qz -> qz));

        Set<Long> topicIds = questions.stream().map(Question::getTopicId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> topicNames = chapterTopicRepository.findByTopicIdIn(topicIds).stream()
                .collect(Collectors.toMap(ChapterTopic::getTopicId, ChapterTopic::getName));

        if (!completed) {
            List<StudentQuestionDto> studentQuestions = new ArrayList<>();
            for (DiagnosticAssessmentAnswer ans : answers) {
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

            return new AssessmentDetailResponse(
                    assessment.getAssessmentId(),
                    assessment.getAssessmentType(),
                    assessment.getScopeType(),
                    statusStr,
                    null,
                    assessment.getTotalMarks(),
                    null,
                    assessment.getStartedAt(),
                    null,
                    null,
                    studentQuestions
            );
        } else {
            List<ResultQuestionDto> resultQuestions = new ArrayList<>();
            Map<Long, int[]> chapterBreakdownCounts = new LinkedHashMap<>();

            Set<Long> reportedQuestionIds = (questionReportRepository != null && !qIds.isEmpty())
                    ? questionReportRepository.findReportedQuestionIdsByUserIdAndQuestionIds(assessment.getUserId(), qIds)
                    : Collections.emptySet();

            for (DiagnosticAssessmentAnswer ans : answers) {
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
                    c[1]++;
                    if (Boolean.TRUE.equals(ans.getIsCorrect())) {
                        c[0]++;
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

            int score = assessment.getScore() != null ? assessment.getScore() : 0;
            double pct = assessment.getTotalMarks() > 0 ? (Math.round((score * 100.0 / assessment.getTotalMarks()) * 10.0) / 10.0) : 0.0;

            return new AssessmentDetailResponse(
                    assessment.getAssessmentId(),
                    assessment.getAssessmentType(),
                    assessment.getScopeType(),
                    statusStr,
                    score,
                    assessment.getTotalMarks(),
                    pct,
                    assessment.getStartedAt(),
                    assessment.getCompletedAt(),
                    chapterBreakdowns,
                    resultQuestions
            );
        }
    }
}
