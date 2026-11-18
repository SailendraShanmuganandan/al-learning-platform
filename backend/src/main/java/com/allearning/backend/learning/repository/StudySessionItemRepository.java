package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.StudySessionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudySessionItemRepository extends JpaRepository<StudySessionItem, Long> {
    List<StudySessionItem> findBySessionIdOrderByItemOrderAsc(Long sessionId);
}

