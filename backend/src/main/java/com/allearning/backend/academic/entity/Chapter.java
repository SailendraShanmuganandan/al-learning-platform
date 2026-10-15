package com.allearning.backend.academic.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * JPA entity for the {@code chapters} table.
 * Status and chapter_order are enforced at DB level via CHECK / UNIQUE constraints
 * and at service level to surface clean error messages before hitting the DB.
 */
@Entity
@Table(name = "chapters")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chapter_id")
    private Long chapterId;

    /** Owning subject — never null, never changed after creation. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false, updatable = false)
    private Subject subject;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    /** 1-based display order within the subject. */
    @Column(name = "chapter_order", nullable = false)
    private Integer chapterOrder;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // --- Constructors ---

    protected Chapter() {}

    public Chapter(Subject subject, String name, Integer chapterOrder, String description) {
        this.subject = subject;
        this.name = name;
        this.chapterOrder = chapterOrder;
        this.description = description;
        this.status = "ACTIVE";
    }

    // --- Getters and Setters ---

    public Long getChapterId() { return chapterId; }
    public Subject getSubject() { return subject; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getChapterOrder() { return chapterOrder; }
    public void setChapterOrder(Integer chapterOrder) { this.chapterOrder = chapterOrder; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
