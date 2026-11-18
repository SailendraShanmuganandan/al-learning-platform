package com.allearning.backend.learning.service;

import com.allearning.backend.learning.config.LearningProperties;
import com.allearning.backend.learning.dto.StudentOptionDto;
import com.allearning.backend.learning.dto.StudentQuestionDto;
import com.allearning.backend.learning.entity.StudySession;
import com.allearning.backend.learning.entity.StudySessionStatus;
import com.allearning.backend.learning.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class Phase6GapTest {

    @Test
    void inProgressStudentQuestionJsonNeverContainsAnswers() throws Exception {
        StudentQuestionDto question = new StudentQuestionDto(
                501L, 1, "Which protocol is reliable?", null, 8L, "Transport",
                3L, "Data Communication",
                List.of(new StudentOptionDto(1501L, "TCP", 1),
                        new StudentOptionDto(1502L, "UDP", 2)));

        String json = new ObjectMapper().writeValueAsString(question);
        assertFalse(json.contains("isCorrect"));
        assertFalse(json.contains("correctOptionId"));
        assertFalse(json.contains("explanation"));
        assertFalse(json.contains("sourceExcerpt"));
        assertFalse(json.contains("selectedOptionId"));
    }

    @Test
    void studySessionEffectiveStatusIncludesGracePeriod() {
        StudySessionService service = studySessionService();
        StudySession session = new StudySession();
        session.setStatus(StudySessionStatus.ACTIVE);
        session.setStartedAt(OffsetDateTime.parse("2026-11-04T10:00:00+05:30"));
        session.setDurationMinutes(20);

        assertEquals(StudySessionStatus.ACTIVE, service.effectiveStatus(
                session, OffsetDateTime.parse("2026-11-04T10:24:59+05:30")));
        assertEquals(StudySessionStatus.ABANDONED, service.effectiveStatus(
                session, OffsetDateTime.parse("2026-11-04T10:25:01+05:30")));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private StudySessionService studySessionService() {
        LearningProperties properties = new LearningProperties();
        properties.setStudySessionGraceMinutes(5);
        return new StudySessionService(
                mock(StudySessionRepository.class),
                mock(StudySessionItemRepository.class),
                mock(QuizAttemptRepository.class),
                mock(QuestionRepository.class),
                mock(QuizRepository.class),
                mock(ChapterTopicRepository.class),
                mock(MistakeRecordRepository.class),
                mock(QuizAttemptService.class),
                mock(AnalyticsService.class),
                mock(QuestionEligibility.class),
                mock(QuestionSamplingService.class),
                mock(com.allearning.backend.academic.service.ChapterValidationService.class),
                mock(com.allearning.backend.catalog.service.CatalogService.class),
                mock(ObjectProvider.class),
                properties);
    }
}
