package com.allearning.backend.learning;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.learning.dto.QuestionOptionRequest;
import com.allearning.backend.learning.entity.ChapterTopic;
import com.allearning.backend.learning.entity.Question;
import com.allearning.backend.learning.entity.QuestionDifficulty;
import com.allearning.backend.learning.entity.TopicStatus;
import com.allearning.backend.learning.repository.*;
import com.allearning.backend.learning.service.LearningUserLookupService;
import com.allearning.backend.learning.service.QuestionService;
import com.allearning.backend.learning.service.QuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionApprovalRulesTest {
    private ChapterTopicRepository topics;
    private QuestionService service;

    @BeforeEach
    void setUp() {
        topics = mock(ChapterTopicRepository.class);
        service = new QuestionService(
                mock(QuestionRepository.class), mock(QuestionOptionRepository.class), topics,
                mock(QuizRepository.class), mock(QuizAnswerRepository.class),
                mock(DiagnosticAssessmentAnswerRepository.class), mock(QuestionReportRepository.class),
                mock(ChapterDocumentRepository.class), mock(QuizService.class),
                mock(ChapterValidationService.class), mock(SubjectLookupService.class),
                mock(LearningUserLookupService.class),
                mock(ApplicationEventPublisher.class), new com.allearning.backend.learning.config.LearningProperties());
    }

    private Question validQuestion() {
        Question q = new Question();
        q.setQuestionText("What is 2 + 2?");
        q.setTopicId(7L);
        q.setDifficulty(QuestionDifficulty.EASY);
        q.setLanguage("EN");
        return q;
    }

    private List<QuestionOptionRequest> options(int correct) {
        return List.of(
                new QuestionOptionRequest(null, "Two", 1, correct == 1),
                new QuestionOptionRequest(null, "Four", 2, correct == 2));
    }

    private void activeTopic() {
        ChapterTopic topic = new ChapterTopic(10L, "Numbers");
        topic.setTopicId(7L);
        when(topics.findByTopicIdAndChapterId(7L, 10L)).thenReturn(Optional.of(topic));
    }

    @Test void passesWhenAllPreconditionsSatisfied() {
        activeTopic();
        assertDoesNotThrow(() -> service.validateApprovalPreconditions(validQuestion(), options(1), 10L));
    }

    @Test void rejectsBlankQuestionText() {
        activeTopic();
        Question q = validQuestion();
        q.setQuestionText(" ");
        assertFailure(q, options(1), "Question text cannot be empty");
    }

    @Test void rejectsFewerThanTwoOptions() {
        activeTopic();
        assertFailure(validQuestion(), List.of(new QuestionOptionRequest(null, "One", 1, true)), "at least 2 options");
    }

    @Test void rejectsZeroCorrectOptions() {
        activeTopic();
        assertFailure(validQuestion(), List.of(
                new QuestionOptionRequest(null, "Two", 1, false),
                new QuestionOptionRequest(null, "Four", 2, false)), "exactly one correct option");
    }

    @Test void rejectsTwoCorrectOptions() {
        activeTopic();
        assertFailure(validQuestion(), List.of(
                new QuestionOptionRequest(null, "Two", 1, true),
                new QuestionOptionRequest(null, "Four", 2, true)), "exactly one correct option");
    }

    @Test void rejectsNullTopic() {
        Question q = validQuestion();
        q.setTopicId(null);
        assertFailure(q, options(1), "active topic");
    }

    @Test void rejectsTopicNotFound() {
        when(topics.findByTopicIdAndChapterId(7L, 10L)).thenReturn(Optional.empty());
        assertFailure(validQuestion(), options(1), "does not belong to chapter");
    }

    @Test void rejectsInactiveTopic() {
        ChapterTopic topic = new ChapterTopic(10L, "Numbers");
        topic.setTopicId(7L);
        topic.setStatus(TopicStatus.INACTIVE);
        when(topics.findByTopicIdAndChapterId(7L, 10L)).thenReturn(Optional.of(topic));
        assertFailure(validQuestion(), options(1), "INACTIVE");
    }

    @Test void rejectsNullDifficulty() {
        activeTopic();
        Question q = validQuestion();
        q.setDifficulty(null);
        assertFailure(q, options(1), "Difficulty must be specified");
    }

    @Test void rejectsBlankLanguage() {
        activeTopic();
        Question q = validQuestion();
        q.setLanguage(" ");
        assertFailure(q, options(1), "Language must be specified");
    }

    private void assertFailure(Question q, List<QuestionOptionRequest> options, String message) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.validateApprovalPreconditions(q, options, 10L));
        assertTrue(ex.getReason().contains(message), ex.getReason());
    }
}
