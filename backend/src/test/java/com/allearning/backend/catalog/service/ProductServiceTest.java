package com.allearning.backend.catalog.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.catalog.dto.*;
import com.allearning.backend.catalog.entity.*;
import com.allearning.backend.catalog.repository.NoteRepository;
import com.allearning.backend.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private ChapterValidationService chapterValidationService;

    @Mock
    private com.allearning.backend.integration.video.VideoPlaybackProvider videoPlaybackProvider;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, noteRepository, chapterValidationService, videoPlaybackProvider);
    }

    @Test
    void createNote_createsProductsRowAndNotesRow_noVideoRow() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Combined Mathematics Notes Vol 1",
                "Pure Mathematics notes for A/L",
                ProductType.PHYSICAL_NOTE,
                new BigDecimal("1500.00"),
                "https://example.com/thumb.jpg",
                List.of(10L, 11L),
                120,
                50,
                null,
                null,
                null
        );

        when(chapterValidationService.findMissingChapterIds(request.chapterIds())).thenReturn(List.of());
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(1L);
            return p;
        });

        ProductDetailResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(1L, response.productId());
        assertEquals("Combined Mathematics Notes Vol 1", response.name());
        assertEquals("combined-mathematics-notes-vol-1", response.slug());
        assertEquals(ProductType.PHYSICAL_NOTE, response.productType());
        assertEquals(120, response.pageCount());
        assertEquals(50, response.stockQuantity());
        assertNull(response.providerVideoId());
        assertNull(response.durationSeconds());
        assertEquals(ProductStatus.DRAFT, response.status());
        assertEquals(2, response.chapterIds().size());
    }

    @Test
    void createVideo_createsProductsRowAndVideosRow_noNotesRow() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Organic Chemistry Video Masterclass",
                "Complete video tutorial",
                ProductType.VIDEO,
                new BigDecimal("2500.00"),
                "https://example.com/chem.jpg",
                List.of(20L),
                null,
                null,
                "32d140e2-e4f4-4eec-9d53-20371e9be607",
                7200,
                "Comprehensive reaction mechanisms"
        );

        when(chapterValidationService.findMissingChapterIds(request.chapterIds())).thenReturn(List.of());
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(2L);
            return p;
        });

        ProductDetailResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(2L, response.productId());
        assertEquals(ProductType.VIDEO, response.productType());
        assertNull(response.pageCount());
        assertNull(response.stockQuantity());
        assertEquals("32d140e2-e4f4-4eec-9d53-20371e9be607", response.providerVideoId());
        assertEquals(7200, response.durationSeconds());
        assertEquals("Comprehensive reaction mechanisms", response.videoDescription());
        assertEquals(ProductStatus.DRAFT, response.status());
    }

    @Test
    void createNote_withVideoFields_throwsBadRequest() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Note With Video Field",
                "desc",
                ProductType.PHYSICAL_NOTE,
                new BigDecimal("500.00"),
                null,
                null,
                50,
                10,
                "32d140e2-e4f4-4eec-9d53-20371e9be607", // forbidden on note
                null,
                null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Video fields"));
    }

    @Test
    void createVideo_withNoteFields_throwsBadRequest() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Video With Note Field",
                "desc",
                ProductType.VIDEO,
                new BigDecimal("500.00"),
                null,
                null,
                50, // forbidden on video
                null,
                "32d140e2-e4f4-4eec-9d53-20371e9be607",
                3600,
                null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Note fields"));
    }

    @Test
    void createProduct_negativePriceOrStockOrZeroPageCount_throwsBadRequest() {
        // Negative price
        ProductCreateRequest req1 = new ProductCreateRequest(
                "Invalid Price", "desc", ProductType.PHYSICAL_NOTE,
                new BigDecimal("-1.00"), null, null, 10, 0, null, null, null
        );
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(req1));
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());

        // Zero page count
        ProductCreateRequest req2 = new ProductCreateRequest(
                "Zero Pages", "desc", ProductType.PHYSICAL_NOTE,
                new BigDecimal("100.00"), null, null, 0, 0, null, null, null
        );
        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(req2));
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());

        // Negative stock
        ProductCreateRequest req3 = new ProductCreateRequest(
                "Negative Stock", "desc", ProductType.PHYSICAL_NOTE,
                new BigDecimal("100.00"), null, null, 10, -5, null, null, null
        );
        ResponseStatusException ex3 = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(req3));
        assertEquals(HttpStatus.BAD_REQUEST, ex3.getStatusCode());
    }

    @Test
    void createProduct_unknownChapterIds_throwsBadRequest() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Unknown Chapter Note", "desc", ProductType.PHYSICAL_NOTE,
                new BigDecimal("100.00"), null, List.of(999L), 10, 0, null, null, null
        );

        when(chapterValidationService.findMissingChapterIds(request.chapterIds())).thenReturn(List.of(999L));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Unknown chapter ID"));
    }

    @Test
    void slugGeneration_appendsSuffixWhenTaken() {
        ProductCreateRequest req = new ProductCreateRequest(
                "Physics Mechanics", "desc", ProductType.PHYSICAL_NOTE,
                new BigDecimal("1000.00"), null, null, 50, 10, null, null, null
        );

        when(productRepository.existsBySlug("physics-mechanics")).thenReturn(true);
        when(productRepository.existsBySlug("physics-mechanics-1")).thenReturn(true);
        when(productRepository.existsBySlug("physics-mechanics-2")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductDetailResponse res = productService.createProduct(req);
        assertEquals("physics-mechanics-2", res.slug());
    }

    @Test
    void updateProduct_nameChangeLeavesSlugUnchanged() {
        Product product = new Product();
        product.setProductId(5L);
        product.setName("Old Name");
        product.setSlug("old-name");
        product.setProductType(ProductType.PHYSICAL_NOTE);
        product.setPrice(new BigDecimal("100.00"));
        product.setStatus(ProductStatus.DRAFT);
        product.setNote(new Note(product, 50, 10));

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductUpdateRequest updateReq = new ProductUpdateRequest(
                "Brand New Shiny Name",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        ProductDetailResponse updated = productService.updateProduct(5L, updateReq);

        assertEquals("Brand New Shiny Name", updated.name());
        assertEquals("old-name", updated.slug(), "Slug must remain stable when name is changed");
    }

    @Test
    void updateProduct_attemptToChangeType_throwsBadRequest() {
        Product product = new Product();
        product.setProductId(1L);
        product.setProductType(ProductType.PHYSICAL_NOTE);
        product.setStatus(ProductStatus.DRAFT);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductUpdateRequest updateReq = new ProductUpdateRequest(
                null, null, null, null, null, null, null, null, null, null,
                ProductType.VIDEO // illegal attempt to change type
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.updateProduct(1L, updateReq));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("type cannot be changed"));
    }

    @Test
    void updateProduct_fieldsNotBelongingToType_throwsBadRequest() {
        Product product = new Product();
        product.setProductId(1L);
        product.setProductType(ProductType.PHYSICAL_NOTE);
        product.setStatus(ProductStatus.DRAFT);
        product.setNote(new Note(product, 50, 10));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductUpdateRequest updateReq = new ProductUpdateRequest(
                null, null, null, null, null, null, null,
                "32d140e2-e4f4-4eec-9d53-20371e9be607", // forbidden on note
                null, null, null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.updateProduct(1L, updateReq));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Video fields"));
    }

    @Test
    void statusTransitions_allowedAndConflictTransitions() {
        Product product = new Product();
        product.setProductId(10L);
        product.setName("Physics Book");
        product.setProductType(ProductType.PHYSICAL_NOTE);
        product.setPrice(new BigDecimal("100.00"));
        product.setStatus(ProductStatus.DRAFT);
        product.setChapterIds(new HashSet<>(List.of(1L)));
        product.setNote(new Note(product, 50, 10));

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        // DRAFT -> PUBLISHED: allowed
        ProductDetailResponse pubRes = productService.updateProductStatus(10L, ProductStatus.PUBLISHED);
        assertEquals(ProductStatus.PUBLISHED, pubRes.status());

        // PUBLISHED -> ARCHIVED: allowed
        ProductDetailResponse archRes = productService.updateProductStatus(10L, ProductStatus.ARCHIVED);
        assertEquals(ProductStatus.ARCHIVED, archRes.status());

        // ARCHIVED -> PUBLISHED: NOT allowed -> 409
        ResponseStatusException conflictEx1 = assertThrows(ResponseStatusException.class,
                () -> productService.updateProductStatus(10L, ProductStatus.PUBLISHED));
        assertEquals(HttpStatus.CONFLICT, conflictEx1.getStatusCode());

        // ARCHIVED -> DRAFT: allowed
        ProductDetailResponse draftRes = productService.updateProductStatus(10L, ProductStatus.DRAFT);
        assertEquals(ProductStatus.DRAFT, draftRes.status());

        // DRAFT -> ARCHIVED: allowed
        ProductDetailResponse draftToArch = productService.updateProductStatus(10L, ProductStatus.ARCHIVED);
        assertEquals(ProductStatus.ARCHIVED, draftToArch.status());
    }

    @Test
    void publishValidation_missingFieldsOrChapters_throwsBadRequest() {
        Product product = new Product();
        product.setProductId(20L);
        product.setName("Incomplete Product");
        product.setProductType(ProductType.PHYSICAL_NOTE);
        product.setPrice(new BigDecimal("100.00"));
        product.setStatus(ProductStatus.DRAFT);
        product.setChapterIds(new HashSet<>()); // NO CHAPTERS!
        product.setNote(new Note(product, null, 0)); // NO PAGE COUNT!

        when(productRepository.findById(20L)).thenReturn(Optional.of(product));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.updateProductStatus(20L, ProductStatus.PUBLISHED));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Missing requirements"));
        assertTrue(ex.getReason().contains("at least one chapter"));
        assertTrue(ex.getReason().contains("page count"));
    }

    @Test
    void getPublicProductBySlug_draftOrArchived_throwsNotFound() {
        Product draft = new Product();
        draft.setProductId(30L);
        draft.setSlug("draft-product");
        draft.setStatus(ProductStatus.DRAFT);

        Product archived = new Product();
        archived.setProductId(31L);
        archived.setSlug("archived-product");
        archived.setStatus(ProductStatus.ARCHIVED);

        when(productRepository.findBySlug("draft-product")).thenReturn(Optional.of(draft));
        when(productRepository.findBySlug("archived-product")).thenReturn(Optional.of(archived));
        when(productRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class,
                () -> productService.getPublicProductBySlug("draft-product"));
        assertEquals(HttpStatus.NOT_FOUND, ex1.getStatusCode());

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class,
                () -> productService.getPublicProductBySlug("archived-product"));
        assertEquals(HttpStatus.NOT_FOUND, ex2.getStatusCode());

        ResponseStatusException ex3 = assertThrows(ResponseStatusException.class,
                () -> productService.getPublicProductBySlug("nonexistent"));
        assertEquals(HttpStatus.NOT_FOUND, ex3.getStatusCode());
    }

    @Test
    void getPublicProductBySlug_linkedOnlyToInactiveChapters_loadsWithEmptyChapters() {
        Product product = new Product();
        product.setProductId(40L);
        product.setName("Physics Mechanics Note");
        product.setSlug("physics-mechanics-note");
        product.setProductType(ProductType.PHYSICAL_NOTE);
        product.setPrice(new BigDecimal("1200.00"));
        product.setStatus(ProductStatus.PUBLISHED);
        product.setChapterIds(new HashSet<>(List.of(99L)));
        product.setNote(new Note(product, 80, 0)); // stock is 0 -> in_stock=false

        when(productRepository.findBySlug("physics-mechanics-note")).thenReturn(Optional.of(product));
        // Chapter 99 is INACTIVE, so chapterValidationService returns empty list
        when(chapterValidationService.getPublicChapterSummaries(Set.of(99L))).thenReturn(List.of());

        PublicProductDetailResponse response = productService.getPublicProductBySlug("physics-mechanics-note");

        assertNotNull(response);
        assertEquals("Physics Mechanics Note", response.name());
        assertEquals("physics-mechanics-note", response.slug());
        assertEquals(80, response.pageCount());
        assertFalse(response.inStock(), "Stock is 0 so inStock must be false");
        assertTrue(response.chapters().isEmpty(), "Inactive chapters must not be included");
    }

    @Test
    void getPublicProductsForChapter_inactiveChapter_throwsNotFound() {
        when(chapterValidationService.isPubliclyVisible(55L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.getPublicProductsForChapter(55L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(productRepository, never()).findByChapterIdInAndStatus(any(), any());
    }

    @Test
    void getPublicProductsForChapter_activeChapter_returnsPublishedProducts() {
        when(chapterValidationService.isPubliclyVisible(55L)).thenReturn(true);

        Product product = new Product();
        product.setProductId(50L);
        product.setName("Calculus Video");
        product.setSlug("calculus-video");
        product.setProductType(ProductType.VIDEO);
        product.setPrice(new BigDecimal("2000.00"));
        product.setStatus(ProductStatus.PUBLISHED);
        product.setChapterIds(new HashSet<>(List.of(55L)));
        product.setVideo(new Video(product, "https://player.vimeo.com/video/555", 3600, "Desc"));

        when(productRepository.findByChapterIdInAndStatus(List.of(55L), ProductStatus.PUBLISHED))
                .thenReturn(List.of(product));

        List<PublicProductSummaryResponse> result = productService.getPublicProductsForChapter(55L);

        assertEquals(1, result.size());
        assertEquals("Calculus Video", result.get(0).name());
        assertEquals(3600, result.get(0).durationSeconds());
    }

    @Test
    void listPublicProducts_invalidSort_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.listPublicProducts(null, null, null, null, "invalid_sort", null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid sort value"));
    }

    @Test
    void reserveStock_successWhenRowsUpdated() {
        when(noteRepository.decrementStockIfAvailable(10L, 2)).thenReturn(1);
        boolean result = productService.reserveStock(10L, 2);
        assertTrue(result);
        verify(noteRepository).decrementStockIfAvailable(10L, 2);
    }

    @Test
    void reserveStock_failsWhenInsufficientStockOrNotFound() {
        when(noteRepository.decrementStockIfAvailable(10L, 5)).thenReturn(0);
        boolean result = productService.reserveStock(10L, 5);
        assertFalse(result);
        verify(noteRepository).decrementStockIfAvailable(10L, 5);
    }

    @Test
    void releaseStock_incrementsStock() {
        productService.releaseStock(10L, 3);
        verify(noteRepository).incrementStock(10L, 3);
    }

    @Test
    void createVideo_invalidUuid_throwsBadRequest() {
        ProductCreateRequest req = new ProductCreateRequest(
                "Invalid UUID Video", "desc", ProductType.VIDEO,
                new BigDecimal("1000.00"), null, null, null, null,
                "not-a-valid-uuid", 3600, null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Bunny Video ID must be a valid UUID/GUID"));
    }

    @Test
    void publishValidation_videoMissingBunnyIdOrDuration_throwsBadRequest() {
        Product product = new Product();
        product.setProductId(25L);
        product.setName("Incomplete Video");
        product.setProductType(ProductType.VIDEO);
        product.setPrice(new BigDecimal("100.00"));
        product.setStatus(ProductStatus.DRAFT);
        product.setChapterIds(new HashSet<>(List.of(1L)));
        product.setVideo(new Video(product, null, null, null));

        when(productRepository.findById(25L)).thenReturn(Optional.of(product));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.updateProductStatus(25L, ProductStatus.PUBLISHED));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Bunny Video ID is required"));
        assertTrue(ex.getReason().contains("Video duration must be set"));
    }

    @Test
    void publishValidation_videoNotReady_throwsBadRequest() {
        String videoId = "32d140e2-e4f4-4eec-9d53-20371e9be607";
        Product product = new Product();
        product.setProductId(26L);
        product.setName("Encoding Video");
        product.setProductType(ProductType.VIDEO);
        product.setPrice(new BigDecimal("100.00"));
        product.setStatus(ProductStatus.DRAFT);
        product.setChapterIds(new HashSet<>(List.of(1L)));
        product.setVideo(new Video(product, videoId, 3600, null));

        when(productRepository.findById(26L)).thenReturn(Optional.of(product));
        when(videoPlaybackProvider.getVideoInfo(videoId))
                .thenReturn(Optional.of(new com.allearning.backend.integration.video.VideoProviderInfo(true, 3600, false)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.updateProductStatus(26L, ProductStatus.PUBLISHED));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("VIDEO_NOT_READY", ex.getReason());
    }

    @Test
    void getAdminPlayback_successAndErrorModes() {
        String videoId = "32d140e2-e4f4-4eec-9d53-20371e9be607";
        Product videoProduct = new Product();
        videoProduct.setProductId(50L);
        videoProduct.setProductType(ProductType.VIDEO);
        videoProduct.setVideo(new Video(videoProduct, videoId, 3600, null));

        Product noteProduct = new Product();
        noteProduct.setProductId(51L);
        noteProduct.setProductType(ProductType.PHYSICAL_NOTE);

        when(productRepository.findById(50L)).thenReturn(Optional.of(videoProduct));
        when(productRepository.findById(51L)).thenReturn(Optional.of(noteProduct));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        when(videoPlaybackProvider.signEmbedUrl(eq(videoId), eq(120)))
                .thenReturn(new com.allearning.backend.integration.video.SignedEmbedResult(
                        "https://iframe.mediadelivery.net/embed/123/" + videoId + "?token=abc&expires=123",
                        java.time.OffsetDateTime.now().plusHours(2)
                ));

        // Success
        var playback = productService.getAdminPlayback(50L);
        assertNotNull(playback);
        assertTrue(playback.embedUrl().contains(videoId));

        // 404 for physical note
        ResponseStatusException noteEx = assertThrows(ResponseStatusException.class,
                () -> productService.getAdminPlayback(51L));
        assertEquals(HttpStatus.NOT_FOUND, noteEx.getStatusCode());

        // 404 for missing
        ResponseStatusException missingEx = assertThrows(ResponseStatusException.class,
                () -> productService.getAdminPlayback(99L));
        assertEquals(HttpStatus.NOT_FOUND, missingEx.getStatusCode());
    }
}
