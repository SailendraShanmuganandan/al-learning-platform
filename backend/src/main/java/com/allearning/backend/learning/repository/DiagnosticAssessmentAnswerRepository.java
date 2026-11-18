package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.dto.QuestionAnswerStats;
import com.allearning.backend.learning.entity.DiagnosticAssessmentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DiagnosticAssessmentAnswerRepository extends JpaRepository<DiagnosticAssessmentAnswer, Long> {
    boolean existsByQuestionId(Long questionId);

    @Query("""
        SELECT COUNT(daa) > 0
        FROM DiagnosticAssessmentAnswer daa
        JOIN DiagnosticAssessment da ON daa.assessmentId = da.assessmentId
        WHERE da.userId = :userId
          AND (da.status = com.allearning.backend.learning.entity.AssessmentStatus.COMPLETED OR da.completedAt IS NOT NULL)
          AND daa.questionId = :questionId
          AND daa.isCorrect IS NOT NULL
    """)
    boolean hasCompletedAnswerForUser(@Param("userId") Long userId, @Param("questionId") Long questionId);

    List<DiagnosticAssessmentAnswer> findByAssessmentIdOrderByQuestionOrderAsc(Long assessmentId);

    @Query("""
        SELECT daa.questionId, MAX(da.completedAt)
        FROM DiagnosticAssessmentAnswer daa
        JOIN DiagnosticAssessment da ON daa.assessmentId = da.assessmentId
        WHERE da.userId = :userId
          AND (da.status = com.allearning.backend.learning.entity.AssessmentStatus.COMPLETED OR da.completedAt IS NOT NULL)
          AND daa.questionId IN :questionIds
        GROUP BY daa.questionId
    """)
    List<Object[]> findLastAnsweredTimesForUser(@Param("userId") Long userId, @Param("questionIds") Collection<Long> questionIds);

    @Query("""
        SELECT new com.allearning.backend.learning.dto.QuestionAnswerStats(
            daa.questionId,
            COUNT(daa.answerId),
            SUM(CASE WHEN daa.isCorrect = true THEN 1L ELSE 0L END)
        )
        FROM DiagnosticAssessmentAnswer daa
        JOIN DiagnosticAssessment da ON daa.assessmentId = da.assessmentId
        WHERE daa.questionId IN :questionIds
          AND (da.status = com.allearning.backend.learning.entity.AssessmentStatus.COMPLETED OR da.completedAt IS NOT NULL)
          AND daa.isCorrect IS NOT NULL
        GROUP BY daa.questionId
    """)
    List<QuestionAnswerStats> getCompletedStatsForQuestions(@Param("questionIds") Collection<Long> questionIds);
}
