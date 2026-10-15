package com.allearning.backend.catalog.controller;

import com.allearning.backend.catalog.dto.*;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST controller for managing catalog products (physical notes and video courses).
 * Security: access is restricted to ADMIN role via {@link com.allearning.backend.security.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final CatalogService catalogService;

    public AdminProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * GET /api/admin/products
     * Returns a paginated list of products, optionally filtered by type, status, and/or search term.
     */
    @GetMapping
    public ResponseEntity<Page<ProductSummaryResponse>> listProducts(
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductSummaryResponse> page = catalogService.listAdminProducts(type, status, search, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * GET /api/admin/products/{id}
     * Returns full product details including subtype fields and linked chapter IDs.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getAdminProduct(id));
    }

    /**
     * POST /api/admin/products
     * Creates a new product (always starts in DRAFT status).
     */
    @PostMapping
    public ResponseEntity<ProductDetailResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductDetailResponse created = catalogService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH /api/admin/products/{id}
     * Updates editable product details, subtype-specific fields, and linked chapters.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ResponseEntity.ok(catalogService.updateProduct(id, request));
    }

    /**
     * PATCH /api/admin/products/{id}/status
     * Transitions product status (DRAFT -> PUBLISHED, PUBLISHED -> ARCHIVED, DRAFT -> ARCHIVED, ARCHIVED -> DRAFT).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductDetailResponse> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusRequest request
    ) {
        return ResponseEntity.ok(catalogService.updateProductStatus(id, request.status()));
    }
}
