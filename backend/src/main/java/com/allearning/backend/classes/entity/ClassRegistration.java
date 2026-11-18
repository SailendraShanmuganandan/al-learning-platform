package com.allearning.backend.classes.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "class_registrations")
public class ClassRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Long registrationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public ClassRegistration() {}

    public ClassRegistration(Long userId, ClassEntity classEntity) {
        this.userId = userId;
        this.classEntity = classEntity;
        this.status = RegistrationStatus.PENDING;
    }

    public ClassRegistration(Long userId, ClassEntity classEntity, RegistrationStatus status) {
        this.userId = userId;
        this.classEntity = classEntity;
        this.status = status != null ? status : RegistrationStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        if (registeredAt == null) {
            registeredAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ClassEntity getClassEntity() {
        return classEntity;
    }

    public void setClassEntity(ClassEntity classEntity) {
        this.classEntity = classEntity;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public OffsetDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(OffsetDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
