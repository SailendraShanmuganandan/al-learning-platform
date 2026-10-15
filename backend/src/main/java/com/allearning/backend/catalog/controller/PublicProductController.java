package com.allearning.backend.catalog.controller;

import com.allearning.backend.catalog.dto.PublicProductDetailResponse;
import com.allearning.backend.catalog.dto.PublicProductSummaryResponse;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.service.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Public REST controller for product browsing.
 * No authentication required — security is enforced in
 * {@link com.allearning.backend.security.SecurityConfig}.
 *
 * Endpoints:
 *   GET /api/products                           — paginated PUBLISHED products with optional filters
 *   GET /api/products/{slug}                    — single PUBLISHED product by slug
 *   GET /api/chapters/{chapterId}/products      — PUBLISHED products for a chapter
 */
@RestController
public class PublicProductController {

    private final CatalogService catalogService;

    public PublicProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * GET /api/products
     * <p>
     * Optional query params:
     *   type=PHYSICAL_NOTE|VIDEO
     *   subjectId=&lt;long&gt;
     *   chapterId=&lt;long&gt;
     *   q=&lt;search text&gt;
     *   sort=newest|price_asc|price_desc  (default: newest)
     *   page=0, size=20
     *
     * Returns 400 on invalid type or sort values.
     */
    @GetMapping("/api/products")
    public ResponseEntity<Page<PublicProductSummaryResponse>> listProducts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductType productType = null;
        if (type != null && !type.isBlank()) {
            try {
                productType = ProductType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid type '" + type + "'. Allowed: PHYSICAL_NOTE, VIDEO");
            }
        }

        Page<PublicProductSummaryResponse> result = catalogService.listPublicProducts(
                productType,
                subjectId,
                chapterId,
                q,
                sort,
                PageRequest.of(page, size)
        );

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/products/{slug}
     * Returns a PUBLISHED product's full details.
     * Returns 404 if missing, DRAFT, or ARCHIVED.
     */
    @GetMapping("/api/products/{slug}")
    public ResponseEntity<PublicProductDetailResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(catalogService.getPublicProductBySlug(slug));
    }

    /**
     * GET /api/chapters/{chapterId}/products
     * Returns all PUBLISHED products linked to this chapter.
     * Returns 404 if the chapter is not publicly visible.
     *
     * Note: This mapping must not conflict with GET /api/chapters/{id} which is in ChapterController.
     * Spring MVC resolves them by the path suffix (/products vs nothing), so no conflict exists.
     */
    @GetMapping("/api/chapters/{chapterId}/products")
    public ResponseEntity<List<PublicProductSummaryResponse>> listChapterProducts(
            @PathVariable Long chapterId) {
        return ResponseEntity.ok(catalogService.getPublicProductsForChapter(chapterId));
    }
}
