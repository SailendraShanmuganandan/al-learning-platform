package com.allearning.backend.learning.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_answers")
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long answerId;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    @Column(name = "selected_option_id")
    private Long selectedOptionId;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    public QuizAnswer() {}

    public Long getAnswerId() { return answerId; }
    public void setAnswerId(Long answerId) { this.answerId = answerId; }

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public int getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(int questionOrder) { this.questionOrder = questionOrder; }

    public Long getSelectedOptionId() { return selectedOptionId; }
    public void setSelectedOptionId(Long selectedOptionId) { this.selectedOptionId = selectedOptionId; }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
}

