package com.allearning.backend.catalog.entity;

import jakarta.persistence.*;

/**
 * Subtype entity for PHYSICAL_NOTE products.
 * Shares primary key with {@link Product}.
 */
@Entity
@Table(name = "notes")
public class Note {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "page_count", nullable = false)
    private Integer pageCount;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    public Note() {}

    public Note(Product product, Integer pageCount, Integer stockQuantity) {
        this.product = product;
        this.pageCount = pageCount;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
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

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}
