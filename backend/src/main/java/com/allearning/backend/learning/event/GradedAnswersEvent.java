package com.allearning.backend.learning.event;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * In-JVM Spring application event published upon completion of any graded submission
 * (chapter quiz, mistake practice, study session, initial diagnostic, overall quiz).
 * <p>
 * Published INSIDE the submit transaction and consumed by synchronous listeners
 * (e.g. {@code MistakeService}) so mistake records remain strictly consistent with stored answers.
 * <p>
 * NOTE FOR FUTURE LISTENERS:
 * Future async listeners (e.g. AI advice, recommendations) must use
 * {@code @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)} + {@code @Async}
 * and must NEVER fail or delay a submission transaction.
 */
public record GradedAnswersEvent(
        Long userId,
        GradedSourceType sourceType,
        Long sourceId,
        Long studySessionId,
        OffsetDateTime completedAt,
        List<GradedAnswerItem> answers
) {
    public record GradedAnswerItem(
            Long questionId,
            Long chapterId,
            Long topicId,
            boolean isCorrect
    ) {}
}

