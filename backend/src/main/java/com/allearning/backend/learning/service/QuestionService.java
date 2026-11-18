package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.entity.*;
import com.allearning.backend.learning.event.QuestionContentChangedEvent;
import com.allearning.backend.learning.exception.LearningConflictException;
import com.allearning.backend.learning.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final ChapterTopicRepository chapterTopicRepository;
    private final QuizRepository quizRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final DiagnosticAssessmentAnswerRepository diagnosticAssessmentAnswerRepository;
    private final QuestionReportRepository questionReportRepository;
    private final ChapterDocumentRepository chapterDocumentRepository;
    private final QuizService quizService;
    private final ChapterValidationService chapterValidationService;
    private final SubjectLookupService subjectLookupService;
    private final LearningUserLookupService userLookupService;
    private final ApplicationEventPublisher eventPublisher;
    private final LearningProperties learningProperties;

    public QuestionService(
            QuestionRepository questionRepository,
            QuestionOptionRepository questionOptionRepository,
            ChapterTopicRepository chapterTopicRepository,
            QuizRepository quizRepository,
            QuizAnswerRepository quizAnswerRepository,
            DiagnosticAssessmentAnswerRepository diagnosticAssessmentAnswerRepository,
            QuestionReportRepository questionReportRepository,
            ChapterDocumentRepository chapterDocumentRepository,
            QuizService quizService,
            ChapterValidationService chapterValidationService,
            SubjectLookupService subjectLookupService,
            LearningUserLookupService userLookupService,
            ApplicationEventPublisher eventPublisher,
            LearningProperties learningProperties
    ) {
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.chapterTopicRepository = chapterTopicRepository;
        this.quizRepository = quizRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.diagnosticAssessmentAnswerRepository = diagnosticAssessmentAnswerRepository;
        this.questionReportRepository = questionReportRepository;
        this.chapterDocumentRepository = chapterDocumentRepository;
        this.quizService = quizService;
        this.chapterValidationService = chapterValidationService;
        this.subjectLookupService = subjectLookupService;
        this.userLookupService = userLookupService;
        this.eventPublisher = eventPublisher;
        this.learningProperties = learningProperties;
    }

    public QuestionDetailResponse createQuestion(Long chapterId, QuestionCreateRequest request, Long adminUserId) {
        verifyChapterExists(chapterId);
        Quiz quiz = quizService.getOrCreateDraftQuiz(chapterId);

        if (request.topicId() != null) {
            verifyTopicBelongsToChapter(request.topicId(), chapterId);
        }

        validateOptionsBasic(request.options());

        Question question = new Question();
        question.setQuizId(quiz.getQuizId());
        question.setQuestionText(request.questionText().trim());
        question.setExplanation(request.explanation());
        question.setTopicId(request.topicId());
        question.setDifficulty(request.difficulty());
        question.setLanguage((request.language() != null && !request.language().isBlank())
                ? request.language().trim().toUpperCase() : "EN");
        question.setSource(QuestionSource.MANUAL);
        question.setCreatedBy(adminUserId);

        boolean approveNow = request.saveMode() != null && request.saveMode().equalsIgnoreCase("APPROVED");
        if (approveNow) {
            validateApprovalPreconditions(question, request.options(), chapterId);
            question.setStatus(QuestionStatus.APPROVED);
            question.setApprovedBy(adminUserId);
            question.setApprovedAt(OffsetDateTime.now());
        } else {
            question.setStatus(QuestionStatus.PENDING);
        }

        int order = 1;
        for (QuestionOptionRequest optReq : request.options()) {
            QuestionOption opt = new QuestionOption(
                    optReq.optionText().trim(),
                    optReq.optionOrder() > 0 ? optReq.optionOrder() : order++,
                    optReq.isCorrect()
            );
            question.addOption(opt);
        }

        Question saved = questionRepository.save(question);
        return getQuestionDetail(saved.getQuestionId());
    }

    @Transactional(readOnly = true)
    public Page<QuestionSummaryResponse> getQuestions(
            Long chapterId,
            Long quizId,
            Long topicId,
            QuestionStatus status,
            QuestionDifficulty difficulty,
            String language,
            QuestionSource source,
            String search,
            Boolean reported,
            Boolean lowCorrectRate,
            Boolean sourceDocumentRemoved,
            Long sourceDocumentId,
            Long generationJobId,
            Pageable pageable
    ) {
        return getQuestions(chapterId, null, quizId, topicId, status, difficulty, language, source, search,
                reported, lowCorrectRate, sourceDocumentRemoved, sourceDocumentId, generationJobId, pageable);
    }

    public Page<QuestionSummaryResponse> getQuestions(
            Long chapterId,
            Long subjectId,
            Long quizId,
            Long topicId,
            QuestionStatus status,
            QuestionDifficulty difficulty,
            String language,
            QuestionSource source,
            String search,
            Boolean reported,
            Boolean lowCorrectRate,
            Boolean sourceDocumentRemoved,
            Long sourceDocumentId,
            Long generationJobId,
            Pageable pageable
    ) {
        Set<Long> targetQuizIds = null;
        if (chapterId != null) {
            verifyChapterExists(chapterId);
            Long effectiveQuizId = quizId;
            if (effectiveQuizId == null) {
                Optional<Quiz> optQuiz = quizRepository.findByChapterId(chapterId);
                if (optQuiz.isEmpty()) {
                    return new PageImpl<>(List.of(), pageable, 0);
                }
                effectiveQuizId = optQuiz.get().getQuizId();
            }
            targetQuizIds = Set.of(effectiveQuizId);
        } else if (quizId != null) {
            targetQuizIds = Set.of(quizId);
        } else if (subjectId != null) {
            List<Long> chapterIds = chapterValidationService.getChapterIdsForSubject(subjectId);
            if (chapterIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            List<Quiz> quizzes = quizRepository.findByChapterIdIn(chapterIds);
            if (quizzes.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            targetQuizIds = quizzes.stream().map(Quiz::getQuizId).collect(Collectors.toSet());
        }

        final Set<Long> finalTargetQuizIds = targetQuizIds;
        final String lang = (language != null && !language.isBlank()) ? language.trim().toUpperCase() : null;
        final String searchPattern = (search != null && !search.isBlank()) ? "%" + search.trim().toLowerCase() + "%" : null;

        // Pre-compute low correct rate question IDs if filter requested
        Set<Long> lowCorrectRateQuestionIds = null;
        if (lowCorrectRate != null) {
            Specification<Question> lowRateSpec = (r, q, cb) -> {
                if (finalTargetQuizIds == null) {
                    return cb.conjunction();
                }
                if (finalTargetQuizIds.size() == 1) {
                    return cb.equal(r.get("quizId"), finalTargetQuizIds.iterator().next());
                }
                return r.get("quizId").in(finalTargetQuizIds);
            };
            List<Question> candidateQuestions = questionRepository.findAll(lowRateSpec);
            List<Long> allQIds = candidateQuestions.stream().map(Question::getQuestionId).toList();
            Map<Long, QuestionAnswerStats> allStats = computeAnswerStats(allQIds);

            lowCorrectRateQuestionIds = new HashSet<>();
            int minAns = learningProperties.getLowCorrectRateMinAnswers();
            int maxPct = learningProperties.getLowCorrectRatePercent();

            for (Map.Entry<Long, QuestionAnswerStats> entry : allStats.entrySet()) {
                QuestionAnswerStats st = entry.getValue();
                if (st.totalAnswers() >= minAns) {
                    double rate = ((double) st.correctAnswers() / st.totalAnswers()) * 100.0;
                    if (rate <= maxPct) {
                        lowCorrectRateQuestionIds.add(entry.getKey());
                    }
                }
            }
        }
        final Set<Long> finalLowRateIds = lowCorrectRateQuestionIds;

        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (finalTargetQuizIds != null) {
                if (finalTargetQuizIds.size() == 1) {
                    predicates.add(cb.equal(root.get("quizId"), finalTargetQuizIds.iterator().next()));
                } else {
                    predicates.add(root.get("quizId").in(finalTargetQuizIds));
                }
            }

            if (topicId != null) {
                predicates.add(cb.equal(root.get("topicId"), topicId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }
            if (lang != null) {
                predicates.add(cb.equal(cb.upper(root.get("language")), lang));
            }
            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (sourceDocumentId != null) {
                predicates.add(cb.equal(root.get("sourceDocumentId"), sourceDocumentId));
            }
            if (generationJobId != null) {
                predicates.add(cb.equal(root.get("generationJobId"), generationJobId));
            }
            if (searchPattern != null) {
                Predicate textMatch = cb.like(cb.lower(root.get("questionText")), searchPattern);
                Predicate explMatch = cb.like(cb.lower(root.get("explanation")), searchPattern);
                predicates.add(cb.or(textMatch, explMatch));
            }
            if (reported != null) {
                var reportSubquery = query.subquery(Long.class);
                var reportRoot = reportSubquery.from(QuestionReport.class);
                reportSubquery.select(reportRoot.get("questionId"))
                        .where(cb.equal(reportRoot.get("status"), QuestionReportStatus.OPEN));
                if (reported) {
                    predicates.add(root.get("questionId").in(reportSubquery));
                } else {
                    predicates.add(cb.not(root.get("questionId").in(reportSubquery)));
                }
            }
            if (sourceDocumentRemoved != null) {
                var docSubquery = query.subquery(Long.class);
                var docRoot = docSubquery.from(ChapterDocument.class);
                docSubquery.select(docRoot.get("documentId"));
                if (sourceDocumentRemoved) {
                    predicates.add(cb.and(
                            root.get("sourceDocumentId").isNotNull(),
                            cb.not(root.get("sourceDocumentId").in(docSubquery))
                    ));
                } else {
                    predicates.add(cb.or(
                            root.get("sourceDocumentId").isNull(),
                            root.get("sourceDocumentId").in(docSubquery)
                    ));
                }
            }
            if (lowCorrectRate != null) {
                if (lowCorrectRate) {
                    if (finalLowRateIds == null || finalLowRateIds.isEmpty()) {
                        predicates.add(cb.disjunction());
                    } else {
                        predicates.add(root.get("questionId").in(finalLowRateIds));
                    }
                } else {
                    if (finalLowRateIds != null && !finalLowRateIds.isEmpty()) {
                        predicates.add(cb.not(root.get("questionId").in(finalLowRateIds)));
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Question> page = questionRepository.findAll(spec, pageable);
        List<Question> questions = page.getContent();
        if (questions.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        List<Long> questionIds = questions.stream().map(Question::getQuestionId).toList();
        Map<Long, QuestionAnswerStats> statsMap = computeAnswerStats(questionIds);
        Map<Long, Long> openReportsMap = computeOpenReportCounts(questionIds);

        Set<Long> topicIds = questions.stream().map(Question::getTopicId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> topicNames = chapterTopicRepository.findByTopicIdIn(topicIds).stream()
                .collect(Collectors.toMap(ChapterTopic::getTopicId, ChapterTopic::getName));

        Set<Long> docIds = questions.stream().map(Question::getSourceDocumentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> existingDocIds = docIds.isEmpty() ? Set.of() : new HashSet<>(chapterDocumentRepository.findAllById(docIds).stream()
                .map(ChapterDocument::getDocumentId).toList());

        Set<Long> pageQuizIds = questions.stream().map(Question::getQuizId).collect(Collectors.toSet());
        Map<Long, Quiz> quizMap = quizRepository.findAllById(pageQuizIds).stream()
                .collect(Collectors.toMap(Quiz::getQuizId, q -> q));
        Set<Long> chapterIds = quizMap.values().stream().map(Quiz::getChapterId).collect(Collectors.toSet());
        Map<Long, ChapterValidationService.ChapterSummary> chapterSummaryMap = new HashMap<>();
        for (Long cid : chapterIds) {
            chapterValidationService.getChapterSummary(cid).ifPresent(cs -> chapterSummaryMap.put(cid, cs));
        }
        Set<Long> subjectIds = chapterSummaryMap.values().stream().map(ChapterValidationService.ChapterSummary::subjectId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SubjectLookupService.SubjectBasicInfo> subjectMap = subjectLookupService.getSubjectsInfo(subjectIds);

        List<QuestionSummaryResponse> summaries = questions.stream().map(q -> {
            QuestionAnswerStats stats = statsMap.get(q.getQuestionId());
            long answerCount = stats != null ? stats.totalAnswers() : 0L;
            Double correctRate = (stats != null && stats.totalAnswers() > 0)
                    ? Math.round(((double) stats.correctAnswers() / stats.totalAnswers() * 100.0) * 10.0) / 10.0
                    : null;
            long openReports = openReportsMap.getOrDefault(q.getQuestionId(), 0L);
            String topicName = q.getTopicId() != null ? topicNames.get(q.getTopicId()) : null;
            boolean docRemoved = q.getSourceDocumentId() != null && !existingDocIds.contains(q.getSourceDocumentId());

            Quiz quiz = quizMap.get(q.getQuizId());
            Long chId = quiz != null ? quiz.getChapterId() : null;
            ChapterValidationService.ChapterSummary cs = chId != null ? chapterSummaryMap.get(chId) : null;
            String chName = cs != null ? cs.name() : (chId != null ? "Chapter " + chId : null);
            Long subId = cs != null ? cs.subjectId() : null;
            SubjectLookupService.SubjectBasicInfo subInfo = subId != null ? subjectMap.get(subId) : null;
            String subName = subInfo != null ? subInfo.name() : null;

            return new QuestionSummaryResponse(
                    q.getQuestionId(),
                    q.getQuizId(),
                    chId,
                    chName,
                    subName,
                    q.getTopicId(),
                    topicName,
                    q.getQuestionText(),
                    q.getDifficulty(),
                    q.getLanguage(),
                    q.getStatus(),
                    q.getSource(),
                    q.getSourceDocumentId(),
                    docRemoved,
                    q.getOptions().size(),
                    answerCount,
                    correctRate,
                    openReports,
                    q.getCreatedAt(),
                    q.getUpdatedAt()
            );
        }).toList();

        return new PageImpl<>(summaries, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public QuestionDetailResponse getQuestionDetail(Long questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + questionId));

        Map<Long, QuestionAnswerStats> statsMap = computeAnswerStats(List.of(questionId));
        Map<Long, Long> openReportsMap = computeOpenReportCounts(List.of(questionId));

        QuestionAnswerStats stats = statsMap.get(questionId);
        long answerCount = stats != null ? stats.totalAnswers() : 0L;
        Double correctRate = (stats != null && stats.totalAnswers() > 0)
                ? Math.round(((double) stats.correctAnswers() / stats.totalAnswers() * 100.0) * 10.0) / 10.0
                : null;
        long openReports = openReportsMap.getOrDefault(questionId, 0L);

        String topicName = null;
        if (q.getTopicId() != null) {
            topicName = chapterTopicRepository.findById(q.getTopicId()).map(ChapterTopic::getName).orElse(null);
        }

        boolean docRemoved = q.getSourceDocumentId() != null && !chapterDocumentRepository.existsById(q.getSourceDocumentId());

        Set<Long> userIds = new HashSet<>();
        if (q.getCreatedBy() != null) userIds.add(q.getCreatedBy());
        if (q.getApprovedBy() != null) userIds.add(q.getApprovedBy());
        Map<Long, String> names = userLookupService.getUserDisplayNames(userIds);

        List<QuestionOptionResponse> options = q.getOptions().stream()
                .map(o -> new QuestionOptionResponse(o.getOptionId(), o.getOptionText(), o.getOptionOrder(), o.isCorrect()))
                .toList();

        return new QuestionDetailResponse(
                q.getQuestionId(),
                q.getQuizId(),
                q.getTopicId(),
                topicName,
                q.getQuestionText(),
                q.getExplanation(),
                q.getDifficulty(),
                q.getLanguage(),
                q.getStatus(),
                q.getSource(),
                q.getSourceDocumentId(),
                docRemoved,
                q.getSourceExcerpt(),
                q.getCreatedBy(),
                names.get(q.getCreatedBy()),
                q.getApprovedBy(),
                names.get(q.getApprovedBy()),
                q.getApprovedAt(),
                options.size(),
                answerCount,
                correctRate,
                openReports,
                q.getCreatedAt(),
                q.getUpdatedAt(),
                options
        );
    }

    public QuestionDetailResponse updateQuestion(Long questionId, QuestionUpdateRequest request, Long adminUserId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + questionId));

        Quiz quiz = quizRepository.findById(question.getQuizId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found for question"));

        if (request.topicId() != null) {
            verifyTopicBelongsToChapter(request.topicId(), quiz.getChapterId());
        }

        validateOptionsBasic(request.options());

        boolean hasRecordedAnswers = quizAnswerRepository.existsByQuestionId(questionId)
                || diagnosticAssessmentAnswerRepository.existsByQuestionId(questionId);

        List<QuestionOption> currentOptions = question.getOptions();

        if (hasRecordedAnswers) {
            // BR-38: Cannot add, delete, or change which option is marked correct
            if (request.options().size() != currentOptions.size()) {
                throw new LearningConflictException("OPTIONS_IMMUTABLE", "Cannot add or remove options after student answers have been recorded");
            }

            // Map existing options by order
            Map<Integer, QuestionOption> existingByOrder = currentOptions.stream()
                    .collect(Collectors.toMap(QuestionOption::getOptionOrder, o -> o));

            for (QuestionOptionRequest optReq : request.options()) {
                QuestionOption existing = existingByOrder.get(optReq.optionOrder());
                if (existing == null) {
                    throw new LearningConflictException("OPTIONS_IMMUTABLE", "Option orders cannot be modified after answers have been recorded");
                }
                if (existing.isCorrect() != optReq.isCorrect()) {
                    throw new LearningConflictException("OPTIONS_IMMUTABLE", "Cannot change which option is marked correct after student answers have been recorded");
                }
            }
        }

        boolean textChanged = !Objects.equals(question.getQuestionText(), request.questionText())
                || !Objects.equals(question.getExplanation(), request.explanation());

        // Update basic question fields
        question.setQuestionText(request.questionText().trim());
        question.setExplanation(request.explanation());
        question.setTopicId(request.topicId());
        question.setDifficulty(request.difficulty());
        if (request.language() != null && !request.language().isBlank()) {
            question.setLanguage(request.language().trim().toUpperCase());
        }

        // If options text changed
        boolean optionsChanged = false;
        if (hasRecordedAnswers) {
            Map<Integer, QuestionOption> existingByOrder = currentOptions.stream()
                    .collect(Collectors.toMap(QuestionOption::getOptionOrder, o -> o));
            for (QuestionOptionRequest optReq : request.options()) {
                QuestionOption existing = existingByOrder.get(optReq.optionOrder());
                if (!Objects.equals(existing.getOptionText(), optReq.optionText().trim())) {
                    existing.setOptionText(optReq.optionText().trim());
                    optionsChanged = true;
                }
            }
        } else {
            // Replace options completely
            question.getOptions().clear();
            int order = 1;
            for (QuestionOptionRequest optReq : request.options()) {
                QuestionOption opt = new QuestionOption(
                        optReq.optionText().trim(),
                        optReq.optionOrder() > 0 ? optReq.optionOrder() : order++,
                        optReq.isCorrect()
                );
                question.addOption(opt);

            }
            optionsChanged = true;
        }

        // If currently APPROVED, ensure preconditions still hold
        if (question.getStatus() == QuestionStatus.APPROVED) {
            validateApprovalPreconditions(question, request.options(), quiz.getChapterId());
        }

        Question saved = questionRepository.save(question);

        if (textChanged || optionsChanged) {
            eventPublisher.publishEvent(new QuestionContentChangedEvent(questionId));
        }

        return getQuestionDetail(saved.getQuestionId());
    }

    public QuestionDetailResponse updateQuestionStatus(Long questionId, QuestionStatus targetStatus, Long adminUserId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + questionId));

        QuestionStatus currentStatus = question.getStatus();
        if (currentStatus == targetStatus) {
            return getQuestionDetail(questionId);
        }

        // Validate allowed state transitions:
        // PENDING -> APPROVED, PENDING -> ARCHIVED
        // APPROVED -> ARCHIVED, APPROVED -> PENDING
        // ARCHIVED -> PENDING
        boolean validTransition =
                (currentStatus == QuestionStatus.PENDING
                        && (targetStatus == QuestionStatus.APPROVED || targetStatus == QuestionStatus.ARCHIVED))
                        || (currentStatus == QuestionStatus.APPROVED
                        && (targetStatus == QuestionStatus.ARCHIVED || targetStatus == QuestionStatus.PENDING))
                        || (currentStatus == QuestionStatus.ARCHIVED
                        && targetStatus == QuestionStatus.PENDING);

        if (!validTransition) {
            throw new LearningConflictException("INVALID_STATUS_TRANSITION",
                    "Invalid status transition from " + currentStatus + " to " + targetStatus);
        }

        Quiz quiz = quizRepository.findById(question.getQuizId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found for question"));

        if (targetStatus == QuestionStatus.APPROVED) {
            List<QuestionOptionRequest> optRequests = question.getOptions().stream()
                    .map(o -> new QuestionOptionRequest(o.getOptionId(), o.getOptionText(), o.getOptionOrder(), o.isCorrect()))
                    .toList();
            validateApprovalPreconditions(question, optRequests, quiz.getChapterId());

            question.setStatus(QuestionStatus.APPROVED);
            question.setApprovedBy(adminUserId);
            question.setApprovedAt(OffsetDateTime.now());

            // Resolve any open reports
            List<QuestionReport> openReports = questionReportRepository.findByQuestionIdAndStatus(questionId, QuestionReportStatus.OPEN);
            for (QuestionReport report : openReports) {
                report.setStatus(QuestionReportStatus.RESOLVED);
                report.setResolvedBy(adminUserId);
                report.setResolvedAt(OffsetDateTime.now());
            }
            questionReportRepository.saveAll(openReports);
        } else {
            // Un-approving or archiving clears approval metadata
            question.setStatus(targetStatus);
            question.setApprovedBy(null);
            question.setApprovedAt(null);
        }

        Question saved = questionRepository.save(question);
        return getQuestionDetail(saved.getQuestionId());
    }

    public BulkStatusResponse bulkUpdateStatus(BulkStatusRequest request, Long adminUserId) {
        List<Long> updated = new ArrayList<>();
        List<BulkStatusResponse.SkippedQuestion> skipped = new ArrayList<>();

        for (Long questionId : request.questionIds()) {
            try {
                Question question = questionRepository.findById(questionId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Question not found: " + questionId));
                if (question.getStatus() == request.targetStatus()) {
                    throw new LearningConflictException("INVALID_STATUS_TRANSITION",
                            "Question " + questionId + " already has status " + request.targetStatus());
                }
                updateQuestionStatusInNewTx(questionId, request.targetStatus(), adminUserId);
                updated.add(questionId);
            } catch (Exception ex) {
                String reason = ex.getMessage();
                if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
                    reason = rse.getReason();
                }
                skipped.add(new BulkStatusResponse.SkippedQuestion(questionId, reason != null ? reason : "Update failed"));
            }
        }

        return new BulkStatusResponse(updated, skipped);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateQuestionStatusInNewTx(Long questionId, QuestionStatus targetStatus, Long adminUserId) {
        updateQuestionStatus(questionId, targetStatus, adminUserId);
    }

    private void validateOptionsBasic(List<QuestionOptionRequest> options) {
        if (options == null || options.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question must have at least 2 options");
        }
        for (QuestionOptionRequest opt : options) {
            if (opt.optionText() == null || opt.optionText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option text cannot be empty");
            }
        }
    }

    public void validateApprovalPreconditions(Question question, List<QuestionOptionRequest> options, Long chapterId) {
        // 1. Non-empty question text
        if (question.getQuestionText() == null || question.getQuestionText().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval precondition failed: Question text cannot be empty");
        }

        // 2. >= 2 options with non-empty text
        if (options == null || options.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval precondition failed: Question must have at least 2 options");
        }
        for (QuestionOptionRequest opt : options) {
            if (opt.optionText() == null || opt.optionText().trim().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval precondition failed: Option text cannot be empty");
            }
        }

        // 3. Exactly one correct option
        long correctCount = options.stream().filter(QuestionOptionRequest::isCorrect).count();
        if (correctCount != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Approval precondition failed: Question must have exactly one correct option, found " + correctCount);
        }

        // 4. Assigned topic is ACTIVE and belongs to the question's chapter
        if (question.getTopicId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval precondition failed: Question must be assigned to an active topic");
        }
        ChapterTopic topic = chapterTopicRepository.findByTopicIdAndChapterId(question.getTopicId(), chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Approval precondition failed: Topic " + question.getTopicId() + " does not belong to chapter " + chapterId));
        if (topic.getStatus() != TopicStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Approval precondition failed: Assigned topic '" + topic.getName() + "' is INACTIVE");
        }

        // 5. Valid difficulty
        if (question.getDifficulty() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval precondition failed: Difficulty must be specified");
        }

        // 6. Valid language
        if (question.getLanguage() == null || question.getLanguage().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval precondition failed: Language must be specified");
        }
    }

    private void verifyChapterExists(Long chapterId) {
        if (chapterId == null || chapterValidationService.getChapterSummary(chapterId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found: " + chapterId);
        }
    }

    private void verifyTopicBelongsToChapter(Long topicId, Long chapterId) {
        if (!chapterTopicRepository.existsById(topicId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: " + topicId);
        }
        chapterTopicRepository.findByTopicIdAndChapterId(topicId, chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Topic " + topicId + " does not belong to chapter " + chapterId));
    }

    private Map<Long, QuestionAnswerStats> computeAnswerStats(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return Map.of();

        List<QuestionAnswerStats> quizStats = quizAnswerRepository.getCompletedStatsForQuestions(questionIds);
        List<QuestionAnswerStats> diagStats = diagnosticAssessmentAnswerRepository.getCompletedStatsForQuestions(questionIds);

        Map<Long, QuestionAnswerStats> combined = new HashMap<>();
        for (QuestionAnswerStats s : quizStats) {
            combined.put(s.questionId(), s);
        }
        for (QuestionAnswerStats s : diagStats) {
            QuestionAnswerStats existing = combined.get(s.questionId());
            if (existing != null) {
                combined.put(s.questionId(), new QuestionAnswerStats(
                        s.questionId(),
                        existing.totalAnswers() + s.totalAnswers(),
                        existing.correctAnswers() + s.correctAnswers()
                ));
            } else {
                combined.put(s.questionId(), s);
            }
        }
        return combined;
    }

    private Map<Long, Long> computeOpenReportCounts(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return Map.of();
        List<QuestionReportCount> counts = questionReportRepository.countOpenReportsForQuestions(questionIds);
        Map<Long, Long> map = new HashMap<>();
        for (QuestionReportCount c : counts) {
            map.put(c.questionId(), c.openReportCount());
        }
        return map;
    }
}
