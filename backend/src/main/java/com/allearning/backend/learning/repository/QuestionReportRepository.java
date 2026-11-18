package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.dto.QuestionReportCount;
import com.allearning.backend.learning.entity.QuestionReport;
import com.allearning.backend.learning.entity.QuestionReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface QuestionReportRepository extends JpaRepository<QuestionReport, Long> {
    long countByQuestionIdAndStatus(Long questionId, QuestionReportStatus status);

    @Query("""
        SELECT COUNT(DISTINCT qr.userId)
        FROM QuestionReport qr
        WHERE qr.questionId = :questionId
          AND qr.status = com.allearning.backend.learning.entity.QuestionReportStatus.OPEN
    """)
    long countDistinctOpenReporters(@Param("questionId") Long questionId);

    long countByStatus(QuestionReportStatus status);

    List<QuestionReport> findByQuestionIdAndStatus(Long questionId, QuestionReportStatus status);

    boolean existsByQuestionIdAndUserId(Long questionId, Long userId);

    @Query("SELECT qr.questionId FROM QuestionReport qr WHERE qr.userId = :userId AND qr.questionId IN :questionIds")
    Set<Long> findReportedQuestionIdsByUserIdAndQuestionIds(@Param("userId") Long userId, @Param("questionIds") Collection<Long> questionIds);

    @Query("""
        SELECT new com.allearning.backend.learning.dto.QuestionReportCount(
            qr.questionId,
            COUNT(qr.reportId)
        )
        FROM QuestionReport qr
        WHERE qr.questionId IN :questionIds
          AND qr.status = com.allearning.backend.learning.entity.QuestionReportStatus.OPEN
        GROUP BY qr.questionId
    """)
    List<QuestionReportCount> countOpenReportsForQuestions(@Param("questionIds") Collection<Long> questionIds);

    @Query("""
        SELECT qr FROM QuestionReport qr
        JOIN Question q ON qr.questionId = q.questionId
        JOIN Quiz qz ON q.quizId = qz.quizId
        WHERE (:status IS NULL OR qr.status = :status)
          AND (:questionId IS NULL OR qr.questionId = :questionId)
          AND (:chapterId IS NULL OR qz.chapterId = :chapterId)
        ORDER BY qr.createdAt DESC
    """)
    Page<QuestionReport> findAdminReports(
            @Param("status") QuestionReportStatus status,
            @Param("questionId") Long questionId,
            @Param("chapterId") Long chapterId,
            Pageable pageable
    );
}
