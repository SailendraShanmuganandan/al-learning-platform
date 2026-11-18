package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.JobStatus;
import com.allearning.backend.learning.entity.QuestionGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionGenerationJobRepository extends JpaRepository<QuestionGenerationJob, Long> {
    List<QuestionGenerationJob> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
    List<QuestionGenerationJob> findByStatus(JobStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(j) FROM QuestionGenerationJob j WHERE j.status = :status AND j.completedAt >= :since")
    long countByStatusAndCompletedAtGreaterThanEqual(
            @org.springframework.data.repository.query.Param("status") JobStatus status,
            @org.springframework.data.repository.query.Param("since") java.time.OffsetDateTime since
    );
}

