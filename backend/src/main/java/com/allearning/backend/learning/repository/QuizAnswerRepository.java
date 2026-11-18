package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.dto.QuestionAnswerStats;
import com.allearning.backend.learning.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    boolean existsByQuestionId(Long questionId);

    @Query("""
        SELECT COUNT(qa) > 0
        FROM QuizAnswer qa
        JOIN QuizAttempt att ON qa.attemptId = att.attemptId
        WHERE att.userId = :userId
          AND att.completedAt IS NOT NULL
          AND qa.questionId = :questionId
          AND qa.isCorrect IS NOT NULL
    """)
    boolean hasCompletedAnswerForUser(@Param("userId") Long userId, @Param("questionId") Long questionId);

    List<QuizAnswer> findByAttemptIdOrderByQuestionOrderAsc(Long attemptId);

    @Query("""
        SELECT qa.questionId, MAX(att.completedAt)
        FROM QuizAnswer qa
        JOIN QuizAttempt att ON qa.attemptId = att.attemptId
        WHERE att.userId = :userId
          AND att.completedAt IS NOT NULL
          AND qa.questionId IN :questionIds
        GROUP BY qa.questionId
    """)
    List<Object[]> findLastAnsweredTimesForUser(@Param("userId") Long userId, @Param("questionIds") Collection<Long> questionIds);

    @Query("""
        SELECT new com.allearning.backend.learning.dto.QuestionAnswerStats(
            qa.questionId,
            COUNT(qa.answerId),
            SUM(CASE WHEN qa.isCorrect = true THEN 1L ELSE 0L END)
        )
        FROM QuizAnswer qa
        JOIN QuizAttempt att ON qa.attemptId = att.attemptId
        WHERE qa.questionId IN :questionIds
          AND att.completedAt IS NOT NULL
          AND qa.isCorrect IS NOT NULL
        GROUP BY qa.questionId
    """)
    List<QuestionAnswerStats> getCompletedStatsForQuestions(@Param("questionIds") Collection<Long> questionIds);

    @Query(value = """
        SELECT qz.chapter_id, q.topic_id, qa.is_correct, att.completed_at, att.attempt_id AS source_id, qa.question_order
        FROM quiz_answers qa
        JOIN quiz_attempts att ON qa.attempt_id = att.attempt_id
        JOIN questions q ON qa.question_id = q.question_id
        JOIN quizzes qz ON q.quiz_id = qz.quiz_id
        WHERE att.user_id = :userId
          AND att.completed_at IS NOT NULL
          AND qa.is_correct IS NOT NULL
        
        UNION ALL
        
        SELECT qz.chapter_id, q.topic_id, daa.is_correct, da.completed_at, da.assessment_id AS source_id, daa.question_order
        FROM diagnostic_assessment_answers daa
        JOIN diagnostic_assessments da ON daa.assessment_id = da.assessment_id
        JOIN questions q ON daa.question_id = q.question_id
        JOIN quizzes qz ON q.quiz_id = qz.quiz_id
        WHERE da.user_id = :userId
          AND (da.status = 'COMPLETED' OR da.completed_at IS NOT NULL)
          AND daa.is_correct IS NOT NULL
        
        ORDER BY 4 DESC, 5 DESC, 6 DESC
    """, nativeQuery = true)
    List<Object[]> findAllUserGradedAnswers(@Param("userId") Long userId);

    @Query(value = """
        SELECT att.user_id, qz.chapter_id, q.topic_id, qa.is_correct, att.completed_at
        FROM quiz_answers qa
        JOIN quiz_attempts att ON qa.attempt_id = att.attempt_id
        JOIN questions q ON qa.question_id = q.question_id
        JOIN quizzes qz ON q.quiz_id = qz.quiz_id
        WHERE att.completed_at IS NOT NULL
          AND qa.is_correct IS NOT NULL
          AND att.user_id IN :userIds
        
        UNION ALL
        
        SELECT da.user_id, qz.chapter_id, q.topic_id, daa.is_correct, da.completed_at
        FROM diagnostic_assessment_answers daa
        JOIN diagnostic_assessments da ON daa.assessment_id = da.assessment_id
        JOIN questions q ON daa.question_id = q.question_id
        JOIN quizzes qz ON q.quiz_id = qz.quiz_id
        WHERE (da.status = 'COMPLETED' OR da.completed_at IS NOT NULL)
          AND daa.is_correct IS NOT NULL
          AND da.user_id IN :userIds
        
        ORDER BY 1, 2, 5 DESC
    """, nativeQuery = true)
    List<Object[]> findGradedAnswersForUsers(@Param("userIds") Collection<Long> userIds);
}
