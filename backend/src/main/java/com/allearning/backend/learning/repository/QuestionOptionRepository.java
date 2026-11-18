package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    List<QuestionOption> findByQuestionQuestionIdOrderByOptionOrderAsc(Long questionId);
    void deleteByQuestionQuestionId(Long questionId);
}
