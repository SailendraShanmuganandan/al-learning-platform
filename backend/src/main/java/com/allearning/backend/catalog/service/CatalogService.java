package com.allearning.backend.catalog.service;

import com.allearning.backend.catalog.dto.*;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for the catalog module.
 * Other modules (e.g. commerce for cart/checkout) interact with catalog exclusively through this interface.
 */
public interface CatalogService {

    /**
     * Lists products for admin with filtering and pagination.
     */
    Page<ProductSummaryResponse> listAdminProducts(
            ProductType type,
            ProductStatus status,
            String search,
            Pageable pageable
    );

    /**
     * Retrieves full product details by ID for admin.
     */
    ProductDetailResponse getAdminProduct(Long productId);

    /**
     * Creates a new product (always starting as DRAFT).
     */
    ProductDetailResponse createProduct(ProductCreateRequest request);

    /**
     * Updates an existing product's fields.
     */
    ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request);

    /**
     * Transitions a product's status according to transition rules.
     */
    ProductDetailResponse updateProductStatus(Long productId, ProductStatus newStatus);

    /**
     * Finds product details by ID (for commerce / public).
     */
    Optional<ProductDetailResponse> findById(Long productId);

    // -------------------------------------------------------------------------
    // Public browsing (no authentication required)
    // -------------------------------------------------------------------------

    /**
     * Lists PUBLISHED products with optional filters and sort, for public consumption.
     *
     * @param type      optional product type filter (PHYSICAL_NOTE or VIDEO)
     * @param subjectId optional subject filter – resolves to chapter IDs via academic module
     * @param chapterId optional direct chapter filter; 400 if the chapter doesn't exist
     * @param q         optional case-insensitive name search
     * @param sort      "newest" (default), "price_asc", or "price_desc"; 400 on invalid value
     * @param pageable  page/size from the request
     * @return page of public product summaries
     */
    Page<PublicProductSummaryResponse> listPublicProducts(
            ProductType type,
            Long subjectId,
            Long chapterId,
            String q,
            String sort,
            Pageable pageable
    );

    /**
     * Returns the public detail of a PUBLISHED product by slug.
     * Throws 404 if missing, DRAFT, or ARCHIVED.
     */
    PublicProductDetailResponse getPublicProductBySlug(String slug);

    /**
     * Returns PUBLISHED products linked to a publicly visible chapter.
     * Throws 404 if the chapter is not publicly visible (INACTIVE or INACTIVE subject).
     */
    List<PublicProductSummaryResponse> getPublicProductsForChapter(Long chapterId);

    // -------------------------------------------------------------------------
    // Internal cross-module operations (commerce / cart / orders)
    // -------------------------------------------------------------------------

    /**
     * Internal method returning purchase-relevant facts for a product ID:
     * id, name, slug, type, status, current price, thumbnail URL, and (for notes) stock_quantity.
     * Used by commerce module to satisfy modular-monolith boundary requirements.
     */
    Optional<ProductPurchaseFacts> getProductPurchaseFacts(Long productId);

    /**
     * Atomically reserves stock for a physical note product.
     * Decrements stock_quantity if and only if stock_quantity >= quantity.
     *
     * @param productId product ID
     * @param quantity  quantity to reserve
     * @return true if successfully reserved, false if insufficient stock or not found
     */
    boolean reserveStock(Long productId, int quantity);

    /**
     * Atomically releases (restores) stock for a physical note product.
     *
     * @param productId product ID
     * @param quantity  quantity to release
     */
    void releaseStock(Long productId, int quantity);

    /**
     * Internal method returning video facts for a product ID:
     * name, slug, thumbnail, duration, description, status, and provider_video_id.
     * Used by commerce module (ownership / My Videos) without exposing public DTOs.
     */
    Optional<VideoProductFacts> getVideoProductFacts(Long productId);

    /**
     * Internal method returning video facts for multiple product IDs.
     */
    java.util.Map<Long, VideoProductFacts> getVideoProductFacts(java.util.Collection<Long> productIds);

    /**
     * Generates a signed embed URL for admin video playback preview.
     * Throws 404 if product not found or not a VIDEO.
     */
    AdminPlaybackResponse getAdminPlayback(Long productId);

    /**
     * Uploads and attaches a PDF file to a VIDEO product.
     * Throws 400 if product is not VIDEO or file is invalid, 404 if not found.
     */
    ProductDetailResponse uploadVideoAttachmentPdf(Long productId, org.springframework.web.multipart.MultipartFile file);

    /**
     * Removes the attached PDF file from a VIDEO product.
     */
    ProductDetailResponse deleteVideoAttachmentPdf(Long productId);

    /**
     * Loads the attached PDF file as a Resource for downloading.
     */
    VideoPdfResource getVideoAttachmentPdfResource(Long productId);

    record VideoPdfResource(
            org.springframework.core.io.Resource resource,
            String filename,
            long fileSize
    ) {}
}
