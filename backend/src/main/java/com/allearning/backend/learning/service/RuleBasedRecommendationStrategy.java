package com.allearning.backend.learning.service;

import com.allearning.backend.learning.dto.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;

@Component
public class RuleBasedRecommendationStrategy implements RecommendationStrategy {

    @Override
    public Recommendation recommend(RecommendationContext ctx) {
        // Collect candidate recommendations in order of evaluation
        List<RecommendationAlternative> candidates = new ArrayList<>();

        // Group syllabus chapters by chapterId
        Map<Long, RecommendationContext.ChapterOrderInfo> syllabusMap = new LinkedHashMap<>();
        for (var c : ctx.syllabusChapters()) {
            syllabusMap.put(c.chapterId(), c);
        }

        // Map performances by chapterId
        Map<Long, ChapterPerformanceDto> perfMap = new LinkedHashMap<>();
        for (var p : ctx.chapterPerformances()) {
            perfMap.put(p.chapterId(), p);
        }

        // 1. Check if NO DATA: no participating chapter has a level other than INSUFFICIENT_DATA
        boolean hasEvaluatedParticipatingData = false;
        for (var c : ctx.syllabusChapters()) {
            if (c.participates()) {
                ChapterPerformanceDto p = perfMap.get(c.chapterId());
                if (p != null && p.level() != null && p.level() != PerformanceLevel.INSUFFICIENT_DATA) {
                    hasEvaluatedParticipatingData = true;
                    break;
                }
            }
        }

        if (!hasEvaluatedParticipatingData) {
            // Rule 1: NO DATA -> Return immediately
            if (ctx.offerInitialDiagnostic()) {
                RecommendationTarget target = RecommendationTarget.ofKind("DIAGNOSTIC");
                return new Recommendation(
                        RecommendationType.TAKE_DIAGNOSTIC,
                        "Initial Diagnostic Assessment",
                        "Take the diagnostic assessment to evaluate your knowledge across the syllabus and get personalized recommendations.",
                        "NO_DATA_DIAGNOSTIC",
                        target,
                        List.of()
                );
            }

            // Otherwise EXPLORE_CHAPTER: first participating chapter with available published quiz
            for (var c : ctx.syllabusChapters()) {
                if (c.participates()) {
                    Long quizId = ctx.availableQuizIdByChapter().get(c.chapterId());
                    if (quizId != null) {
                        RecommendationTarget target = new RecommendationTarget(
                                "QUIZ",
                                c.chapterId(),
                                c.chapterName(),
                                c.subjectId(),
                                quizId,
                                null,
                                null,
                                null,
                                null,
                                null
                        );
                        return new Recommendation(
                                RecommendationType.EXPLORE_CHAPTER,
                                "Explore " + c.chapterName(),
                                "Start exploring the syllabus by taking the quiz for " + c.chapterName() + ".",
                                "NO_DATA_EXPLORE_QUIZ",
                                target,
                                List.of()
                        );
                    }
                }
            }

            // If none with quiz: pointing at the first active chapter without a quiz target
            if (!ctx.syllabusChapters().isEmpty()) {
                var first = ctx.syllabusChapters().get(0);
                RecommendationTarget target = new RecommendationTarget(
                        "CHAPTER",
                        first.chapterId(),
                        first.chapterName(),
                        first.subjectId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );
                return new Recommendation(
                        RecommendationType.EXPLORE_CHAPTER,
                        "Explore " + first.chapterName(),
                        "Start your learning journey with " + first.chapterName() + ".",
                        "NO_DATA_EXPLORE_CHAPTER",
                        target,
                        List.of()
                );
            }

            // If no content at all
            RecommendationTarget target = RecommendationTarget.ofKind("SUBJECT_LIST");
            return new Recommendation(
                    RecommendationType.EXPLORE_CHAPTER,
                    "Explore Subjects",
                    "Browse available subjects to begin your study.",
                    "NO_DATA_SUBJECT_LIST",
                    target,
                    List.of()
            );
        }

        // Rule 2: WEAKEST CHAPTER(S)
        List<ChapterPerformanceDto> weakChapters = new ArrayList<>();
        for (var c : ctx.syllabusChapters()) {
            if (c.participates()) {
                ChapterPerformanceDto p = perfMap.get(c.chapterId());
                if (p != null && p.level() == PerformanceLevel.WEAK) {
                    weakChapters.add(p);
                }
            }
        }

        if (!weakChapters.isEmpty()) {
            // Sort: lowest accuracy (ascending), tie -> most recent answer (descending), then lowest chapterId
            weakChapters.sort((a, b) -> {
                double accA = a.accuracy() != null ? a.accuracy() : 0.0;
                double accB = b.accuracy() != null ? b.accuracy() : 0.0;
                int cmp = Double.compare(accA, accB);
                if (cmp != 0) return cmp;

                OffsetDateTime timeA = ctx.mostRecentAnswerByChapter().get(a.chapterId());
                OffsetDateTime timeB = ctx.mostRecentAnswerByChapter().get(b.chapterId());
                if (timeA != null || timeB != null) {
                    if (timeA == null) return 1;
                    if (timeB == null) return -1;
                    int tCmp = timeB.compareTo(timeA); // Most recent first
                    if (tCmp != 0) return tCmp;
                }

                return Long.compare(a.chapterId(), b.chapterId());
            });

            for (ChapterPerformanceDto weak : weakChapters) {
                RecommendationAlternative rec = evaluateWeakChapter(weak, ctx, syllabusMap.get(weak.chapterId()));
                if (rec != null) {
                    candidates.add(rec);
                }
            }
        }

        // Rule 3: NEXT UNASSESSED CHAPTER (first participating chapter with INSUFFICIENT_DATA in syllabus order)
        for (var c : ctx.syllabusChapters()) {
            if (c.participates()) {
                ChapterPerformanceDto p = perfMap.get(c.chapterId());
                if (p == null || p.level() == PerformanceLevel.INSUFFICIENT_DATA) {
                    Long quizId = ctx.availableQuizIdByChapter().get(c.chapterId());
                    RecommendationTarget target;
                    if (quizId != null) {
                        target = new RecommendationTarget("QUIZ", c.chapterId(), c.chapterName(), c.subjectId(), quizId, null, null, null, null, null);
                    } else {
                        target = new RecommendationTarget("CHAPTER", c.chapterId(), c.chapterName(), c.subjectId(), null, null, null, null, null, null);
                    }
                    candidates.add(new RecommendationAlternative(
                            RecommendationType.NEXT_CHAPTER,
                            "Next: " + c.chapterName(),
                            "Continue through the syllabus by studying " + c.chapterName() + ".",
                            "NEXT_UNASSESSED_CHAPTER",
                            target
                    ));
                    break; // Only the first unassessed chapter
                }
            }
        }

        // Rule 4: DEVELOPING (lowest-accuracy DEVELOPING chapter with an available quiz)
        List<ChapterPerformanceDto> devChapters = new ArrayList<>();
        for (var c : ctx.syllabusChapters()) {
            if (c.participates()) {
                ChapterPerformanceDto p = perfMap.get(c.chapterId());
                if (p != null && p.level() == PerformanceLevel.DEVELOPING && ctx.availableQuizIdByChapter().containsKey(c.chapterId())) {
                    devChapters.add(p);
                }
            }
        }
        if (!devChapters.isEmpty()) {
            devChapters.sort(Comparator.comparingDouble(p -> p.accuracy() != null ? p.accuracy() : 0.0));
            ChapterPerformanceDto lowestDev = devChapters.get(0);
            Long qId = ctx.availableQuizIdByChapter().get(lowestDev.chapterId());
            RecommendationTarget target = new RecommendationTarget(
                    "QUIZ",
                    lowestDev.chapterId(),
                    lowestDev.chapterName(),
                    lowestDev.subjectId(),
                    qId,
                    null,
                    null,
                    null,
                    null,
                    ctx.weakTopicsByChapter().get(lowestDev.chapterId())
            );
            candidates.add(new RecommendationAlternative(
                    RecommendationType.TAKE_CHAPTER_QUIZ,
                    "Practice " + lowestDev.chapterName(),
                    "Keep practising " + lowestDev.chapterName() + " to reach strong proficiency.",
                    "DEVELOPING_PRACTICE",
                    target
            ));
        }

        // Rule 5: ALL STRONG
        boolean allStrong = true;
        List<ChapterPerformanceDto> strongChapters = new ArrayList<>();
        int participatingCount = 0;
        for (var c : ctx.syllabusChapters()) {
            if (c.participates()) {
                participatingCount++;
                ChapterPerformanceDto p = perfMap.get(c.chapterId());
                if (p == null || p.level() != PerformanceLevel.STRONG) {
                    allStrong = false;
                } else {
                    strongChapters.add(p);
                }
            }
        }

        if (participatingCount > 0 && allStrong) {
            if (ctx.emailVerified()) {
                candidates.add(new RecommendationAlternative(
                        RecommendationType.TAKE_OVERALL_QUIZ,
                        "Comprehensive Assessment",
                        "You have achieved strong proficiency across all chapters. Take the overall quiz to test your mastery.",
                        "ALL_STRONG_OVERALL_QUIZ",
                        RecommendationTarget.ofKind("OVERALL_QUIZ")
                ));
            } else {
                // Oldest-practised strong chapter quiz
                strongChapters.sort((a, b) -> {
                    OffsetDateTime timeA = ctx.mostRecentAnswerByChapter().get(a.chapterId());
                    OffsetDateTime timeB = ctx.mostRecentAnswerByChapter().get(b.chapterId());
                    if (timeA == null && timeB == null) return Long.compare(a.chapterId(), b.chapterId());
                    if (timeA == null) return -1;
                    if (timeB == null) return 1;
                    return timeA.compareTo(timeB); // Oldest first
                });

                for (ChapterPerformanceDto sc : strongChapters) {
                    Long quizId = ctx.availableQuizIdByChapter().get(sc.chapterId());
                    if (quizId != null) {
                        candidates.add(new RecommendationAlternative(
                                RecommendationType.TAKE_CHAPTER_QUIZ,
                                "Refresh " + sc.chapterName(),
                                "Keep your skills sharp by retaking the quiz for " + sc.chapterName() + ".",
                                "ALL_STRONG_REFRESH",
                                new RecommendationTarget("QUIZ", sc.chapterId(), sc.chapterName(), sc.subjectId(), quizId, null, null, null, null, null)
                        ));
                        break;
                    }
                }
            }
        }

        // General alternative: whenever practiceable mistakes exist overall
        if (ctx.mistakeSummary() != null && ctx.mistakeSummary().totalPracticeable() > 0) {
            candidates.add(new RecommendationAlternative(
                    RecommendationType.PRACTICE_MISTAKES,
                    "Practice Mistakes",
                    "You have " + ctx.mistakeSummary().totalPracticeable() + " questions ready for mistake review.",
                    "OVERALL_MISTAKES",
                    RecommendationTarget.ofKind("MISTAKES")
            ));
        }

        // Build final Recommendation: first candidate is main, up to 3 distinct targets fill alternatives
        if (candidates.isEmpty()) {
            RecommendationTarget target = RecommendationTarget.ofKind("SUBJECT_LIST");
            return new Recommendation(
                    RecommendationType.EXPLORE_CHAPTER,
                    "Explore Subjects",
                    "Browse available subjects to begin your study.",
                    "DEFAULT_EXPLORE",
                    target,
                    List.of()
            );
        }

        RecommendationAlternative main = candidates.get(0);
        List<RecommendationAlternative> alternatives = new ArrayList<>();
        Set<String> seenTargets = new HashSet<>();
        seenTargets.add(targetKey(main.target()));

        for (int i = 1; i < candidates.size() && alternatives.size() < 3; i++) {
            RecommendationAlternative alt = candidates.get(i);
            String key = targetKey(alt.target());
            if (seenTargets.add(key)) {
                alternatives.add(alt);
            }
        }

        return new Recommendation(
                main.type(),
                main.title(),
                main.reason(),
                main.reasonCode(),
                main.target(),
                alternatives
        );
    }

    private RecommendationAlternative evaluateWeakChapter(
            ChapterPerformanceDto weak,
            RecommendationContext ctx,
            RecommendationContext.ChapterOrderInfo syllabusInfo
    ) {
        Long cid = weak.chapterId();
        String cName = weak.chapterName() != null ? weak.chapterName() : (syllabusInfo != null ? syllabusInfo.chapterName() : "Chapter " + cid);
        Long sId = weak.subjectId() != null ? weak.subjectId() : (syllabusInfo != null ? syllabusInfo.subjectId() : null);
        List<String> weakTopics = ctx.weakTopicsByChapter().getOrDefault(cid, List.of());
        double accuracy = weak.accuracy() != null ? weak.accuracy() : 0.0;

        List<LearningResourceInfo> resources = ctx.resourcesByChapter().getOrDefault(cid, List.of());
        LearningResourceInfo preferredResource = pickPreferredResource(resources);

        int mistakes = ctx.activePracticeableMistakesByChapter().getOrDefault(cid, 0);
        Long quizId = ctx.availableQuizIdByChapter().get(cid);

        // 2a: accuracy < recommendationVeryWeakAccuracy AND chapter has published resource -> REVIEW_RESOURCE
        if (accuracy < ctx.veryWeakAccuracyThreshold() && preferredResource != null) {
            RecommendationTarget target = new RecommendationTarget(
                    "PRODUCT",
                    cid,
                    cName,
                    sId,
                    null,
                    preferredResource.productId(),
                    preferredResource.slug(),
                    preferredResource.productType(),
                    preferredResource.owned(),
                    weakTopics
            );
            return new RecommendationAlternative(
                    RecommendationType.REVIEW_RESOURCE,
                    "Review " + preferredResource.name(),
                    "Your accuracy in " + cName + " is low (" + Math.round(accuracy * 100) + "%). Reviewing this resource will help strengthen your foundation.",
                    "WEAK_REVIEW_RESOURCE",
                    target
            );
        }

        // 2b: else student has >= minMistakesForPractice practiceable active mistakes -> PRACTICE_MISTAKES
        if (mistakes >= ctx.minMistakesForPractice()) {
            RecommendationTarget target = new RecommendationTarget(
                    "MISTAKES",
                    cid,
                    cName,
                    sId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    weakTopics
            );
            return new RecommendationAlternative(
                    RecommendationType.PRACTICE_MISTAKES,
                    "Practice Mistakes in " + cName,
                    "You have " + mistakes + " mistakes to review in " + cName + " to reinforce your understanding.",
                    "WEAK_PRACTICE_MISTAKES",
                    target
            );
        }

        // 2c: else a published resource exists -> REVIEW_RESOURCE
        if (preferredResource != null) {
            RecommendationTarget target = new RecommendationTarget(
                    "PRODUCT",
                    cid,
                    cName,
                    sId,
                    null,
                    preferredResource.productId(),
                    preferredResource.slug(),
                    preferredResource.productType(),
                    preferredResource.owned(),
                    weakTopics
            );
            return new RecommendationAlternative(
                    RecommendationType.REVIEW_RESOURCE,
                    "Study " + preferredResource.name(),
                    "Review " + preferredResource.name() + " to improve your performance in " + cName + ".",
                    "WEAK_RESOURCE_AVAILABLE",
                    target
            );
        }

        // 2d: else chapter quiz is available -> TAKE_CHAPTER_QUIZ
        if (quizId != null) {
            RecommendationTarget target = new RecommendationTarget(
                    "QUIZ",
                    cid,
                    cName,
                    sId,
                    quizId,
                    null,
                    null,
                    null,
                    null,
                    weakTopics
            );
            return new RecommendationAlternative(
                    RecommendationType.TAKE_CHAPTER_QUIZ,
                    "Retake " + cName + " Quiz",
                    "Take the " + cName + " quiz again to practice and improve your score.",
                    "WEAK_TAKE_QUIZ",
                    target
            );
        }

        return null;
    }

    private LearningResourceInfo pickPreferredResource(List<LearningResourceInfo> resources) {
        if (resources == null || resources.isEmpty()) return null;
        // Prefer: 1. owned VIDEO, 2. NOTE, 3. any published
        for (var r : resources) {
            if ("VIDEO".equalsIgnoreCase(r.productType()) && r.owned()) {
                return r;
            }
        }
        for (var r : resources) {
            if ("PHYSICAL_NOTE".equalsIgnoreCase(r.productType())) {
                return r;
            }
        }
        return resources.get(0);
    }

    private String targetKey(RecommendationTarget t) {
        if (t == null) return "null";
        return t.kind() + ":" + t.chapterId() + ":" + t.productId() + ":" + t.quizId();
    }
}

