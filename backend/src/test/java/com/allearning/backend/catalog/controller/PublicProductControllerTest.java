package com.allearning.backend.catalog.controller;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.catalog.dto.PublicProductDetailResponse;
import com.allearning.backend.catalog.dto.PublicProductSummaryResponse;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the public product browsing endpoints.
 *
 * CRITICAL security assertions:
 *  - video_url MUST NEVER appear in any public response body (FR-VID-06).
 *  - stock_quantity MUST NEVER appear in any public response body.
 *  - DRAFT and ARCHIVED products must never surface publicly.
 *  - Unauthenticated access must be allowed (HTTP 200, not 401).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:publiccatalogtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "spring.jpa.open-in-view=false",
        "spring.security.user.name=disabled",
        "spring.security.user.password=disabled"
})
class PublicProductControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @MockitoBean
    private CatalogService catalogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)
                .build();
    }

    // -------------------------------------------------------------------------
    // FR-VID-06 Security: video_url must NEVER appear in public responses
    // -------------------------------------------------------------------------

    /**
     * This test is a compile-time + runtime guard against the video_url field
     * appearing in the public product detail response DTO.
     * If someone adds video_url to PublicProductDetailResponse, this test fails.
     */
    @Test
    @DisplayName("SECURITY: PublicProductDetailResponse must not declare a videoUrl field")
    void publicDetailResponse_mustNotHaveVideoUrlField() {
        boolean hasVideoUrlField = false;
        for (java.lang.reflect.RecordComponent rc : PublicProductDetailResponse.class.getRecordComponents()) {
            if (rc.getName().equalsIgnoreCase("videoUrl") ||
                    rc.getName().equalsIgnoreCase("video_url")) {
                hasVideoUrlField = true;
                break;
            }
        }
        assertThat(hasVideoUrlField)
                .as("PublicProductDetailResponse must NOT contain a videoUrl field (FR-VID-06)")
                .isFalse();
    }

    /**
     * Same guard for the list summary DTO.
     */
    @Test
    @DisplayName("SECURITY: PublicProductSummaryResponse must not declare a videoUrl field")
    void publicSummaryResponse_mustNotHaveVideoUrlField() {
        boolean hasVideoUrlField = false;
        for (java.lang.reflect.RecordComponent rc : PublicProductSummaryResponse.class.getRecordComponents()) {
            if (rc.getName().equalsIgnoreCase("videoUrl") ||
                    rc.getName().equalsIgnoreCase("video_url")) {
                hasVideoUrlField = true;
                break;
            }
        }
        assertThat(hasVideoUrlField)
                .as("PublicProductSummaryResponse must NOT contain a videoUrl field (FR-VID-06)")
                .isFalse();
    }

    /**
     * Ensures the JSON serialised by /api/products does NOT contain "video_url" or "videoUrl".
     */
    @Test
    @DisplayName("SECURITY: GET /api/products JSON response must not contain video_url")
    void listProducts_responseJson_mustNotContainVideoUrl() throws Exception {
        PublicProductSummaryResponse summary = new PublicProductSummaryResponse(
                1L, "Physics Video", "physics-video",
                ProductType.VIDEO,
                BigDecimal.valueOf(999.00), null,
                null, null,
                3600, 2
        );
        when(catalogService.listPublicProducts(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(summary)));

        String body = mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .as("Public product list response must not expose video_url (FR-VID-06)")
                .doesNotContainIgnoringCase("videoUrl")
                .doesNotContainIgnoringCase("video_url");
    }

    /**
     * Ensures the JSON serialised by /api/products/{slug} does NOT contain "video_url" or "videoUrl".
     */
    @Test
    @DisplayName("SECURITY: GET /api/products/{slug} JSON response must not contain video_url")
    void getProductBySlug_responseJson_mustNotContainVideoUrl() throws Exception {
        PublicProductDetailResponse detail = new PublicProductDetailResponse(
                1L, "Physics Video", "physics-video",
                "A great video", ProductType.VIDEO,
                BigDecimal.valueOf(999.00), null,
                List.of(),
                null, null,
                3600, "Detailed video description"
        );
        when(catalogService.getPublicProductBySlug(eq("physics-video"))).thenReturn(detail);

        String body = mockMvc.perform(get("/api/products/physics-video").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .as("Public product detail response must not expose video_url (FR-VID-06)")
                .doesNotContainIgnoringCase("videoUrl")
                .doesNotContainIgnoringCase("video_url");
    }

    /**
     * Ensures stock_quantity never appears in public responses.
     */
    @Test
    @DisplayName("SECURITY: Public response DTOs must not declare stockQuantity field")
    void publicDtos_mustNotHaveStockQuantityField() {
        // Check detail response
        boolean detailHasStock = false;
        for (java.lang.reflect.RecordComponent rc : PublicProductDetailResponse.class.getRecordComponents()) {
            if (rc.getName().equalsIgnoreCase("stockQuantity") ||
                    rc.getName().equalsIgnoreCase("stock_quantity")) {
                detailHasStock = true;
                break;
            }
        }
        assertThat(detailHasStock)
                .as("PublicProductDetailResponse must NOT contain stockQuantity (expose inStock boolean instead)")
                .isFalse();

        // Check summary response
        boolean summaryHasStock = false;
        for (java.lang.reflect.RecordComponent rc : PublicProductSummaryResponse.class.getRecordComponents()) {
            if (rc.getName().equalsIgnoreCase("stockQuantity") ||
                    rc.getName().equalsIgnoreCase("stock_quantity")) {
                summaryHasStock = true;
                break;
            }
        }
        assertThat(summaryHasStock)
                .as("PublicProductSummaryResponse must NOT contain stockQuantity (expose inStock boolean instead)")
                .isFalse();
    }

    // -------------------------------------------------------------------------
    // Accessibility: unauthenticated users can hit public endpoints
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/products is accessible without authentication")
    void listProducts_noAuth_returns200() throws Exception {
        when(catalogService.listPublicProducts(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/products/{slug} is accessible without authentication")
    void getProductBySlug_noAuth_returns200() throws Exception {
        PublicProductDetailResponse detail = new PublicProductDetailResponse(
                1L, "A Note", "a-note", "desc", ProductType.PHYSICAL_NOTE,
                BigDecimal.TEN, null, List.of(), 100, true, null, null
        );
        when(catalogService.getPublicProductBySlug(eq("a-note"))).thenReturn(detail);

        mockMvc.perform(get("/api/products/a-note").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/chapters/{id}/products is accessible without authentication")
    void listChapterProducts_noAuth_returns200() throws Exception {
        when(catalogService.getPublicProductsForChapter(eq(1L))).thenReturn(List.of());

        mockMvc.perform(get("/api/chapters/1/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // 400 for invalid type / sort
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/products?type=INVALID returns 400")
    void listProducts_invalidType_returns400() throws Exception {
        mockMvc.perform(get("/api/products?type=INVALID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/products?sort=INVALID returns 400")
    void listProducts_invalidSort_returns400() throws Exception {
        when(catalogService.listPublicProducts(any(), any(), any(), any(), eq("INVALID"), any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid sort value"));

        mockMvc.perform(get("/api/products?sort=INVALID").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/products/{slug} returns 404 when product is missing, DRAFT, or ARCHIVED")
    void getProductBySlug_notFound_returns404() throws Exception {
        when(catalogService.getPublicProductBySlug(eq("draft-or-missing")))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Product not found"));

        mockMvc.perform(get("/api/products/draft-or-missing").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/chapters/{chapterId}/products returns 404 when chapter is INACTIVE or does not exist")
    void getChapterProducts_inactive_returns404() throws Exception {
        when(catalogService.getPublicProductsForChapter(eq(999L)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Chapter not found"));

        mockMvc.perform(get("/api/chapters/999/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
