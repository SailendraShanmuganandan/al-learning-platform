package com.allearning.backend.learning.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "question_options")
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "option_order", nullable = false)
    private int optionOrder;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect = false;

    public QuestionOption() {}

    public QuestionOption(String optionText, int optionOrder, boolean isCorrect) {
        this.optionText = optionText;
        this.optionOrder = optionOrder;
        this.isCorrect = isCorrect;
    }

    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public int getOptionOrder() { return optionOrder; }
    public void setOptionOrder(int optionOrder) { this.optionOrder = optionOrder; }

    public boolean isCorrect() { return isCorrect; }
    public boolean getIsCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
}

