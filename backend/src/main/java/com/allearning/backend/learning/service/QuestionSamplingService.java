package com.allearning.backend.learning.service;

import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.entity.MistakeRecord;
import com.allearning.backend.learning.entity.MistakeStatus;
import com.allearning.backend.learning.entity.Question;
import com.allearning.backend.learning.entity.QuestionDifficulty;
import com.allearning.backend.learning.exception.NotEnoughQuestionsException;
import com.allearning.backend.learning.repository.DiagnosticAssessmentAnswerRepository;
import com.allearning.backend.learning.repository.MistakeRecordRepository;
import com.allearning.backend.learning.repository.QuizAnswerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class QuestionSamplingService {

    private final QuestionEligibility questionEligibility;
    private final MistakeRecordRepository mistakeRecordRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final DiagnosticAssessmentAnswerRepository diagnosticAssessmentAnswerRepository;
    private final LearningProperties learningProperties;
    private final RandomGenerator randomGenerator;

    public QuestionSamplingService(
            QuestionEligibility questionEligibility,
            MistakeRecordRepository mistakeRecordRepository,
            QuizAnswerRepository quizAnswerRepository,
            DiagnosticAssessmentAnswerRepository diagnosticAssessmentAnswerRepository,
            LearningProperties learningProperties,
            RandomGenerator randomGenerator
    ) {
        this.questionEligibility = questionEligibility;
        this.mistakeRecordRepository = mistakeRecordRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.diagnosticAssessmentAnswerRepository = diagnosticAssessmentAnswerRepository;
        this.learningProperties = learningProperties;
        this.randomGenerator = randomGenerator;
    }

    public List<Question> sampleForChapter(Long userId, Long chapterId, String languageOrNull, int count) {
        if (count <= 0) {
            return List.of();
        }

        List<Long> eligibleIds = questionEligibility.listEligibleIds(chapterId, languageOrNull);
        if (eligibleIds.size() < count) {
            throw new NotEnoughQuestionsException(
                    count,
                    eligibleIds.size(),
                    chapterId,
                    "Not enough eligible questions in chapter " + chapterId
            );
        }

        List<Question> eligibleQuestions = questionEligibility.filterEligibleQuestions(eligibleIds);
        return doSampleQuestions(userId, eligibleQuestions, count);
    }

    public List<Question> sampleForAssessment(
            Long userId,
            Collection<Long> scopeChapterIds,
            String languageOrNull,
            int perChapterCount,
            int maxTotal
    ) {
        if (scopeChapterIds == null || scopeChapterIds.isEmpty() || perChapterCount <= 0 || maxTotal <= 0) {
            return List.of();
        }

        List<List<Question>> perChapterSampled = new ArrayList<>();
        for (Long chapterId : scopeChapterIds) {
            List<Long> eligibleIds = questionEligibility.listEligibleIds(chapterId, languageOrNull);
            if (eligibleIds.size() < perChapterCount) {
                throw new NotEnoughQuestionsException(
                        perChapterCount,
                        eligibleIds.size(),
                        chapterId,
                        "Not enough eligible questions in chapter " + chapterId + " for assessment"
                );
            }
            List<Question> eligibleQuestions = questionEligibility.filterEligibleQuestions(eligibleIds);
            List<Question> sampled = doSampleQuestions(userId, eligibleQuestions, perChapterCount);
            perChapterSampled.add(new ArrayList<>(sampled));
        }

        // Cap at maxTotal taking round-robin across chapters
        List<Question> result = new ArrayList<>();
        boolean addedAny = true;
        while (result.size() < maxTotal && addedAny) {
            addedAny = false;
            for (List<Question> list : perChapterSampled) {
                if (!list.isEmpty() && result.size() < maxTotal) {
                    result.add(list.remove(0));
                    addedAny = true;
                }
            }
        }

        return result;
    }

    private List<Question> doSampleQuestions(Long userId, List<Question> eligibleQuestions, int count) {
        if (eligibleQuestions.size() <= count) {
            List<Question> copy = new ArrayList<>(eligibleQuestions);
            shuffle(copy);
            return copy;
        }

        // Calculate target difficulty counts
        int targetEasy = (int) Math.round(count * (learningProperties.getDifficultyEasyPercent() / 100.0));
        int targetHard = (int) Math.round(count * (learningProperties.getDifficultyHardPercent() / 100.0));
        int targetMed = count - targetEasy - targetHard;
        if (targetMed < 0) {
            targetMed = 0;
            targetHard = count - targetEasy;
        }

        // Determine user history if userId != null
        Set<Long> activeMistakeIds = new LinkedHashSet<>();
        Map<Long, OffsetDateTime> lastAnsweredMap = new HashMap<>();

        if (userId != null) {
            List<MistakeRecord> mistakes = mistakeRecordRepository.findByUserIdAndStatusOrderByLastAnsweredAtDesc(userId, MistakeStatus.ACTIVE);
            for (MistakeRecord mr : mistakes) {
                activeMistakeIds.add(mr.getQuestionId());
            }

            Set<Long> questionIds = eligibleQuestions.stream().map(Question::getQuestionId).collect(Collectors.toSet());
            List<Object[]> quizHistory = quizAnswerRepository.findLastAnsweredTimesForUser(userId, questionIds);
            for (Object[] row : quizHistory) {
                Long qId = (Long) row[0];
                OffsetDateTime dt = (OffsetDateTime) row[1];
                lastAnsweredMap.put(qId, dt);
            }

            List<Object[]> diagHistory = diagnosticAssessmentAnswerRepository.findLastAnsweredTimesForUser(userId, questionIds);
            for (Object[] row : diagHistory) {
                Long qId = (Long) row[0];
                OffsetDateTime dt = (OffsetDateTime) row[1];
                OffsetDateTime existing = lastAnsweredMap.get(qId);
                if (existing == null || (dt != null && dt.isAfter(existing))) {
                    lastAnsweredMap.put(qId, dt);
                }
            }
        }

        int maxMistakes = (userId != null)
                ? (int) Math.round(count * (learningProperties.getMistakeWeightPercent() / 100.0))
                : 0;

        // Group eligible questions by topic
        Map<Long, List<Question>> topicMap = new LinkedHashMap<>();
        for (Question q : eligibleQuestions) {
            Long tid = q.getTopicId() != null ? q.getTopicId() : -1L;
            topicMap.computeIfAbsent(tid, k -> new ArrayList<>()).add(q);
        }

        // For each topic, sort candidates by tier (Tier 1 mistakes, Tier 2 unattempted, Tier 3 least-recently attempted)
        for (List<Question> list : topicMap.values()) {
            shuffle(list);
            list.sort((q1, q2) -> {
                int t1 = getTier(q1.getQuestionId(), activeMistakeIds, lastAnsweredMap);
                int t2 = getTier(q2.getQuestionId(), activeMistakeIds, lastAnsweredMap);
                if (t1 != t2) {
                    return Integer.compare(t1, t2);
                }
                if (t1 == 3) {
                    OffsetDateTime d1 = lastAnsweredMap.get(q1.getQuestionId());
                    OffsetDateTime d2 = lastAnsweredMap.get(q2.getQuestionId());
                    if (d1 != null && d2 != null) {
                        return d1.compareTo(d2); // least-recently answered first
                    }
                }
                return 0;
            });
        }

        // Topic round-robin selection with difficulty quotas
        List<Question> selected = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();
        int easyCount = 0;
        int medCount = 0;
        int hardCount = 0;
        int mistakesSelected = 0;

        List<Long> topicIds = new ArrayList<>(topicMap.keySet());
        shuffle(topicIds);

        int topicIndex = 0;
        int consecutiveEmpty = 0;

        while (selected.size() < count && consecutiveEmpty < topicIds.size()) {
            Long tid = topicIds.get(topicIndex % topicIds.size());
            List<Question> candidates = topicMap.get(tid);

            Question chosen = null;
            if (candidates != null && !candidates.isEmpty()) {
                // Determine preferred difficulty needed
                QuestionDifficulty preferredDiff = null;
                int easyNeeded = targetEasy - easyCount;
                int medNeeded = targetMed - medCount;
                int hardNeeded = targetHard - hardCount;

                if (medNeeded >= easyNeeded && medNeeded >= hardNeeded && medNeeded > 0) {
                    preferredDiff = QuestionDifficulty.MEDIUM;
                } else if (easyNeeded >= hardNeeded && easyNeeded > 0) {
                    preferredDiff = QuestionDifficulty.EASY;
                } else if (hardNeeded > 0) {
                    preferredDiff = QuestionDifficulty.HARD;
                }

                // Find candidate matching preferred difficulty (respecting mistake cap)
                for (int i = 0; i < candidates.size(); i++) {
                    Question candidate = candidates.get(i);
                    boolean isMistake = activeMistakeIds.contains(candidate.getQuestionId());
                    if (isMistake && mistakesSelected >= maxMistakes) {
                        continue;
                    }
                    if (preferredDiff == null || candidate.getDifficulty() == preferredDiff) {
                        chosen = candidates.remove(i);
                        break;
                    }
                }

                // Graceful fallback: pick first available candidate
                if (chosen == null) {
                    for (int i = 0; i < candidates.size(); i++) {
                        Question candidate = candidates.get(i);
                        boolean isMistake = activeMistakeIds.contains(candidate.getQuestionId());
                        if (isMistake && mistakesSelected >= maxMistakes) {
                            continue;
                        }
                        chosen = candidates.remove(i);
                        break;
                    }
                }

                // If only mistake remains and we hit mistake cap, pick it anyway if we need questions
                if (chosen == null && !candidates.isEmpty()) {
                    chosen = candidates.remove(0);
                }
            }

            if (chosen != null && selectedIds.add(chosen.getQuestionId())) {
                selected.add(chosen);
                if (chosen.getDifficulty() == QuestionDifficulty.EASY) easyCount++;
                else if (chosen.getDifficulty() == QuestionDifficulty.HARD) hardCount++;
                else medCount++;

                if (activeMistakeIds.contains(chosen.getQuestionId())) {
                    mistakesSelected++;
                }
                consecutiveEmpty = 0;
            } else {
                consecutiveEmpty++;
            }

            topicIndex++;
        }

        // If still need questions (edge case due to duplicates or topic drain), pick from remaining eligible
        if (selected.size() < count) {
            for (Question q : eligibleQuestions) {
                if (selectedIds.add(q.getQuestionId())) {
                    selected.add(q);
                    if (selected.size() == count) break;
                }
            }
        }

        return selected;
    }

    private int getTier(Long questionId, Set<Long> activeMistakeIds, Map<Long, OffsetDateTime> lastAnsweredMap) {
        if (activeMistakeIds.contains(questionId)) {
            return 1;
        }
        if (!lastAnsweredMap.containsKey(questionId)) {
            return 2;
        }
        return 3;
    }

    private <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = randomGenerator.nextInt(i + 1);
            T temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }
}

