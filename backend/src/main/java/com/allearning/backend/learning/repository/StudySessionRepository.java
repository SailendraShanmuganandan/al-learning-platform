package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.StudySession;
import com.allearning.backend.learning.entity.StudySessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<StudySession> findFirstByUserIdAndStatusOrderByStartedAtDesc(Long userId, StudySessionStatus status);

    Page<StudySession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    Page<StudySession> findByUserIdAndStatusOrderByStartedAtDesc(Long userId, StudySessionStatus status, Pageable pageable);

    List<StudySession> findByStatus(StudySessionStatus status);
}
