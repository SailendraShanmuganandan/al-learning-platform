package com.allearning.backend.learning.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "question_generation_jobs")
public class QuestionGenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "requested_count", nullable = false)
    private int requestedCount;

    @Column(name = "generated_count", nullable = false)
    private int generatedCount = 0;

    @Column(name = "rejected_count", nullable = false)
    private int rejectedCount = 0;

    @Column(name = "language", nullable = false, length = 5)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public QuestionGenerationJob() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public int getRequestedCount() { return requestedCount; }
    public void setRequestedCount(int requestedCount) { this.requestedCount = requestedCount; }

    public int getGeneratedCount() { return generatedCount; }
    public void setGeneratedCount(int generatedCount) { this.generatedCount = generatedCount; }

    public int getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}

