package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.dto.*;
import com.allearning.backend.learning.entity.ChapterTopic;
import com.allearning.backend.learning.repository.ChapterTopicRepository;
import com.allearning.backend.learning.repository.QuizAnswerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final QuizAnswerRepository quizAnswerRepository;
    private final ChapterValidationService chapterValidationService;
    private final SubjectLookupService subjectLookupService;
    private final ChapterTopicRepository chapterTopicRepository;
    private final QuestionEligibility questionEligibility;
    private final PerformanceCalculator performanceCalculator;
    private final LearningProperties learningProperties;

    public AnalyticsService(
            QuizAnswerRepository quizAnswerRepository,
            ChapterValidationService chapterValidationService,
            SubjectLookupService subjectLookupService,
            ChapterTopicRepository chapterTopicRepository,
            QuestionEligibility questionEligibility,
            PerformanceCalculator performanceCalculator,
            LearningProperties learningProperties
    ) {
        this.quizAnswerRepository = quizAnswerRepository;
        this.chapterValidationService = chapterValidationService;
        this.subjectLookupService = subjectLookupService;
        this.chapterTopicRepository = chapterTopicRepository;
        this.questionEligibility = questionEligibility;
        this.performanceCalculator = performanceCalculator;
        this.learningProperties = learningProperties;
    }

    public List<ChapterPerformanceDto> getChapterPerformances(Long userId, Long subjectId) {
        List<Long> activeChapterIds;
        if (subjectId != null) {
            activeChapterIds = chapterValidationService.getActiveChapterIdsBySubject(subjectId);
        } else {
            activeChapterIds = chapterValidationService.getActiveChapterIds();
        }

        if (activeChapterIds.isEmpty()) {
            return List.of();
        }

        // Load all graded answers for the user, pre-sorted most recent first
        List<Object[]> rows = quizAnswerRepository.findAllUserGradedAnswers(userId);

        // Group by chapterId
        Map<Long, List<Boolean>> chapterAnswers = new HashMap<>();
        for (Object[] row : rows) {
            Long chapId = ((Number) row[0]).longValue();
            Boolean isCorrect = (Boolean) row[2];
            chapterAnswers.computeIfAbsent(chapId, k -> new ArrayList<>()).add(isCorrect);
        }

        List<ChapterPerformanceDto> result = new ArrayList<>();
        for (Long chapterId : activeChapterIds) {
            var optSummary = chapterValidationService.getChapterSummary(chapterId);
            if (optSummary.isEmpty()) continue;

            String chapterName = optSummary.get().name();
            Long qSubjectId = optSummary.get().subjectId();
            String subjectName = null;
            if (qSubjectId != null) {
                var optSubject = subjectLookupService.getSubjectInfo(qSubjectId);
                if (optSubject.isPresent()) {
                    subjectName = optSubject.get().name();
                }
            }

            long eligibleCount = questionEligibility.countEligible(chapterId, null);
            boolean participates = eligibleCount >= learningProperties.getMinEligibleQuestions();

            List<Boolean> answers = chapterAnswers.getOrDefault(chapterId, List.of());
            PerformanceCalculator.PerformanceResult calc = performanceCalculator.calculate(
                    answers,
                    PerformanceScope.CHAPTER,
                    learningProperties
            );

            result.add(new ChapterPerformanceDto(
                    chapterId,
                    chapterName,
                    qSubjectId,
                    subjectName,
                    participates,
                    calc.answerCount(),
                    calc.accuracy(),
                    calc.level(),
                    calc.trend()
            ));
        }

        return result;
    }

    public ChapterAnalyticsDetailResponse getChapterDetail(Long userId, Long chapterId) {
        var optSummary = chapterValidationService.getChapterSummary(chapterId);
        if (optSummary.isEmpty() || !chapterValidationService.isChapterActive(chapterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found or inactive: " + chapterId);
        }

        String chapterName = optSummary.get().name();
        Long subjectId = optSummary.get().subjectId();
        String subjectName = null;
        if (subjectId != null) {
            var optSubject = subjectLookupService.getSubjectInfo(subjectId);
            if (optSubject.isPresent()) {
                subjectName = optSubject.get().name();
            }
        }

        long eligibleCount = questionEligibility.countEligible(chapterId, null);
        boolean participates = eligibleCount >= learningProperties.getMinEligibleQuestions();

        List<Object[]> rows = quizAnswerRepository.findAllUserGradedAnswers(userId);

        List<Boolean> chapterAnswers = new ArrayList<>();
        Map<Long, List<Boolean>> topicAnswers = new HashMap<>();

        for (Object[] row : rows) {
            Long chapId = ((Number) row[0]).longValue();
            if (chapId.equals(chapterId)) {
                Boolean isCorrect = (Boolean) row[2];
                chapterAnswers.add(isCorrect);

                if (row[1] != null) {
                    Long tid = ((Number) row[1]).longValue();
                    topicAnswers.computeIfAbsent(tid, k -> new ArrayList<>()).add(isCorrect);
                }
            }
        }

        PerformanceCalculator.PerformanceResult chapterCalc = performanceCalculator.calculate(
                chapterAnswers,
                PerformanceScope.CHAPTER,
                learningProperties
        );

        ChapterPerformanceDto chapterDto = new ChapterPerformanceDto(
                chapterId,
                chapterName,
                subjectId,
                subjectName,
                participates,
                chapterCalc.answerCount(),
                chapterCalc.accuracy(),
                chapterCalc.level(),
                chapterCalc.trend()
        );

        // Topics that have at least one graded answer
        List<TopicPerformanceDto> topicDtos = new ArrayList<>();
        if (!topicAnswers.isEmpty()) {
            Map<Long, String> topicNames = chapterTopicRepository.findByTopicIdIn(topicAnswers.keySet()).stream()
                    .collect(Collectors.toMap(ChapterTopic::getTopicId, ChapterTopic::getName));

            for (Map.Entry<Long, List<Boolean>> entry : topicAnswers.entrySet()) {
                Long topicId = entry.getKey();
                List<Boolean> tAns = entry.getValue();

                PerformanceCalculator.PerformanceResult topicCalc = performanceCalculator.calculate(
                        tAns,
                        PerformanceScope.TOPIC,
                        learningProperties
                );

                topicDtos.add(new TopicPerformanceDto(
                        topicId,
                        topicNames.getOrDefault(topicId, "Topic " + topicId),
                        topicCalc.answerCount(),
                        topicCalc.accuracy(),
                        topicCalc.level(),
                        null // trend is null for topics per spec
                ));
            }
        }

        return new ChapterAnalyticsDetailResponse(chapterDto, topicDtos);
    }

    public WeakAreasResponse getWeakAreas(Long userId) {
        List<ChapterPerformanceDto> chapterPerfs = getChapterPerformances(userId, null);
        List<WeakAreaItemDto> items = new ArrayList<>();

        // 1. Weak participating chapters
        for (ChapterPerformanceDto cp : chapterPerfs) {
            if (cp.participates() && cp.level() == PerformanceLevel.WEAK) {
                items.add(new WeakAreaItemDto(
                        "CHAPTER",
                        cp.chapterId(),
                        cp.chapterName(),
                        cp.chapterId(),
                        cp.chapterName(),
                        cp.answerCount(),
                        cp.accuracy(),
                        cp.level()
                ));
            }
        }

        // 2. Weak topics inside participating chapters
        Set<Long> participatingChapterIds = chapterPerfs.stream()
                .filter(ChapterPerformanceDto::participates)
                .map(ChapterPerformanceDto::chapterId)
                .collect(Collectors.toSet());

        if (!participatingChapterIds.isEmpty()) {
            List<Object[]> rows = quizAnswerRepository.findAllUserGradedAnswers(userId);

            Map<Long, List<Boolean>> topicAnswers = new HashMap<>();
            Map<Long, Long> topicToChapter = new HashMap<>();

            for (Object[] row : rows) {
                Long chapId = ((Number) row[0]).longValue();
                if (participatingChapterIds.contains(chapId) && row[1] != null) {
                    Long tid = ((Number) row[1]).longValue();
                    Boolean isCorrect = (Boolean) row[2];
                    topicAnswers.computeIfAbsent(tid, k -> new ArrayList<>()).add(isCorrect);
                    topicToChapter.putIfAbsent(tid, chapId);
                }
            }

            if (!topicAnswers.isEmpty()) {
                Map<Long, ChapterTopic> topicEntities = chapterTopicRepository.findByTopicIdIn(topicAnswers.keySet()).stream()
                        .collect(Collectors.toMap(ChapterTopic::getTopicId, t -> t));

                Map<Long, String> chapterNames = chapterValidationService.getChapterNames(participatingChapterIds);

                for (Map.Entry<Long, List<Boolean>> entry : topicAnswers.entrySet()) {
                    Long topicId = entry.getKey();
                    List<Boolean> tAns = entry.getValue();

                    PerformanceCalculator.PerformanceResult tc = performanceCalculator.calculate(
                            tAns,
                            PerformanceScope.TOPIC,
                            learningProperties
                    );

                    if (tc.level() == PerformanceLevel.WEAK) {
                        Long chapId = topicToChapter.get(topicId);
                        String tName = topicEntities.containsKey(topicId) ? topicEntities.get(topicId).getName() : "Topic " + topicId;
                        String cName = chapterNames.getOrDefault(chapId, "Chapter " + chapId);

                        items.add(new WeakAreaItemDto(
                                "TOPIC",
                                topicId,
                                tName,
                                chapId,
                                cName,
                                tc.answerCount(),
                                tc.accuracy(),
                                tc.level()
                        ));
                    }
                }
            }
        }

        // Sort by accuracy ascending (lowest accuracy first)
        items.sort(Comparator.comparingDouble(item -> item.accuracy() != null ? item.accuracy() : 0.0));

        return new WeakAreasResponse(items);
    }

    public SyllabusProgressResponse getProgress(Long userId, Long subjectId) {
        List<SubjectLookupService.SubjectBasicInfo> activeSubjects;
        if (subjectId != null) {
            var infoOpt = subjectLookupService.getSubjectInfo(subjectId);
            if (infoOpt.isPresent() && infoOpt.get().isActive()) {
                activeSubjects = List.of(infoOpt.get());
            } else {
                activeSubjects = List.of();
            }
        } else {
            List<Long> activeSubjectIds = subjectLookupService.getActiveSubjectIds();
            activeSubjects = subjectLookupService.getSubjectsInfo(activeSubjectIds).values().stream()
                    .filter(SubjectLookupService.SubjectBasicInfo::isActive)
                    .sorted(Comparator.comparing(SubjectLookupService.SubjectBasicInfo::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        if (activeSubjects.isEmpty()) {
            ProgressSummary emptySummary = new ProgressSummary(0, 0, 0, 0, 0, 0, 0, null, null);
            return new SyllabusProgressResponse(emptySummary, List.of());
        }

        List<SubjectProgressDto> subjectDtos = new ArrayList<>();
        int ovTotal = 0, ovPart = 0, ovWeak = 0, ovDev = 0, ovStrong = 0, ovNed = 0;
        int ovPercentSum = 0;

        for (var subj : activeSubjects) {
            List<Long> chapIds = chapterValidationService.getActiveChapterIdsBySubject(subj.subjectId());
            if (chapIds.isEmpty()) {
                ProgressSummary subjSummary = new ProgressSummary(0, 0, 0, 0, 0, 0, 0, null, null);
                subjectDtos.add(new SubjectProgressDto(subj.subjectId(), subj.name(), subjSummary, List.of()));
                continue;
            }

            var chapterSummaries = new ArrayList<>(
                    chapterValidationService.getPublicChapterSummaries(chapIds)
            );
            chapterSummaries.sort(Comparator.comparing(ChapterValidationService.PublicChapterSummary::chapterOrder, Comparator.nullsLast(Integer::compareTo)));

            // Performance map for this subject
            Map<Long, ChapterPerformanceDto> perfMap = getChapterPerformances(userId, subj.subjectId()).stream()
                    .collect(Collectors.toMap(ChapterPerformanceDto::chapterId, p -> p));

            List<ChapterProgressDto> chapterDtos = new ArrayList<>();
            int subPart = 0, subWeak = 0, subDev = 0, subStrong = 0, subNed = 0;
            int subPercentSum = 0;

            for (var cSum : chapterSummaries) {
                Long cid = cSum.chapterId();
                ChapterPerformanceDto perf = perfMap.get(cid);

                String state;
                Integer percent = null;
                Double accuracy = null;
                PerformanceLevel level = PerformanceLevel.INSUFFICIENT_DATA;
                PerformanceTrend trend = PerformanceTrend.STEADY;
                int answerCount = 0;

                boolean participates = perf != null
                        ? perf.participates()
                        : questionEligibility.countEligible(cid, null) >= learningProperties.getMinEligibleQuestions();

                if (!participates) {
                    state = "NO_QUESTIONS";
                } else {
                    subPart++;
                    ovPart++;
                    if (perf != null) {
                        accuracy = perf.accuracy();
                        level = perf.level();
                        trend = perf.trend();
                        answerCount = perf.answerCount();
                    }

                    if (level == PerformanceLevel.INSUFFICIENT_DATA) {
                        state = "NOT_ENOUGH_DATA";
                        subNed++;
                        ovNed++;
                    } else {
                        state = level.name();
                        if (accuracy != null) {
                            percent = (int) Math.round(accuracy * 100.0);
                        } else {
                            percent = 0;
                        }
                        subPercentSum += percent;
                        ovPercentSum += percent;

                        if (level == PerformanceLevel.WEAK) {
                            subWeak++;
                            ovWeak++;
                        } else if (level == PerformanceLevel.DEVELOPING) {
                            subDev++;
                            ovDev++;
                        } else if (level == PerformanceLevel.STRONG) {
                            subStrong++;
                            ovStrong++;
                        }
                    }
                }

                chapterDtos.add(new ChapterProgressDto(
                        cid,
                        cSum.name(),
                        cSum.chapterOrder(),
                        state,
                        answerCount,
                        accuracy,
                        percent,
                        level,
                        trend
                ));
            }

            int subTotal = chapterSummaries.size();
            ovTotal += subTotal;
            int subAssessed = subWeak + subDev + subStrong;
            Double subCoverage = subPart > 0 ? (subAssessed * 100.0 / subPart) : null;
            Double subAvgPercent = subAssessed > 0 ? ((double) subPercentSum / subAssessed) : null;

            ProgressSummary subjSummary = new ProgressSummary(
                    subTotal,
                    subPart,
                    subAssessed,
                    subWeak,
                    subDev,
                    subStrong,
                    subNed,
                    subCoverage,
                    subAvgPercent
            );

            subjectDtos.add(new SubjectProgressDto(subj.subjectId(), subj.name(), subjSummary, chapterDtos));
        }

        int ovAssessed = ovWeak + ovDev + ovStrong;
        Double ovCoverage = ovPart > 0 ? (ovAssessed * 100.0 / ovPart) : null;
        Double ovAvgPercent = ovAssessed > 0 ? ((double) ovPercentSum / ovAssessed) : null;

        ProgressSummary ovSummary = new ProgressSummary(
                ovTotal,
                ovPart,
                ovAssessed,
                ovWeak,
                ovDev,
                ovStrong,
                ovNed,
                ovCoverage,
                ovAvgPercent
        );

        return new SyllabusProgressResponse(ovSummary, subjectDtos);
    }
}
