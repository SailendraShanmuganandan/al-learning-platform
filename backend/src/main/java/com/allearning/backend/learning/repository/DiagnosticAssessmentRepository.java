package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.AssessmentStatus;
import com.allearning.backend.learning.entity.AssessmentType;
import com.allearning.backend.learning.entity.DiagnosticAssessment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticAssessmentRepository extends JpaRepository<DiagnosticAssessment, Long>, JpaSpecificationExecutor<DiagnosticAssessment> {

    List<DiagnosticAssessment> findByUserIdOrderByStartedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT da FROM DiagnosticAssessment da WHERE da.assessmentId = :id")
    Optional<DiagnosticAssessment> findByIdWithLock(@Param("id") Long id);

    boolean existsByUserIdAndStatus(Long userId, AssessmentStatus status);

    boolean existsByUserIdAndCompletedAtIsNotNull(Long userId);

    Page<DiagnosticAssessment> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    @Query("""
        SELECT da FROM DiagnosticAssessment da
        WHERE da.userId = :userId AND da.status = :status
        ORDER BY da.startedAt DESC
    """)
    Page<DiagnosticAssessment> findByUserIdAndStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") AssessmentStatus status,
            Pageable pageable
    );
}
