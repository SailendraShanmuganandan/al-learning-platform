package com.allearning.backend.learning.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "study_session_items")
public class StudySessionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private StudySessionItemType itemType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public StudySessionItem() {}

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public StudySessionItemType getItemType() { return itemType; }
    public void setItemType(StudySessionItemType itemType) { this.itemType = itemType; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public int getItemOrder() { return itemOrder; }
    public void setItemOrder(int itemOrder) { this.itemOrder = itemOrder; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}

