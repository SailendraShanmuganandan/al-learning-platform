package com.allearning.backend.learning;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.learning.entity.*;
import com.allearning.backend.learning.exception.LearningConflictException;
import com.allearning.backend.learning.repository.*;
import com.allearning.backend.learning.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionStatusMachineTest {
    private QuestionRepository questions;
    private QuizRepository quizzes;
    private QuestionService service;

    private static class TestQuestionService extends QuestionService {
        private TestQuestionService(QuestionRepository questions, QuestionOptionRepository options,
                                    ChapterTopicRepository topics, QuizRepository quizzes,
                                    QuizAnswerRepository answers,
                                    DiagnosticAssessmentAnswerRepository diagnostics,
                                    QuestionReportRepository reports,
                                    ChapterDocumentRepository documents, QuizService quizService,
                                    ChapterValidationService chapters, SubjectLookupService subjects,
                                    LearningUserLookupService users,
                                    ApplicationEventPublisher publisher,
                                    com.allearning.backend.learning.config.LearningProperties properties) {
            super(questions, options, topics, quizzes, answers, diagnostics, reports, documents,
                    quizService, chapters, subjects, users, publisher, properties);
        }

        @Override
        public com.allearning.backend.learning.dto.QuestionDetailResponse getQuestionDetail(Long questionId) {
            return null;
        }
    }

    @BeforeEach
    void setUp() {
        questions = mock(QuestionRepository.class);
        quizzes = mock(QuizRepository.class);
        service = new TestQuestionService(questions, mock(QuestionOptionRepository.class),
                mock(ChapterTopicRepository.class), quizzes, mock(QuizAnswerRepository.class),
                mock(DiagnosticAssessmentAnswerRepository.class), mock(QuestionReportRepository.class),
                mock(ChapterDocumentRepository.class), mock(QuizService.class),
                mock(ChapterValidationService.class), mock(SubjectLookupService.class),
                mock(LearningUserLookupService.class),
                mock(ApplicationEventPublisher.class), new com.allearning.backend.learning.config.LearningProperties());
        doReturn(null).when(questions).findById(anyLong());
        Quiz quiz = new Quiz(10L, "Quiz", null);
        quiz.setQuizId(1L);
        when(quizzes.findById(1L)).thenReturn(Optional.of(quiz));
    }

    private Question question(QuestionStatus status) {
        Question q = new Question();
        q.setQuestionId(100L);
        q.setQuizId(1L);
        q.setQuestionText("Valid question");
        q.setTopicId(7L);
        q.setDifficulty(QuestionDifficulty.EASY);
        q.setLanguage("EN");
        q.addOption(new QuestionOption("A", 1, true));
        q.addOption(new QuestionOption("B", 2, false));
        q.setStatus(status);
        when(questions.findById(100L)).thenReturn(Optional.of(q));
        when(questions.save(q)).thenReturn(q);
        return q;
    }

    @Test void pendingToApproved() {
        Question q = question(QuestionStatus.PENDING);
        ChapterTopicRepository topics = mock(ChapterTopicRepository.class);
        // Approval validation needs a topic repository; replace through a dedicated service.
        QuestionService withTopic = new TestQuestionService(questions, mock(QuestionOptionRepository.class),
                topics, quizzes, mock(QuizAnswerRepository.class),
                mock(DiagnosticAssessmentAnswerRepository.class), mock(QuestionReportRepository.class),
                mock(ChapterDocumentRepository.class), mock(QuizService.class),
                mock(ChapterValidationService.class), mock(SubjectLookupService.class),
                mock(LearningUserLookupService.class),
                mock(ApplicationEventPublisher.class), new com.allearning.backend.learning.config.LearningProperties());
        when(questions.findById(100L)).thenReturn(Optional.of(q));
        ChapterTopic topic = new ChapterTopic(10L, "Topic");
        when(topics.findByTopicIdAndChapterId(7L, 10L)).thenReturn(Optional.of(topic));
        withTopic.updateQuestionStatus(100L, QuestionStatus.APPROVED, 99L);
        assertEquals(QuestionStatus.APPROVED, q.getStatus());
    }

    @Test void pendingToArchived() {
        Question q = question(QuestionStatus.PENDING);
        service.updateQuestionStatus(100L, QuestionStatus.ARCHIVED, 99L);
        assertEquals(QuestionStatus.ARCHIVED, q.getStatus());
    }

    @Test void approvedToArchived() {
        Question q = question(QuestionStatus.APPROVED);
        service.updateQuestionStatus(100L, QuestionStatus.ARCHIVED, 99L);
        assertEquals(QuestionStatus.ARCHIVED, q.getStatus());
    }

    @Test void approvedToPendingClearsApproval() {
        Question q = question(QuestionStatus.APPROVED);
        q.setApprovedBy(99L);
        q.setApprovedAt(java.time.OffsetDateTime.now());
        service.updateQuestionStatus(100L, QuestionStatus.PENDING, 99L);
        assertNull(q.getApprovedBy());
        assertNull(q.getApprovedAt());
    }

    @Test void archivedToPending() {
        Question q = question(QuestionStatus.ARCHIVED);
        service.updateQuestionStatus(100L, QuestionStatus.PENDING, 99L);
        assertEquals(QuestionStatus.PENDING, q.getStatus());
    }

    @Test void pendingToPendingNoOp() {
        Question q = question(QuestionStatus.PENDING);
        assertDoesNotThrow(() -> service.updateQuestionStatus(100L, QuestionStatus.PENDING, 99L));
    }

    @Test void archivedToApprovedRejected() {
        question(QuestionStatus.ARCHIVED);
        LearningConflictException ex = assertThrows(LearningConflictException.class,
                () -> service.updateQuestionStatus(100L, QuestionStatus.APPROVED, 99L));
        assertEquals("INVALID_STATUS_TRANSITION", ex.getErrorCode());
    }

    @Test void archivedToArchivedNoOp() {
        question(QuestionStatus.ARCHIVED);
        assertDoesNotThrow(() -> service.updateQuestionStatus(100L, QuestionStatus.ARCHIVED, 99L));
    }
}
