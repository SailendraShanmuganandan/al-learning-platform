package com.allearning.backend.learning.service;

import com.allearning.backend.learning.config.LearningProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceCalculatorTest {

    private PerformanceCalculator calculator;
    private LearningProperties properties;

    @BeforeEach
    void setUp() {
        calculator = new PerformanceCalculator();
        properties = new LearningProperties();
        properties.setAccuracyWindow(20);
        properties.setMinAnswersChapter(5);
        properties.setMinAnswersTopic(3);
    }

    @Test
    @DisplayName("Windowing: Takes at most 20 most recent answers")
    void accuracyWindowTest() {
        // 25 answers: first 20 are TRUE, remaining 5 are FALSE
        List<Boolean> answers = new ArrayList<>();
        for (int i = 0; i < 20; i++) answers.add(true);
        for (int i = 0; i < 5; i++) answers.add(false);

        var result = calculator.calculate(answers, PerformanceScope.CHAPTER, properties);
        assertEquals(20, result.answerCount());
        assertEquals(20, result.correctCount());
        assertEquals(1.0, result.accuracy());
    }

    @Test
    @DisplayName("Thresholds: Exactly 49%, 50%, 79%, 80% boundary tests")
    void boundaryThresholdsTest() {
        // Over 100 answers with window = 100 to test exact percentages
        int window = 100;
        int min = 5;

        // 49 correct out of 100 => 49% => WEAK (< 50%)
        List<Boolean> list49 = new ArrayList<>();
        for (int i = 0; i < 49; i++) list49.add(true);
        for (int i = 0; i < 51; i++) list49.add(false);
        var r49 = calculator.calculate(list49, PerformanceScope.CHAPTER, window, min, 3);
        assertEquals(PerformanceLevel.WEAK, r49.level());

        // 50 correct out of 100 => 50% => DEVELOPING (>= 50% and < 80%)
        List<Boolean> list50 = new ArrayList<>();
        for (int i = 0; i < 50; i++) list50.add(true);
        for (int i = 0; i < 50; i++) list50.add(false);
        var r50 = calculator.calculate(list50, PerformanceScope.CHAPTER, window, min, 3);
        assertEquals(PerformanceLevel.DEVELOPING, r50.level());

        // 79 correct out of 100 => 79% => DEVELOPING (< 80%)
        List<Boolean> list79 = new ArrayList<>();
        for (int i = 0; i < 79; i++) list79.add(true);
        for (int i = 0; i < 21; i++) list79.add(false);
        var r79 = calculator.calculate(list79, PerformanceScope.CHAPTER, window, min, 3);
        assertEquals(PerformanceLevel.DEVELOPING, r79.level());

        // 80 correct out of 100 => 80% => STRONG (>= 80%)
        List<Boolean> list80 = new ArrayList<>();
        for (int i = 0; i < 80; i++) list80.add(true);
        for (int i = 0; i < 20; i++) list80.add(false);
        var r80 = calculator.calculate(list80, PerformanceScope.CHAPTER, window, min, 3);
        assertEquals(PerformanceLevel.STRONG, r80.level());
    }

    @Test
    @DisplayName("Minimum answers: Chapter min 4 vs 5, Topic min 2 vs 3")
    void minimumAnswersTest() {
        // Chapter: 4 answers -> INSUFFICIENT_DATA
        List<Boolean> fourAnswers = List.of(true, true, true, true);
        var chap4 = calculator.calculate(fourAnswers, PerformanceScope.CHAPTER, properties);
        assertEquals(PerformanceLevel.INSUFFICIENT_DATA, chap4.level());

        // Chapter: 5 answers -> level calculated
        List<Boolean> fiveAnswers = List.of(true, true, true, true, true);
        var chap5 = calculator.calculate(fiveAnswers, PerformanceScope.CHAPTER, properties);
        assertEquals(PerformanceLevel.STRONG, chap5.level());

        // Topic: 2 answers -> INSUFFICIENT_DATA
        List<Boolean> twoAnswers = List.of(true, true);
        var top2 = calculator.calculate(twoAnswers, PerformanceScope.TOPIC, properties);
        assertEquals(PerformanceLevel.INSUFFICIENT_DATA, top2.level());

        // Topic: 3 answers -> level calculated
        List<Boolean> threeAnswers = List.of(true, true, true);
        var top3 = calculator.calculate(threeAnswers, PerformanceScope.TOPIC, properties);
        assertEquals(PerformanceLevel.STRONG, top3.level());
    }

    @Test
    @DisplayName("Trend: Exactly +/- 10 percentage points gives IMPROVING, DECLINING, STEADY")
    void trendCalculationTest() {
        // Exactly 20 answers needed
        // Newest 10 has 6 correct (60%), Previous 10 has 5 correct (50%) => delta = +10 => IMPROVING
        List<Boolean> improving = new ArrayList<>();
        for (int i = 0; i < 6; i++) improving.add(true);
        for (int i = 0; i < 4; i++) improving.add(false);
        for (int i = 0; i < 5; i++) improving.add(true);
        for (int i = 0; i < 5; i++) improving.add(false);
        var rImp = calculator.calculate(improving, PerformanceScope.CHAPTER, properties);
        assertEquals(PerformanceTrend.IMPROVING, rImp.trend());

        // Newest 10 has 4 correct (40%), Previous 10 has 5 correct (50%) => delta = -10 => DECLINING
        List<Boolean> declining = new ArrayList<>();
        for (int i = 0; i < 4; i++) declining.add(true);
        for (int i = 0; i < 6; i++) declining.add(false);
        for (int i = 0; i < 5; i++) declining.add(true);
        for (int i = 0; i < 5; i++) declining.add(false);
        var rDec = calculator.calculate(declining, PerformanceScope.CHAPTER, properties);
        assertEquals(PerformanceTrend.DECLINING, rDec.trend());

        // Newest 10 has 5 correct (50%), Previous 10 has 5 correct (50%) => delta = 0 => STEADY
        List<Boolean> steady = new ArrayList<>();
        for (int i = 0; i < 5; i++) steady.add(true);
        for (int i = 0; i < 5; i++) steady.add(false);
        for (int i = 0; i < 5; i++) steady.add(true);
        for (int i = 0; i < 5; i++) steady.add(false);
        var rSte = calculator.calculate(steady, PerformanceScope.CHAPTER, properties);
        assertEquals(PerformanceTrend.STEADY, rSte.trend());
    }

    @Test
    @DisplayName("Trend: Null when fewer than 20 answers exist")
    void trendNullWith19AnswersTest() {
        List<Boolean> nineteen = new ArrayList<>();
        for (int i = 0; i < 19; i++) nineteen.add(true);

        var result = calculator.calculate(nineteen, PerformanceScope.CHAPTER, properties);
        assertNull(result.trend());
    }
}

