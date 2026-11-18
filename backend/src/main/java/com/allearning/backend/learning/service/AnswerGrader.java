package com.allearning.backend.learning.service;

import com.allearning.backend.learning.dto.SubmitAnswerItem;
import com.allearning.backend.learning.exception.GradingValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AnswerGrader {

    public record ServedQuestionForGrading(
            Long questionId,
            Long chapterId,
            Long topicId,
            Set<Long> optionIds,
            Long correctOptionId,
            String explanation
    ) {}

    public record GradedAnswerDetail(
            Long questionId,
            Long selectedOptionId,
            Long correctOptionId,
            boolean isCorrect,
            String explanation
    ) {}

    public record GradedResult(
            int score,
            int totalMarks,
            double percentage,
            Map<Long, GradedAnswerDetail> answersByQuestionId
    ) {}

    public GradedResult grade(
            List<ServedQuestionForGrading> servedQuestions,
            List<SubmitAnswerItem> submittedAnswers
    ) {
        if (servedQuestions == null || servedQuestions.isEmpty()) {
            throw new GradingValidationException("INVALID_ANSWER", "No questions to grade");
        }

        Map<Long, ServedQuestionForGrading> servedMap = new LinkedHashMap<>();
        for (ServedQuestionForGrading q : servedQuestions) {
            servedMap.put(q.questionId(), q);
        }

        if (submittedAnswers == null) {
            submittedAnswers = List.of();
        }

        // Check duplicate questionIds in submitted answers
        Set<Long> seenQuestions = new HashSet<>();
        for (SubmitAnswerItem ans : submittedAnswers) {
            if (ans.questionId() == null) {
                throw new GradingValidationException("INVALID_ANSWER", "questionId cannot be null");
            }
            if (!seenQuestions.add(ans.questionId())) {
                throw new GradingValidationException("INVALID_ANSWER", "Duplicate question in submitted answers: " + ans.questionId());
            }
        }

        // Check for questions submitted that were not served
        for (SubmitAnswerItem ans : submittedAnswers) {
            if (!servedMap.containsKey(ans.questionId())) {
                throw new GradingValidationException("INVALID_ANSWER", "Question " + ans.questionId() + " was not part of this attempt");
            }
        }

        // Check all served questions are answered
        List<Long> unansweredIds = new ArrayList<>();
        for (Long servedId : servedMap.keySet()) {
            if (!seenQuestions.contains(servedId)) {
                unansweredIds.add(servedId);
            }
        }
        if (!unansweredIds.isEmpty()) {
            throw new GradingValidationException(
                    HttpStatus.BAD_REQUEST,
                    "UNANSWERED_QUESTIONS",
                    "All questions must be answered",
                    Map.of("unansweredQuestionIds", unansweredIds)
            );
        }

        // Grade each question
        int score = 0;
        Map<Long, GradedAnswerDetail> gradedDetails = new LinkedHashMap<>();

        for (SubmitAnswerItem ans : submittedAnswers) {
            ServedQuestionForGrading served = servedMap.get(ans.questionId());
            if (ans.selectedOptionId() == null) {
                throw new GradingValidationException("INVALID_ANSWER", "selectedOptionId cannot be null for question " + ans.questionId());
            }
            if (!served.optionIds().contains(ans.selectedOptionId())) {
                throw new GradingValidationException("INVALID_ANSWER", "Option " + ans.selectedOptionId() + " does not belong to question " + ans.questionId());
            }

            boolean isCorrect = Objects.equals(ans.selectedOptionId(), served.correctOptionId());
            if (isCorrect) {
                score++;
            }

            gradedDetails.put(ans.questionId(), new GradedAnswerDetail(
                    ans.questionId(),
                    ans.selectedOptionId(),
                    served.correctOptionId(),
                    isCorrect,
                    served.explanation()
            ));
        }

        int totalMarks = servedQuestions.size();
        double percentage = totalMarks > 0 ? (Math.round((score * 100.0 / totalMarks) * 10.0) / 10.0) : 0.0;

        return new GradedResult(score, totalMarks, percentage, gradedDetails);
    }
}

