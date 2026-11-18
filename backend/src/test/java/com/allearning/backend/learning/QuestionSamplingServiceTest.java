package com.allearning.backend.learning;

import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.entity.MistakeRecord;
import com.allearning.backend.learning.entity.MistakeStatus;
import com.allearning.backend.learning.entity.Question;
import com.allearning.backend.learning.entity.QuestionDifficulty;
import com.allearning.backend.learning.exception.NotEnoughQuestionsException;
import com.allearning.backend.learning.repository.DiagnosticAssessmentAnswerRepository;
import com.allearning.backend.learning.repository.MistakeRecordRepository;
import com.allearning.backend.learning.repository.QuizAnswerRepository;
import com.allearning.backend.learning.service.QuestionEligibility;
import com.allearning.backend.learning.service.QuestionSamplingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionSamplingServiceTest {
    private QuestionEligibility eligibility;
    private MistakeRecordRepository mistakes;
    private QuizAnswerRepository quizAnswers;
    private DiagnosticAssessmentAnswerRepository diagnosticAnswers;
    private LearningProperties properties;

    @BeforeEach
    void setUp() {
        eligibility = mock(QuestionEligibility.class);
        mistakes = mock(MistakeRecordRepository.class);
        quizAnswers = mock(QuizAnswerRepository.class);
        diagnosticAnswers = mock(DiagnosticAssessmentAnswerRepository.class);
        properties = new LearningProperties();
    }

    private QuestionSamplingService service() {
        return new QuestionSamplingService(eligibility, mistakes, quizAnswers, diagnosticAnswers,
                properties, RandomGeneratorFactory.of("L64X128MixRandom").create(42L));
    }

    private void pool(long chapterId, List<Question> questions) {
        List<Long> ids = questions.stream().map(Question::getQuestionId).toList();
        when(eligibility.listEligibleIds(chapterId, null)).thenReturn(ids);
        when(eligibility.filterEligibleQuestions(ids)).thenReturn(questions);
    }

    private Question question(long id, long topic, QuestionDifficulty difficulty) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setTopicId(topic);
        q.setDifficulty(difficulty);
        q.setLanguage("EN");
        return q;
    }

    @Test void sampleForChapterReturnsRequestedCount() {
        List<Question> pool = new ArrayList<>();
        for (long i = 1; i <= 10; i++) pool.add(question(i, i % 4 + 1, i % 3 == 0 ? QuestionDifficulty.HARD : QuestionDifficulty.MEDIUM));
        pool(1L, pool);
        assertEquals(5, service().sampleForChapter(null, 1L, null, 5).size());
    }

    @Test void sampleForChapterRejectsTooFewQuestions() {
        pool(1L, List.of(question(1, 1, QuestionDifficulty.EASY), question(2, 2, QuestionDifficulty.EASY)));
        assertThrows(NotEnoughQuestionsException.class, () -> service().sampleForChapter(null, 1L, null, 3));
    }

    @Test void sampleForChapterUsesDifficultyRatios() {
        List<Question> pool = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            QuestionDifficulty d = i <= 30 ? QuestionDifficulty.EASY : i <= 80 ? QuestionDifficulty.MEDIUM : QuestionDifficulty.HARD;
            pool.add(question(i, i % 5 + 1, d));
        }
        pool(1L, pool);
        List<Question> sample = service().sampleForChapter(null, 1L, null, 20);
        Map<QuestionDifficulty, Long> counts = sample.stream().collect(java.util.stream.Collectors.groupingBy(Question::getDifficulty, java.util.stream.Collectors.counting()));
        assertTrue(Math.abs(counts.getOrDefault(QuestionDifficulty.EASY, 0L) / 20.0 - .30) <= .15);
        assertTrue(Math.abs(counts.getOrDefault(QuestionDifficulty.MEDIUM, 0L) / 20.0 - .50) <= .15);
        assertTrue(Math.abs(counts.getOrDefault(QuestionDifficulty.HARD, 0L) / 20.0 - .20) <= .15);
    }

    @Test void sampleForChapterSpreadsAcrossTopics() {
        List<Question> pool = new ArrayList<>();
        for (long t = 1; t <= 4; t++) for (long i = 1; i <= 5; i++) pool.add(question(t * 10 + i, t, QuestionDifficulty.values()[(int)(i % 3)]));
        pool(1L, pool);
        List<Question> sample = service().sampleForChapter(null, 1L, null, 20);
        assertTrue(sample.stream().map(Question::getTopicId).collect(java.util.stream.Collectors.toSet()).size() >= 2);
    }

    @Test void activeMistakesArePreferred() {
        List<Question> pool = new ArrayList<>();
        List<Question> mistakesPool = new ArrayList<>();
        for (long i = 1; i <= 20; i++) {
            Question q = question(i, i % 4 + 1, QuestionDifficulty.EASY);
            pool.add(q);
            if (i <= 6) mistakesPool.add(q);
        }
        pool(1L, pool);
        MistakeRecord record = new MistakeRecord(7L, 1L);
        MistakeRecord active = new MistakeRecord(7L, 1L);
        when(mistakes.findByUserIdAndStatusOrderByLastAnsweredAtDesc(7L, MistakeStatus.ACTIVE))
                .thenReturn(List.of(active));
        when(quizAnswers.findLastAnsweredTimesForUser(eq(7L), any())).thenReturn(List.of());
        when(diagnosticAnswers.findLastAnsweredTimesForUser(eq(7L), any())).thenReturn(List.of());
        List<Question> sample = service().sampleForChapter(7L, 1L, null, 10);
        assertTrue(sample.stream().anyMatch(q -> q.getQuestionId().equals(1L)));
    }

    @Test void assessmentIsCappedAtMaxTotal() {
        List<Question> first = new ArrayList<>(), second = new ArrayList<>();
        for (long i = 1; i <= 5; i++) first.add(question(i, 1, QuestionDifficulty.EASY));
        for (long i = 6; i <= 10; i++) second.add(question(i, 2, QuestionDifficulty.EASY));
        pool(1L, first);
        pool(2L, second);
        assertEquals(3, service().sampleForAssessment(null, List.of(1L, 2L), null, 3, 3).size());
    }

    @Test void assessmentRejectsChapterWithTooFewQuestions() {
        pool(1L, List.of(question(1, 1, QuestionDifficulty.EASY)));
        assertThrows(NotEnoughQuestionsException.class,
                () -> service().sampleForAssessment(null, List.of(1L), null, 2, 10));
    }

    @Test void assessmentRoundRobinSpreadsAcrossChapters() {
        List<Question> first = new ArrayList<>(), second = new ArrayList<>();
        for (long i = 1; i <= 4; i++) first.add(question(i, i, QuestionDifficulty.EASY));
        for (long i = 5; i <= 8; i++) second.add(question(i, i, QuestionDifficulty.MEDIUM));
        pool(1L, first);
        pool(2L, second);
        List<Question> sample = service().sampleForAssessment(null, List.of(1L, 2L), null, 2, 4);
        assertTrue(sample.stream().limit(2).anyMatch(q -> q.getQuestionId() <= 4));
        assertTrue(sample.stream().skip(1).limit(2).anyMatch(q -> q.getQuestionId() > 4));
    }
}
