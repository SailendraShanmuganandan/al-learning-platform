package com.allearning.backend.learning.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "chapter_documents")
public class ChapterDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 20)
    private ExtractionStatus extractionStatus = ExtractionStatus.PENDING;

    @Column(name = "is_chatbot_source", nullable = false)
    private boolean isChatbotSource = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public ChapterDocument() {}

    public ChapterDocument(Long chapterId, String filename, String storageKey, Long fileSizeBytes, String mimeType, Long uploadedBy) {
        this.chapterId = chapterId;
        this.fileName = filename;
        this.storageKey = storageKey;
        this.fileSizeBytes = fileSizeBytes;
        this.fileType = mimeType;
        this.uploadedBy = uploadedBy;
        this.extractionStatus = ExtractionStatus.PENDING;
        this.status = DocumentStatus.ACTIVE;
    }

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

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public ExtractionStatus getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(ExtractionStatus extractionStatus) { this.extractionStatus = extractionStatus; }

    public boolean isChatbotSource() { return isChatbotSource; }
    public void setChatbotSource(boolean chatbotSource) { isChatbotSource = chatbotSource; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public Long getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
