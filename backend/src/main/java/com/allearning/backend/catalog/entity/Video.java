package com.allearning.backend.catalog.entity;

import jakarta.persistence.*;

/**
 * Subtype entity for VIDEO products.
 * Shares primary key with {@link Product}.
 */
@Entity
@Table(name = "videos")
public class Video {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "video_provider", nullable = false, length = 50)
    private String videoProvider = "BUNNY";

    @Column(name = "provider_video_id", length = 100)
    private String providerVideoId;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "pdf_file_path", length = 500)
    private String pdfFilePath;

    @Column(name = "pdf_original_filename", length = 255)
    private String pdfOriginalFilename;

    @Column(name = "pdf_file_size")
    private Long pdfFileSize;

    public Video() {}

    public Video(Product product, String providerVideoId, Integer durationSeconds, String description) {
        this.product = product;
        this.videoProvider = "BUNNY";
        this.providerVideoId = providerVideoId;
        this.durationSeconds = durationSeconds;
        this.description = description;
    }

    public Video(Product product, String videoProvider, String providerVideoId, String videoUrl, Integer durationSeconds, String description) {
        this.product = product;
        this.videoProvider = (videoProvider != null && !videoProvider.isBlank()) ? videoProvider : "BUNNY";
        this.providerVideoId = providerVideoId;
        this.videoUrl = videoUrl;
        this.durationSeconds = durationSeconds;
        this.description = description;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getVideoProvider() {
        return videoProvider;
    }

    public void setVideoProvider(String videoProvider) {
        this.videoProvider = videoProvider;
    }

    public String getProviderVideoId() {
        return providerVideoId;
    }

    public void setProviderVideoId(String providerVideoId) {
        this.providerVideoId = providerVideoId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPdfFilePath() {
        return pdfFilePath;
    }

    public void setPdfFilePath(String pdfFilePath) {
        this.pdfFilePath = pdfFilePath;
    }

    public String getPdfOriginalFilename() {
        return pdfOriginalFilename;
    }

    public void setPdfOriginalFilename(String pdfOriginalFilename) {
        this.pdfOriginalFilename = pdfOriginalFilename;
    }

    public Long getPdfFileSize() {
        return pdfFileSize;
    }

    public void setPdfFileSize(Long pdfFileSize) {
        this.pdfFileSize = pdfFileSize;
    }
}
