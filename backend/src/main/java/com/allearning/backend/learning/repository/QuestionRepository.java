package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.Question;
import com.allearning.backend.learning.entity.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    long countByQuizId(Long quizId);

    long countByQuizIdAndStatus(Long quizId, QuestionStatus status);

    long countByTopicId(Long topicId);

    @Query("""
        SELECT q FROM Question q JOIN q.quiz quiz WHERE quiz.chapterId = :chapterId
    """)
    List<Question> findAllByChapterId(@Param("chapterId") Long chapterId);

    long countByStatus(QuestionStatus status);

    List<Question> findByQuestionIdIn(Collection<Long> questionIds);

    @Query("""
        SELECT COUNT(q)
        FROM Question q
        JOIN q.quiz qz
        JOIN q.topic t
        WHERE qz.chapterId = :chapterId
          AND q.status = com.allearning.backend.learning.entity.QuestionStatus.APPROVED
          AND t.status = com.allearning.backend.learning.entity.TopicStatus.ACTIVE
          AND (:language IS NULL OR q.language = :language)
    """)
    long countEligibleForChapter(@Param("chapterId") Long chapterId, @Param("language") String language);

    @Query("""
        SELECT q.language, COUNT(q)
        FROM Question q
        JOIN q.quiz qz
        JOIN q.topic t
        WHERE qz.chapterId = :chapterId
          AND q.status = com.allearning.backend.learning.entity.QuestionStatus.APPROVED
          AND t.status = com.allearning.backend.learning.entity.TopicStatus.ACTIVE
        GROUP BY q.language
    """)
    List<Object[]> countEligibleByLanguageForChapter(@Param("chapterId") Long chapterId);

    @Query("""
        SELECT q
        FROM Question q
        JOIN q.quiz qz
        JOIN q.topic t
        WHERE qz.chapterId = :chapterId
          AND q.status = com.allearning.backend.learning.entity.QuestionStatus.APPROVED
          AND t.status = com.allearning.backend.learning.entity.TopicStatus.ACTIVE
          AND (:language IS NULL OR q.language = :language)
    """)
    List<Question> findEligibleForChapter(@Param("chapterId") Long chapterId, @Param("language") String language);

    @Query("""
        SELECT q.questionId
        FROM Question q
        JOIN q.quiz qz
        JOIN q.topic t
        WHERE qz.chapterId = :chapterId
          AND q.status = com.allearning.backend.learning.entity.QuestionStatus.APPROVED
          AND t.status = com.allearning.backend.learning.entity.TopicStatus.ACTIVE
          AND (:language IS NULL OR q.language = :language)
    """)
    List<Long> findEligibleIdsForChapter(@Param("chapterId") Long chapterId, @Param("language") String language);

    @Query("""
        SELECT q.questionId
        FROM Question q
        JOIN q.topic t
        WHERE q.questionId IN :questionIds
          AND q.status = com.allearning.backend.learning.entity.QuestionStatus.APPROVED
          AND t.status = com.allearning.backend.learning.entity.TopicStatus.ACTIVE
    """)
    List<Long> filterEligibleIds(@Param("questionIds") Collection<Long> questionIds);

    @Query("""
        SELECT q
        FROM Question q
        JOIN q.topic t
        WHERE q.questionId IN :questionIds
          AND q.status = com.allearning.backend.learning.entity.QuestionStatus.APPROVED
          AND t.status = com.allearning.backend.learning.entity.TopicStatus.ACTIVE
    """)
    List<Question> filterEligibleQuestions(@Param("questionIds") Collection<Long> questionIds);
}
