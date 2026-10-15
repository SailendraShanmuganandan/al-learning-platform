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

    @Column(name = "video_url", nullable = false, length = 1000)
    private String videoUrl;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public Video() {}

    public Video(Product product, String videoUrl, Integer durationSeconds, String description) {
        this.product = product;
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
}
