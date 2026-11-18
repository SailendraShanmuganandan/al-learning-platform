package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.entity.MistakeRecord;
import com.allearning.backend.learning.entity.MistakeStatus;
import com.allearning.backend.learning.event.GradedAnswersEvent;
import com.allearning.backend.learning.event.GradedSourceType;
import com.allearning.backend.learning.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MistakeServiceTest {

    private MistakeRecordRepository mistakeRecordRepository;
    private QuestionRepository questionRepository;
    private QuizRepository quizRepository;
    private ChapterTopicRepository chapterTopicRepository;
    private QuizAttemptRepository quizAttemptRepository;
    private QuizAnswerRepository quizAnswerRepository;
    private QuestionEligibility questionEligibility;
    private ChapterValidationService chapterValidationService;
    private SubjectLookupService subjectLookupService;
    private LearningProperties learningProperties;
    private RandomGenerator randomGenerator;

    private MistakeService mistakeService;

    @BeforeEach
    void setUp() {
        mistakeRecordRepository = mock(MistakeRecordRepository.class);
        questionRepository = mock(QuestionRepository.class);
        quizRepository = mock(QuizRepository.class);
        chapterTopicRepository = mock(ChapterTopicRepository.class);
        quizAttemptRepository = mock(QuizAttemptRepository.class);
        quizAnswerRepository = mock(QuizAnswerRepository.class);
        questionEligibility = mock(QuestionEligibility.class);
        chapterValidationService = mock(ChapterValidationService.class);
        subjectLookupService = mock(SubjectLookupService.class);
        learningProperties = new LearningProperties();
        randomGenerator = RandomGenerator.of("Random");

        mistakeService = new MistakeService(
                mistakeRecordRepository,
                questionRepository,
                quizRepository,
                chapterTopicRepository,
                quizAttemptRepository,
                quizAnswerRepository,
                questionEligibility,
                chapterValidationService,
                subjectLookupService,
                learningProperties,
                randomGenerator
        );
    }

    @Test
    @DisplayName("Transitions: Wrong answer on new question creates ACTIVE mistake with streak 0")
    void wrongAnswerNewRecordTest() {
        Long userId = 100L;
        Long qId = 1L;
        when(mistakeRecordRepository.findByUserIdAndQuestionId(userId, qId)).thenReturn(Optional.empty());

        GradedAnswersEvent event = new GradedAnswersEvent(
                userId,
                GradedSourceType.CHAPTER_QUIZ,
                50L,
                null,
                OffsetDateTime.now(),
                List.of(new GradedAnswersEvent.GradedAnswerItem(qId, 10L, 100L, false))
        );

        mistakeService.onGradedAnswers(event);

        ArgumentCaptor<MistakeRecord> captor = ArgumentCaptor.forClass(MistakeRecord.class);
        verify(mistakeRecordRepository).save(captor.capture());
        MistakeRecord saved = captor.getValue();

        assertEquals(userId, saved.getUserId());
        assertEquals(qId, saved.getQuestionId());
        assertEquals(MistakeStatus.ACTIVE, saved.getStatus());
        assertEquals(0, saved.getCorrectStreak());
        assertNull(saved.getMasteredAt());
    }

    @Test
    @DisplayName("Transitions: Correct answer on new question does NOT create any record")
    void correctAnswerNoExistingRecordTest() {
        Long userId = 100L;
        Long qId = 1L;
        when(mistakeRecordRepository.findByUserIdAndQuestionId(userId, qId)).thenReturn(Optional.empty());

        GradedAnswersEvent event = new GradedAnswersEvent(
                userId,
                GradedSourceType.CHAPTER_QUIZ,
                50L,
                null,
                OffsetDateTime.now(),
                List.of(new GradedAnswersEvent.GradedAnswerItem(qId, 10L, 100L, true))
        );

        mistakeService.onGradedAnswers(event);
        verify(mistakeRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transitions: Correct answer increments streak, reaching 3 marks MASTERED")
    void correctStreakMasteryTest() {
        Long userId = 100L;
        Long qId = 1L;

        MistakeRecord record = new MistakeRecord(userId, qId);
        record.setStatus(MistakeStatus.ACTIVE);
        record.setCorrectStreak(2);

        when(mistakeRecordRepository.findByUserIdAndQuestionId(userId, qId)).thenReturn(Optional.of(record));

        GradedAnswersEvent event = new GradedAnswersEvent(
                userId,
                GradedSourceType.MISTAKE_PRACTICE,
                50L,
                null,
                OffsetDateTime.now(),
                List.of(new GradedAnswersEvent.GradedAnswerItem(qId, 10L, 100L, true))
        );

        mistakeService.onGradedAnswers(event);

        verify(mistakeRecordRepository).save(record);
        assertEquals(3, record.getCorrectStreak());
        assertEquals(MistakeStatus.MASTERED, record.getStatus());
        assertNotNull(record.getMasteredAt());
    }

    @Test
    @DisplayName("Transitions: Wrong answer on MASTERED record returns it to ACTIVE with streak 0")
    void wrongAnswerOnMasteredTest() {
        Long userId = 100L;
        Long qId = 1L;

        MistakeRecord record = new MistakeRecord(userId, qId);
        record.setStatus(MistakeStatus.MASTERED);
        record.setCorrectStreak(3);
        record.setMasteredAt(OffsetDateTime.now().minusDays(1));

        when(mistakeRecordRepository.findByUserIdAndQuestionId(userId, qId)).thenReturn(Optional.of(record));

        GradedAnswersEvent event = new GradedAnswersEvent(
                userId,
                GradedSourceType.CHAPTER_QUIZ,
                50L,
                null,
                OffsetDateTime.now(),
                List.of(new GradedAnswersEvent.GradedAnswerItem(qId, 10L, 100L, false))
        );

        mistakeService.onGradedAnswers(event);

        verify(mistakeRecordRepository).save(record);
        assertEquals(0, record.getCorrectStreak());
        assertEquals(MistakeStatus.ACTIVE, record.getStatus());
        assertNull(record.getMasteredAt());
    }
}
