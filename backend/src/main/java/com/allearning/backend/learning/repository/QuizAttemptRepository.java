package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.QuizAttempt;
import com.allearning.backend.learning.entity.QuizAttemptType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<QuizAttempt> findFirstByUserIdAndQuizIdAndAttemptTypeAndCompletedAtIsNull(
            Long userId,
            Long quizId,
            QuizAttemptType attemptType
    );

    Optional<QuizAttempt> findFirstByStudySessionId(Long studySessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.attemptId = :attemptId")
    Optional<QuizAttempt> findByIdWithLock(@Param("attemptId") Long attemptId);

    Page<QuizAttempt> findByUserIdAndQuizIdAndAttemptTypeAndCompletedAtIsNotNullOrderByStartedAtDesc(
            Long userId,
            Long quizId,
            QuizAttemptType attemptType,
            Pageable pageable
    );

    List<QuizAttempt> findByUserIdAndQuizIdAndAttemptTypeAndCompletedAtIsNotNullOrderByStartedAtDesc(
            Long userId,
            Long quizId,
            QuizAttemptType attemptType
    );

    List<QuizAttempt> findByUserIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(Long userId, Pageable pageable);
}
