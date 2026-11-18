package com.allearning.backend.learning.service;

import com.allearning.backend.learning.dto.SubmitAnswerItem;
import com.allearning.backend.learning.exception.GradingValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnswerGraderTest {

    private AnswerGrader grader;

    @BeforeEach
    void setUp() {
        grader = new AnswerGrader();
    }

    private AnswerGrader.ServedQuestionForGrading makeServed(Long id, Long correctOpt, Set<Long> options) {
        return new AnswerGrader.ServedQuestionForGrading(id, 10L, 100L, options, correctOpt, "Explanation for " + id);
    }

    @Test
    @DisplayName("Grading: All questions answered correctly produces 100% score")
    void allCorrectTest() {
        var served = List.of(
                makeServed(1L, 11L, Set.of(11L, 12L)),
                makeServed(2L, 22L, Set.of(21L, 22L))
        );

        var submitted = List.of(
                new SubmitAnswerItem(1L, 11L),
                new SubmitAnswerItem(2L, 22L)
        );

        var result = grader.grade(served, submitted);
        assertEquals(2, result.score());
        assertEquals(2, result.totalMarks());
        assertEquals(100.0, result.percentage());
        assertTrue(result.answersByQuestionId().get(1L).isCorrect());
        assertTrue(result.answersByQuestionId().get(2L).isCorrect());
    }

    @Test
    @DisplayName("Grading: Mixed answers calculate correct score and details")
    void mixedAnswersTest() {
        var served = List.of(
                makeServed(1L, 11L, Set.of(11L, 12L)),
                makeServed(2L, 22L, Set.of(21L, 22L))
        );

        var submitted = List.of(
                new SubmitAnswerItem(1L, 11L),
                new SubmitAnswerItem(2L, 21L) // wrong option
        );

        var result = grader.grade(served, submitted);
        assertEquals(1, result.score());
        assertEquals(2, result.totalMarks());
        assertEquals(50.0, result.percentage());
        assertTrue(result.answersByQuestionId().get(1L).isCorrect());
        assertFalse(result.answersByQuestionId().get(2L).isCorrect());
    }

    @Test
    @DisplayName("Validation: Missing served question throws 400 UNANSWERED_QUESTIONS with missing IDs")
    void unansweredQuestionTest() {
        var served = List.of(
                makeServed(1L, 11L, Set.of(11L, 12L)),
                makeServed(2L, 22L, Set.of(21L, 22L))
        );

        var submitted = List.of(
                new SubmitAnswerItem(1L, 11L)
        );

        GradingValidationException ex = assertThrows(GradingValidationException.class, () -> grader.grade(served, submitted));
        assertEquals("UNANSWERED_QUESTIONS", ex.getErrorCode());
        assertTrue(ex.getDetails().containsKey("unansweredQuestionIds"));
        assertEquals(List.of(2L), ex.getDetails().get("unansweredQuestionIds"));
    }

    @Test
    @DisplayName("Validation: Question submitted that was not served throws 400 INVALID_ANSWER")
    void questionNotServedTest() {
        var served = List.of(
                makeServed(1L, 11L, Set.of(11L, 12L))
        );

        var submitted = List.of(
                new SubmitAnswerItem(1L, 11L),
                new SubmitAnswerItem(999L, 11L)
        );

        GradingValidationException ex = assertThrows(GradingValidationException.class, () -> grader.grade(served, submitted));
        assertEquals("INVALID_ANSWER", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("was not part of this attempt"));
    }

    @Test
    @DisplayName("Validation: Duplicate question in submission throws 400 INVALID_ANSWER")
    void duplicateQuestionTest() {
        var served = List.of(
                makeServed(1L, 11L, Set.of(11L, 12L)),
                makeServed(2L, 22L, Set.of(21L, 22L))
        );

        var submitted = List.of(
                new SubmitAnswerItem(1L, 11L),
                new SubmitAnswerItem(1L, 12L)
        );

        GradingValidationException ex = assertThrows(GradingValidationException.class, () -> grader.grade(served, submitted));
        assertEquals("INVALID_ANSWER", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Duplicate question"));
    }

    @Test
    @DisplayName("Validation: Option not belonging to question throws 400 INVALID_ANSWER")
    void optionNotBelongingTest() {
        var served = List.of(
                makeServed(1L, 11L, Set.of(11L, 12L))
        );

        var submitted = List.of(
                new SubmitAnswerItem(1L, 999L) // 999 is not in Set.of(11, 12)
        );

        GradingValidationException ex = assertThrows(GradingValidationException.class, () -> grader.grade(served, submitted));
        assertEquals("INVALID_ANSWER", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("does not belong to question"));
    }
}

