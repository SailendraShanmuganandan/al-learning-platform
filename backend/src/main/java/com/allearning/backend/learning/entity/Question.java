package com.allearning.backend.learning.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", insertable = false, updatable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "topic_id")
    private Long topicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", insertable = false, updatable = false)
    private ChapterTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private QuestionDifficulty difficulty;

    @Column(name = "language", nullable = false, length = 5)
    private String language = "EN";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuestionStatus status = QuestionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private QuestionSource source = QuestionSource.MANUAL;

    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    @Column(name = "generation_job_id")
    private Long generationJobId;

    @Column(name = "source_excerpt", columnDefinition = "TEXT")
    private String sourceExcerpt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("optionOrder ASC")
    private List<QuestionOption> options = new ArrayList<>();

    public Question() {}

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void addOption(QuestionOption option) {
        options.add(option);
        option.setQuestion(this);
    }

    public void removeOption(QuestionOption option) {
        options.remove(option);
        option.setQuestion(null);
    }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getId() { return questionId; }

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }

    public Quiz getQuiz() { return quiz; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }

    public ChapterTopic getTopic() { return topic; }

    public QuestionDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(QuestionDifficulty difficulty) { this.difficulty = difficulty; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public QuestionStatus getStatus() { return status; }
    public void setStatus(QuestionStatus status) { this.status = status; }

    public QuestionSource getSource() { return source; }
    public void setSource(QuestionSource source) { this.source = source; }

    public Long getSourceDocumentId() { return sourceDocumentId; }
    public void setSourceDocumentId(Long sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; }

    public Long getGenerationJobId() { return generationJobId; }
    public void setGenerationJobId(Long generationJobId) { this.generationJobId = generationJobId; }

    public String getSourceExcerpt() { return sourceExcerpt; }
    public void setSourceExcerpt(String sourceExcerpt) { this.sourceExcerpt = sourceExcerpt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<QuestionOption> getOptions() { return options; }
    public void setOptions(List<QuestionOption> options) { this.options = options; }
}
