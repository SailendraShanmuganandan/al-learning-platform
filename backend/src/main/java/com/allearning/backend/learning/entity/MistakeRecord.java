package com.allearning.backend.learning.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "mistake_records")
public class MistakeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mistake_id")
    private Long mistakeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MistakeStatus status = MistakeStatus.ACTIVE;

    @Column(name = "correct_streak", nullable = false)
    private int correctStreak = 0;

    @Column(name = "last_answered_at", nullable = false)
    private OffsetDateTime lastAnsweredAt;

    @Column(name = "mastered_at")
    private OffsetDateTime masteredAt;

    public MistakeRecord() {}

    public MistakeRecord(Long userId, Long questionId) {
        this.userId = userId;
        this.questionId = questionId;
        this.status = MistakeStatus.ACTIVE;
        this.correctStreak = 0;
        this.lastAnsweredAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (lastAnsweredAt == null) lastAnsweredAt = OffsetDateTime.now();
    }

    public Long getMistakeId() { return mistakeId; }
    public void setMistakeId(Long mistakeId) { this.mistakeId = mistakeId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public MistakeStatus getStatus() { return status; }
    public void setStatus(MistakeStatus status) { this.status = status; }

    public int getCorrectStreak() { return correctStreak; }
    public void setCorrectStreak(int correctStreak) { this.correctStreak = correctStreak; }

    public OffsetDateTime getLastAnsweredAt() { return lastAnsweredAt; }
    public void setLastAnsweredAt(OffsetDateTime lastAnsweredAt) { this.lastAnsweredAt = lastAnsweredAt; }

    public OffsetDateTime getMasteredAt() { return masteredAt; }
    public void setMasteredAt(OffsetDateTime masteredAt) { this.masteredAt = masteredAt; }
}

