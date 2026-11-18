package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.learning.dto.QuizDetailResponse;
import com.allearning.backend.learning.dto.QuizStatusUpdateRequest;
import com.allearning.backend.learning.dto.QuizSummaryResponse;
import com.allearning.backend.learning.dto.QuizUpdateRequest;
import com.allearning.backend.learning.entity.QuestionStatus;
import com.allearning.backend.learning.entity.Quiz;
import com.allearning.backend.learning.entity.QuizStatus;
import com.allearning.backend.learning.repository.QuestionRepository;
import com.allearning.backend.learning.repository.QuizRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionEligibility questionEligibility;
    private final ChapterValidationService chapterValidationService;
    private final SubjectLookupService subjectLookupService;

    public QuizService(
            QuizRepository quizRepository,
            QuestionRepository questionRepository,
            QuestionEligibility questionEligibility,
            ChapterValidationService chapterValidationService,
            SubjectLookupService subjectLookupService
    ) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.questionEligibility = questionEligibility;
        this.chapterValidationService = chapterValidationService;
        this.subjectLookupService = subjectLookupService;
    }

    public Quiz getOrCreateDraftQuiz(Long chapterId) {
        return quizRepository.findByChapterId(chapterId)
                .orElseGet(() -> {
                    ChapterValidationService.ChapterSummary chapterSummary = chapterValidationService.getChapterSummary(chapterId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found: " + chapterId));

                    String title = chapterSummary.name() + " Quiz";
                    Quiz quiz = new Quiz(chapterId, title, null);
                    quiz.setStatus(QuizStatus.DRAFT);
                    return quizRepository.save(quiz);
                });
    }

    @Transactional(readOnly = true)
    public Page<QuizSummaryResponse> getAdminQuizzes(Long subjectId, Long chapterId, QuizStatus status, Pageable pageable) {
        List<Long> chapterIdsForSubject = null;
        if (subjectId != null) {
            chapterIdsForSubject = chapterValidationService.getActiveChapterIdsBySubject(subjectId);
            if (chapterIdsForSubject.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
        }

        final List<Long> filterChapterIds = chapterIdsForSubject;

        Specification<Quiz> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (chapterId != null) {
                predicates.add(cb.equal(root.get("chapterId"), chapterId));
            } else if (filterChapterIds != null) {
                predicates.add(root.get("chapterId").in(filterChapterIds));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Quiz> quizPage = quizRepository.findAll(spec, pageable);

        List<QuizSummaryResponse> summaries = quizPage.getContent().stream()
                .map(this::toQuizSummary)
                .toList();

        return new PageImpl<>(summaries, pageable, quizPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizDetail(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found: " + quizId));
        return toQuizDetail(quiz);
    }

    public QuizDetailResponse updateQuiz(Long quizId, QuizUpdateRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found: " + quizId));

        if (request.title() != null && !request.title().isBlank()) {
            quiz.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            quiz.setDescription(request.description());
        }

        Quiz saved = quizRepository.save(quiz);
        return toQuizDetail(saved);
    }

    public QuizDetailResponse updateQuizStatus(Long quizId, QuizStatusUpdateRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found: " + quizId));

        quiz.setStatus(request.status());
        Quiz saved = quizRepository.save(quiz);
        return toQuizDetail(saved);
    }

    private QuizSummaryResponse toQuizSummary(Quiz quiz) {
        Long chapterId = quiz.getChapterId();
        String chapterName = "Chapter " + chapterId;
        Long subjectId = null;
        String subjectName = null;

        var optSummary = chapterValidationService.getChapterSummary(chapterId);
        if (optSummary.isPresent()) {
            var sum = optSummary.get();
            chapterName = sum.name();
            subjectId = sum.subjectId();
            if (subjectId != null) {
                var optSubject = subjectLookupService.getSubjectInfo(subjectId);
                if (optSubject.isPresent()) {
                    subjectName = optSubject.get().name();
                }
            }
        }

        long questionCount = questionRepository.countByQuizId(quiz.getQuizId());
        long approvedCount = questionRepository.countByQuizIdAndStatus(quiz.getQuizId(), QuestionStatus.APPROVED);
        long pendingCount = questionRepository.countByQuizIdAndStatus(quiz.getQuizId(), QuestionStatus.PENDING);
        long eligibleCount = questionEligibility.countEligible(chapterId, null);
        Map<String, Long> eligibleByLanguage = questionEligibility.eligibleLanguageCounts(chapterId);

        return new QuizSummaryResponse(
                quiz.getQuizId(),
                chapterId,
                chapterName,
                subjectId,
                subjectName,
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getStatus(),
                questionCount,
                approvedCount,
                pendingCount,
                eligibleCount,
                eligibleByLanguage,
                quiz.getCreatedAt(),
                quiz.getUpdatedAt()
        );
    }

    private QuizDetailResponse toQuizDetail(Quiz quiz) {
        QuizSummaryResponse summary = toQuizSummary(quiz);
        return new QuizDetailResponse(
                summary.quizId(),
                summary.chapterId(),
                summary.chapterName(),
                summary.subjectId(),
                summary.subjectName(),
                summary.title(),
                summary.description(),
                summary.status(),
                summary.questionCount(),
                summary.approvedCount(),
                summary.pendingCount(),
                summary.eligibleCount(),
                summary.eligibleByLanguage(),
                summary.createdAt(),
                summary.updatedAt()
        );
    }
}
