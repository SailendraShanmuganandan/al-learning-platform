package com.allearning.backend.learning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the learning domain.
 */
@Configuration
@ConfigurationProperties(prefix = "app.learning")
public class LearningProperties {

    /**
     * Fixed mastery streak threshold (BR-33). Non-configurable.
     */
    public static final int MASTERY_STREAK = 3;

    private int attemptQuestionCount = 10;
    private int minEligibleQuestions = 5;
    private int assessmentQuestionsPerChapter = 2;
    private int assessmentMaxQuestions = 60;
    private double weakThreshold = 0.50;
    private double strongThreshold = 0.80;
    private int accuracyWindow = 20;
    private int minAnswersChapter = 5;
    private int minAnswersTopic = 3;
    private int trendMinAnswers = 20;
    private int trendDeltaPoints = 10;
    private int reportThreshold = 3;
    private int lowCorrectRateMinAnswers = 20;
    private int lowCorrectRatePercent = 20;
    private int difficultyEasyPercent = 30;
    private int difficultyMediumPercent = 50;
    private int difficultyHardPercent = 20;
    private int mistakeWeightPercent = 30;

    private int studySessionGraceMinutes = 5;
    private java.util.Map<Integer, Integer> studySessionQuestionCounts = new java.util.HashMap<>(java.util.Map.of(
            10, 5,
            20, 10,
            30, 15,
            60, 30
    ));
    private double recommendationVeryWeakAccuracy = 0.40;
    private int recommendationMinMistakesForPractice = 3;
    private int adminAnalyticsMaxStudents = 5000;

    public int getStudySessionGraceMinutes() {
        return studySessionGraceMinutes;
    }

    public void setStudySessionGraceMinutes(int studySessionGraceMinutes) {
        this.studySessionGraceMinutes = studySessionGraceMinutes;
    }

    public java.util.Map<Integer, Integer> getStudySessionQuestionCounts() {
        return studySessionQuestionCounts;
    }

    public void setStudySessionQuestionCounts(java.util.Map<Integer, Integer> studySessionQuestionCounts) {
        this.studySessionQuestionCounts = studySessionQuestionCounts;
    }

    public double getRecommendationVeryWeakAccuracy() {
        return recommendationVeryWeakAccuracy;
    }

    public void setRecommendationVeryWeakAccuracy(double recommendationVeryWeakAccuracy) {
        this.recommendationVeryWeakAccuracy = recommendationVeryWeakAccuracy;
    }

    public int getRecommendationMinMistakesForPractice() {
        return recommendationMinMistakesForPractice;
    }

    public void setRecommendationMinMistakesForPractice(int recommendationMinMistakesForPractice) {
        this.recommendationMinMistakesForPractice = recommendationMinMistakesForPractice;
    }

    public int getAdminAnalyticsMaxStudents() {
        return adminAnalyticsMaxStudents;
    }

    public void setAdminAnalyticsMaxStudents(int adminAnalyticsMaxStudents) {
        this.adminAnalyticsMaxStudents = adminAnalyticsMaxStudents;
    }

    public int getAttemptQuestionCount() {
        return attemptQuestionCount;
    }

    public void setAttemptQuestionCount(int attemptQuestionCount) {
        this.attemptQuestionCount = attemptQuestionCount;
    }

    public int getMinEligibleQuestions() {
        return minEligibleQuestions;
    }

    public void setMinEligibleQuestions(int minEligibleQuestions) {
        this.minEligibleQuestions = minEligibleQuestions;
    }

    public int getAssessmentQuestionsPerChapter() {
        return assessmentQuestionsPerChapter;
    }

    public void setAssessmentQuestionsPerChapter(int assessmentQuestionsPerChapter) {
        this.assessmentQuestionsPerChapter = assessmentQuestionsPerChapter;
    }

    public int getAssessmentMaxQuestions() {
        return assessmentMaxQuestions;
    }

    public void setAssessmentMaxQuestions(int assessmentMaxQuestions) {
        this.assessmentMaxQuestions = assessmentMaxQuestions;
    }

    public double getWeakThreshold() {
        return weakThreshold;
    }

    public void setWeakThreshold(double weakThreshold) {
        this.weakThreshold = weakThreshold;
    }

    public double getStrongThreshold() {
        return strongThreshold;
    }

    public void setStrongThreshold(double strongThreshold) {
        this.strongThreshold = strongThreshold;
    }

    public int getAccuracyWindow() {
        return accuracyWindow;
    }

    public void setAccuracyWindow(int accuracyWindow) {
        this.accuracyWindow = accuracyWindow;
    }

    public int getMinAnswersChapter() {
        return minAnswersChapter;
    }

    public void setMinAnswersChapter(int minAnswersChapter) {
        this.minAnswersChapter = minAnswersChapter;
    }

    public int getMinAnswersTopic() {
        return minAnswersTopic;
    }

    public void setMinAnswersTopic(int minAnswersTopic) {
        this.minAnswersTopic = minAnswersTopic;
    }

    public int getTrendMinAnswers() {
        return trendMinAnswers;
    }

    public void setTrendMinAnswers(int trendMinAnswers) {
        this.trendMinAnswers = trendMinAnswers;
    }

    public int getTrendDeltaPoints() {
        return trendDeltaPoints;
    }

    public void setTrendDeltaPoints(int trendDeltaPoints) {
        this.trendDeltaPoints = trendDeltaPoints;
    }

    public int getReportThreshold() {
        return reportThreshold;
    }

    public void setReportThreshold(int reportThreshold) {
        this.reportThreshold = reportThreshold;
    }

    public int getLowCorrectRateMinAnswers() {
        return lowCorrectRateMinAnswers;
    }

    public void setLowCorrectRateMinAnswers(int lowCorrectRateMinAnswers) {
        this.lowCorrectRateMinAnswers = lowCorrectRateMinAnswers;
    }

    public int getLowCorrectRatePercent() {
        return lowCorrectRatePercent;
    }

    public void setLowCorrectRatePercent(int lowCorrectRatePercent) {
        this.lowCorrectRatePercent = lowCorrectRatePercent;
    }

    public int getDifficultyEasyPercent() {
        return difficultyEasyPercent;
    }

    public void setDifficultyEasyPercent(int difficultyEasyPercent) {
        this.difficultyEasyPercent = difficultyEasyPercent;
    }

    public int getDifficultyMediumPercent() {
        return difficultyMediumPercent;
    }

    public void setDifficultyMediumPercent(int difficultyMediumPercent) {
        this.difficultyMediumPercent = difficultyMediumPercent;
    }

    public int getDifficultyHardPercent() {
        return difficultyHardPercent;
    }

    public void setDifficultyHardPercent(int difficultyHardPercent) {
        this.difficultyHardPercent = difficultyHardPercent;
    }

    public int getMistakeWeightPercent() {
        return mistakeWeightPercent;
    }

    public void setMistakeWeightPercent(int mistakeWeightPercent) {
        this.mistakeWeightPercent = mistakeWeightPercent;
    }
}
