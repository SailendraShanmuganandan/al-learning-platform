package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.Quiz;
import com.allearning.backend.learning.entity.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long>, JpaSpecificationExecutor<Quiz> {

    Optional<Quiz> findByChapterId(Long chapterId);

    boolean existsByChapterId(Long chapterId);

    List<Quiz> findByChapterIdIn(Collection<Long> chapterIds);
}

