package com.allearning.backend.learning.service;

import com.allearning.backend.learning.config.LearningProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PerformanceCalculator {

    public record PerformanceResult(
            int answerCount,
            int correctCount,
            Double accuracy,
            PerformanceLevel level,
            PerformanceTrend trend
    ) {}

    public PerformanceResult calculate(
            List<Boolean> answersMostRecentFirst,
            PerformanceScope scope,
            LearningProperties properties
    ) {
        int window = properties != null ? properties.getAccuracyWindow() : 20;
        int minChapter = properties != null ? properties.getMinAnswersChapter() : 5;
        int minTopic = properties != null ? properties.getMinAnswersTopic() : 3;
        return calculate(answersMostRecentFirst, scope, window, minChapter, minTopic);
    }

    public PerformanceResult calculate(
            List<Boolean> answersMostRecentFirst,
            int accuracyWindow,
            PerformanceScope scope,
            LearningProperties properties
    ) {
        return calculate(answersMostRecentFirst, scope, accuracyWindow,
                properties != null ? properties.getMinAnswersChapter() : 5,
                properties != null ? properties.getMinAnswersTopic() : 3);
    }

    public PerformanceResult calculate(
            List<Boolean> answersMostRecentFirst,
            PerformanceScope scope,
            int accuracyWindow,
            int minAnswersChapter,
            int minAnswersTopic
    ) {
        if (answersMostRecentFirst == null || answersMostRecentFirst.isEmpty()) {
            return new PerformanceResult(0, 0, null, PerformanceLevel.INSUFFICIENT_DATA, null);
        }

        int count = Math.min(answersMostRecentFirst.size(), accuracyWindow);
        List<Boolean> considered = answersMostRecentFirst.subList(0, count);

        int correctCount = 0;
        for (Boolean b : considered) {
            if (Boolean.TRUE.equals(b)) {
                correctCount++;
            }
        }

        int minRequired = (scope == PerformanceScope.CHAPTER) ? minAnswersChapter : minAnswersTopic;
        Double accuracy = (count > 0) ? (Math.round(((double) correctCount / count) * 1000.0) / 1000.0) : null;

        PerformanceLevel level;
        if (count < minRequired) {
            level = PerformanceLevel.INSUFFICIENT_DATA;
        } else {
            // Integer-safe threshold comparison: < 50% => WEAK, >= 50% and < 80% => DEVELOPING, >= 80% => STRONG
            long left = (long) correctCount * 100;
            if (left < 50L * count) {
                level = PerformanceLevel.WEAK;
            } else if (left < 80L * count) {
                level = PerformanceLevel.DEVELOPING;
            } else {
                level = PerformanceLevel.STRONG;
            }
        }

        PerformanceTrend trend = null;
        if (answersMostRecentFirst.size() >= accuracyWindow) {
            List<Boolean> newest10 = considered.subList(0, 10);
            List<Boolean> previous10 = considered.subList(10, 20);

            int newest10Correct = 0;
            for (Boolean b : newest10) {
                if (Boolean.TRUE.equals(b)) newest10Correct++;
            }

            int previous10Correct = 0;
            for (Boolean b : previous10) {
                if (Boolean.TRUE.equals(b)) previous10Correct++;
            }

            // Percentage points: each correct out of 10 is 10 points
            int newestPct = newest10Correct * 10;
            int previousPct = previous10Correct * 10;
            int delta = newestPct - previousPct;

            if (delta >= 10) {
                trend = PerformanceTrend.IMPROVING;
            } else if (delta <= -10) {
                trend = PerformanceTrend.DECLINING;
            } else {
                trend = PerformanceTrend.STEADY;
            }
        }

        return new PerformanceResult(count, correctCount, accuracy, level, trend);
    }
}
