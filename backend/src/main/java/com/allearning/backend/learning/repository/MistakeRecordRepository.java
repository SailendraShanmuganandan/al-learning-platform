package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.MistakeRecord;
import com.allearning.backend.learning.entity.MistakeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MistakeRecordRepository extends JpaRepository<MistakeRecord, Long> {
    Optional<MistakeRecord> findByUserIdAndQuestionId(Long userId, Long questionId);
    List<MistakeRecord> findByUserId(Long userId);
    List<MistakeRecord> findByUserIdOrderByLastAnsweredAtDesc(Long userId);
    List<MistakeRecord> findByUserIdAndStatusOrderByLastAnsweredAtDesc(Long userId, MistakeStatus status);
}

