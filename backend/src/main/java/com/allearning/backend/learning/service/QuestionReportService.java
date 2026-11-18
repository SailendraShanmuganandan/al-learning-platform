package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.entity.*;
import com.allearning.backend.learning.exception.LearningConflictException;
import com.allearning.backend.learning.exception.NotAnsweredByUserException;
import com.allearning.backend.learning.repository.*;
import org.springframework.data.domain.Page;
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
public class QuestionReportService {

    private final QuestionReportRepository questionReportRepository;
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final DiagnosticAssessmentAnswerRepository diagnosticAssessmentAnswerRepository;
    private final ChapterValidationService chapterValidationService;
    private final LearningUserLookupService userLookupService;
    private final LearningProperties learningProperties;

    public QuestionReportService(
            QuestionReportRepository questionReportRepository,
            QuestionRepository questionRepository,
            QuizRepository quizRepository,
            QuizAnswerRepository quizAnswerRepository,
            DiagnosticAssessmentAnswerRepository diagnosticAssessmentAnswerRepository,
            ChapterValidationService chapterValidationService,
            LearningUserLookupService userLookupService,
            LearningProperties learningProperties
    ) {
        this.questionReportRepository = questionReportRepository;
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.diagnosticAssessmentAnswerRepository = diagnosticAssessmentAnswerRepository;
        this.chapterValidationService = chapterValidationService;
        this.userLookupService = userLookupService;
        this.learningProperties = learningProperties;
    }

    public QuestionReportCreateResponse createReport(Long userId, Long questionId, QuestionReportCreateRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + questionId));

        boolean answered = quizAnswerRepository.hasCompletedAnswerForUser(userId, questionId)
                || diagnosticAssessmentAnswerRepository.hasCompletedAnswerForUser(userId, questionId);

        if (!answered) {
            throw new NotAnsweredByUserException("NOT_ANSWERED_BY_USER", "You can only report questions you have answered in a completed attempt");
        }

        if (questionReportRepository.existsByQuestionIdAndUserId(questionId, userId)) {
            throw new LearningConflictException("ALREADY_REPORTED", "You have already reported this question");
        }

        QuestionReport report = new QuestionReport();
        report.setQuestionId(questionId);
        report.setUserId(userId);
        report.setReason(request.reason());
        report.setComment(request.comment());
        report.setStatus(QuestionReportStatus.OPEN);

        QuestionReport saved = questionReportRepository.save(report);

        long openCount = questionReportRepository.countDistinctOpenReporters(questionId);
        if (openCount >= learningProperties.getReportThreshold() && question.getStatus() == QuestionStatus.APPROVED) {
            question.setStatus(QuestionStatus.PENDING);
            question.setApprovedBy(null);
            question.setApprovedAt(null);
            questionRepository.save(question);
        }

        return new QuestionReportCreateResponse(saved.getReportId(), saved.getStatus());
    }

    @Transactional(readOnly = true)
    public Page<QuestionReportAdminResponse> getAdminReports(
            QuestionReportStatus status,
            Long questionId,
            Long chapterId,
            Pageable pageable
    ) {
        Page<QuestionReport> page = questionReportRepository.findAdminReports(status, questionId, chapterId, pageable);
        if (page.isEmpty()) {
            return page.map(r -> null);
        }

        Set<Long> userIds = page.stream().map(QuestionReport::getUserId).collect(Collectors.toSet());
        Map<Long, String> displayNames = userLookupService.getUserDisplayNames(userIds);

        Set<Long> qIds = page.stream().map(QuestionReport::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questions = questionRepository.findByQuestionIdIn(qIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, q -> q));

        Set<Long> quizIds = questions.values().stream().map(Question::getQuizId).collect(Collectors.toSet());
        Map<Long, Quiz> quizzes = quizRepository.findAllById(quizIds).stream()
                .collect(Collectors.toMap(Quiz::getQuizId, qz -> qz));

        return page.map(r -> {
            Question q = questions.get(r.getQuestionId());
            String qText = q != null ? q.getQuestionText() : null;
            QuestionStatus qStatus = q != null ? q.getStatus() : null;
            Quiz qz = (q != null) ? quizzes.get(q.getQuizId()) : null;
            Long chapId = qz != null ? qz.getChapterId() : null;
            String chapName = (chapId != null)
                    ? chapterValidationService.getChapterSummary(chapId).map(ChapterValidationService.ChapterSummary::name).orElse(null)
                    : null;
            String reporterName = displayNames.get(r.getUserId());

            return new QuestionReportAdminResponse(
                    r.getReportId(),
                    r.getQuestionId(),
                    qText,
                    qStatus,
                    chapId,
                    chapName,
                    r.getReason(),
                    r.getComment(),
                    r.getStatus(),
                    new QuestionReportAdminResponse.ReporterDto(r.getUserId(), reporterName != null ? reporterName : "User " + r.getUserId()),
                    r.getCreatedAt(),
                    r.getResolvedBy(),
                    r.getResolvedAt()
            );
        });
    }

    public QuestionReportAdminResponse updateReportStatus(Long reportId, Long adminId, QuestionReportStatus targetStatus) {
        QuestionReport report = questionReportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question report not found: " + reportId));

        if (report.getStatus() != QuestionReportStatus.OPEN) {
            throw new LearningConflictException("INVALID_STATUS_TRANSITION", "Can only change status from OPEN");
        }

        if (targetStatus != QuestionReportStatus.RESOLVED && targetStatus != QuestionReportStatus.DISMISSED) {
            throw new LearningConflictException("INVALID_STATUS_TRANSITION", "Target status must be RESOLVED or DISMISSED");
        }

        report.setStatus(targetStatus);
        report.setResolvedBy(adminId);
        report.setResolvedAt(OffsetDateTime.now());

        QuestionReport saved = questionReportRepository.save(report);

        Question q = questionRepository.findById(saved.getQuestionId()).orElse(null);
        Quiz qz = (q != null) ? quizRepository.findById(q.getQuizId()).orElse(null) : null;
        Long chapId = qz != null ? qz.getChapterId() : null;
        String chapName = (chapId != null)
                ? chapterValidationService.getChapterSummary(chapId).map(ChapterValidationService.ChapterSummary::name).orElse(null)
                : null;
        String reporterName = userLookupService.getUserDisplayNames(Set.of(saved.getUserId())).get(saved.getUserId());

        return new QuestionReportAdminResponse(
                saved.getReportId(),
                saved.getQuestionId(),
                q != null ? q.getQuestionText() : null,
                q != null ? q.getStatus() : null,
                chapId,
                chapName,
                saved.getReason(),
                saved.getComment(),
                saved.getStatus(),
                new QuestionReportAdminResponse.ReporterDto(saved.getUserId(), reporterName != null ? reporterName : "User " + saved.getUserId()),
                saved.getCreatedAt(),
                saved.getResolvedBy(),
                saved.getResolvedAt()
        );
    }
}
