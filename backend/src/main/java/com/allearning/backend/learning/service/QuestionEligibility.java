package com.allearning.backend.learning.service;

import com.allearning.backend.learning.entity.Question;
import com.allearning.backend.learning.repository.QuestionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Transactional(readOnly = true)
public class QuestionEligibility {

    private final QuestionRepository questionRepository;

    public QuestionEligibility(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public boolean isChapterParticipating(Long chapterId) {
        return chapterId != null && countEligible(chapterId, null) >= 5L;
    }

    public boolean isQuestionEligible(Question question) {
        if (question == null || question.getStatus() == null
                || question.getStatus().name().equals("PENDING")
                || question.getTopic() == null
                || question.getTopic().getStatus() == null
                || !question.getTopic().getStatus().name().equals("ACTIVE")) {
            return false;
        }
        return question.getStatus().name().equals("APPROVED");
    }

    public long countEligible(Long chapterId, String language) {
        String lang = (language != null && !language.isBlank()) ? language.trim().toUpperCase() : null;
        return questionRepository.countEligibleForChapter(chapterId, lang);
    }

    public Map<String, Long> eligibleLanguageCounts(Long chapterId) {
        List<Object[]> rows = questionRepository.countEligibleByLanguageForChapter(chapterId);
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            String lang = (String) row[0];
            Long count = (Long) row[1];
            if (lang != null) {
                result.put(lang.toUpperCase(), count);
            }
        }
        return result;
    }

    public List<Long> listEligibleIds(Long chapterId, String language) {
        String lang = (language != null && !language.isBlank()) ? language.trim().toUpperCase() : null;
        return questionRepository.findEligibleIdsForChapter(chapterId, lang);
    }

    public List<Long> filterEligible(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        return questionRepository.filterEligibleIds(questionIds);
    }

    public List<Question> filterEligibleQuestions(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        return questionRepository.filterEligibleQuestions(questionIds);
    }
}
