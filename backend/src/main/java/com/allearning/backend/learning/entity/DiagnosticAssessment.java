package com.allearning.backend.learning.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "diagnostic_assessments")
public class DiagnosticAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_id")
    private Long assessmentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false, length = 30)
    private AssessmentType assessmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private AssessmentScopeType scopeType;

    @Column(name = "scope_reference", columnDefinition = "JSONB")
    private String scopeReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssessmentStatus status;

    @Column(name = "score")
    private Integer score;

    @Column(name = "total_marks", nullable = false)
    private int totalMarks;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public DiagnosticAssessment() {}

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) startedAt = OffsetDateTime.now();
    }

    public Long getAssessmentId() { return assessmentId; }
    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public AssessmentType getAssessmentType() { return assessmentType; }
    public void setAssessmentType(AssessmentType assessmentType) { this.assessmentType = assessmentType; }

    public AssessmentScopeType getScopeType() { return scopeType; }
    public void setScopeType(AssessmentScopeType scopeType) { this.scopeType = scopeType; }

    public String getScopeReference() { return scopeReference; }
    public void setScopeReference(String scopeReference) { this.scopeReference = scopeReference; }

    public AssessmentStatus getStatus() { return status; }
    public void setStatus(AssessmentStatus status) { this.status = status; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public int getTotalMarks() { return totalMarks; }
    public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public OffsetDateTime getCreatedAt() { return startedAt; }
}
