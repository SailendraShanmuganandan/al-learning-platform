package com.allearning.backend.learning.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long attemptId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_type", nullable = false, length = 30)
    private QuizAttemptType attemptType;

    @Column(name = "quiz_id")
    private Long quizId;

    @Column(name = "study_session_id")
    private Long studySessionId;

    @Column(name = "score")
    private Integer score;

    @Column(name = "total_marks", nullable = false)
    private int totalMarks;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public QuizAttempt() {}

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) startedAt = OffsetDateTime.now();
    }

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public QuizAttemptType getAttemptType() { return attemptType; }
    public void setAttemptType(QuizAttemptType attemptType) { this.attemptType = attemptType; }

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }

    public Long getStudySessionId() { return studySessionId; }
    public void setStudySessionId(Long studySessionId) { this.studySessionId = studySessionId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public int getTotalMarks() { return totalMarks; }
    public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}

