package com.allearning.backend.commerce;

import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.catalog.dto.AdminPlaybackResponse;
import com.allearning.backend.catalog.dto.VideoProductFacts;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.entity.Order;
import com.allearning.backend.commerce.entity.OrderItem;
import com.allearning.backend.commerce.entity.Payment;
import com.allearning.backend.commerce.entity.PaymentStatus;
import com.allearning.backend.commerce.entity.PaymentRecordStatus;
import com.allearning.backend.commerce.repository.OrderRepository;
import com.allearning.backend.commerce.repository.PaymentRepository;
import com.allearning.backend.integration.video.SignedEmbedResult;
import com.allearning.backend.integration.video.VideoPlaybackProvider;
import com.allearning.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:videoplaybacktestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "bunny.stream.library-id=12345",
        "bunny.stream.token-key=testtokenkey",
        "app.video.embed-ttl-minutes=120"
})
public class VideoPlaybackIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CatalogService catalogService;

    @MockitoBean
    private VideoPlaybackProvider videoPlaybackProvider;

    private MockMvc mockMvc;
    private User student;
    private User admin;
    private String studentToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)
                .build();

        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();

        student = new User(
                "Video Student",
                "student-video@example.com",
                "$2a$10$dummyHash",
                "0771234567",
                UserRole.STUDENT,
                AccountStatus.ACTIVE
        );
        student.setEmailVerified(true);
        student = userRepository.save(student);
        studentToken = "Bearer " + jwtService.generateToken(student);

        admin = new User(
                "Video Admin",
                "admin-video@example.com",
                "$2a$10$dummyHash",
                null,
                UserRole.ADMIN,
                AccountStatus.ACTIVE
        );
        admin.setEmailVerified(true);
        admin = userRepository.save(admin);
        adminToken = "Bearer " + jwtService.generateToken(admin);

        when(videoPlaybackProvider.isConfigured()).thenReturn(true);
        when(videoPlaybackProvider.signEmbedUrl(any(), any(Duration.class)))
                .thenReturn(new SignedEmbedResult(
                        "https://iframe.mediadelivery.net/embed/12345/32d140e2-e4f4-4eec-9d53-20371e9be607?token=abcdef1234567890&expires=1799999999",
                        OffsetDateTime.now().plusHours(2)
                ));
        when(videoPlaybackProvider.signEmbedUrl(any(), anyInt()))
                .thenReturn(new SignedEmbedResult(
                        "https://iframe.mediadelivery.net/embed/12345/32d140e2-e4f4-4eec-9d53-20371e9be607?token=abcdef1234567890&expires=1799999999",
                        OffsetDateTime.now().plusHours(2)
                ));
    }

    @Test
    @DisplayName("Role RBAC: guest gets 401, admin gets 403 on student endpoints, student gets 403 on admin playback")
    void roleAccessControl_enforcedCorrectly() throws Exception {
        // Guest on student endpoint -> 401
        mockMvc.perform(get("/api/my/videos"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/my/videos/10"))
                .andExpect(status().isUnauthorized());

        // Admin on student endpoint -> 403
        mockMvc.perform(get("/api/my/videos").header("Authorization", adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/my/videos/10").header("Authorization", adminToken))
                .andExpect(status().isForbidden());

        // Student on admin preview endpoint -> 403
        mockMvc.perform(get("/api/admin/products/10/playback").header("Authorization", studentToken))
                .andExpect(status().isForbidden());

        // Admin on admin preview endpoint -> 200
        when(catalogService.getAdminPlayback(10L)).thenReturn(new AdminPlaybackResponse(
                "https://iframe.mediadelivery.net/embed/12345/32d140e2?token=abc&expires=123",
                OffsetDateTime.now().plusHours(2)
        ));
        mockMvc.perform(get("/api/admin/products/10/playback").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embedUrl").isNotEmpty());
    }

    @Test
    @DisplayName("Ownership: PAID order appears in My Videos and plays; PENDING, EXPIRED or REFUNDED orders do not")
    void ownershipLifecycle_paidOrderPlays_refundedRevokes() throws Exception {
        Long videoProductId = 100L;
        String providerVideoId = "32d140e2-e4f4-4eec-9d53-20371e9be607";

        // Mock catalog service facts
        VideoProductFacts facts = new VideoProductFacts(
                videoProductId,
                "Organic Chemistry Masterclass",
                "organic-chemistry-masterclass",
                "https://example.com/thumb.jpg",
                7200,
                "Full organic chemistry course",
                ProductStatus.PUBLISHED,
                providerVideoId
        );
        when(catalogService.getVideoProductFacts(videoProductId)).thenReturn(Optional.of(facts));
        when(catalogService.getVideoProductFacts(List.of(videoProductId))).thenReturn(Map.of(videoProductId, facts));

        // 1. Initially student owns no videos
        mockMvc.perform(get("/api/my/videos").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/my/videos/" + videoProductId).header("Authorization", studentToken))
                .andExpect(status().isForbidden());

        // 2. Create a PENDING order containing the video -> still not owned
        Order pendingOrder = createOrder(student.getUserId(), PaymentStatus.PENDING, videoProductId, "VIDEO");
        mockMvc.perform(get("/api/my/videos").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/my/videos/" + videoProductId).header("Authorization", studentToken))
                .andExpect(status().isForbidden());

        // 3. Mark order as PAID -> owned!
        pendingOrder.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(pendingOrder);

        Payment payment = new Payment(pendingOrder.getOrderId(), "PAYHERE", new BigDecimal("2500.00"));
        payment.setPaymentStatus(PaymentRecordStatus.PAID);
        payment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/my/videos").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(videoProductId))
                .andExpect(jsonPath("$[0].name").value("Organic Chemistry Masterclass"))
                .andExpect(jsonPath("$[0].duration_seconds").value(7200))
                .andExpect(jsonPath("$[0].purchased_at").isNotEmpty())
                // Ensure no video IDs, tokens, or embed URLs in the summary
                .andExpect(jsonPath("$[0].providerVideoId").doesNotExist())
                .andExpect(jsonPath("$[0].provider_video_id").doesNotExist())
                .andExpect(jsonPath("$[0].embedUrl").doesNotExist())
                .andExpect(jsonPath("$[0].token").doesNotExist());

        // Playback endpoint -> returns 200 with embedUrl and Cache-Control: no-store
        mockMvc.perform(get("/api/my/videos/" + videoProductId).header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.productId").value(videoProductId))
                .andExpect(jsonPath("$.embedUrl").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());

        // 4. Refund the order -> ownership is revoked
        pendingOrder.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(pendingOrder);

        mockMvc.perform(get("/api/my/videos").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/my/videos/" + videoProductId).header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ARCHIVED video purchased by student continues to play (FR-VID-05, BR-10)")
    void archivedVideoPurchased_stillPlays() throws Exception {
        Long videoProductId = 200L;
        String providerVideoId = "4742a81b-bf15-42fe-8b1c-8fcb9024c550";

        VideoProductFacts archivedFacts = new VideoProductFacts(
                videoProductId,
                "Archived Old Syllabus Video",
                "archived-old-syllabus",
                "https://example.com/archived.jpg",
                3600,
                "Old syllabus tutorial",
                ProductStatus.ARCHIVED,
                providerVideoId
        );
        when(catalogService.getVideoProductFacts(videoProductId)).thenReturn(Optional.of(archivedFacts));
        when(catalogService.getVideoProductFacts(List.of(videoProductId))).thenReturn(Map.of(videoProductId, archivedFacts));

        Order paidOrder = createOrder(student.getUserId(), PaymentStatus.PAID, videoProductId, "VIDEO");
        Payment payment = new Payment(paidOrder.getOrderId(), "PAYHERE", new BigDecimal("1500.00"));
        payment.setPaymentStatus(PaymentRecordStatus.PAID);
        payment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/my/videos").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(videoProductId));

        mockMvc.perform(get("/api/my/videos/" + videoProductId).header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embedUrl").isNotEmpty());
    }

    @Test
    @DisplayName("Playback returns 503 VIDEO_PLAYBACK_NOT_CONFIGURED when provider is not configured")
    void unconfiguredProvider_returns503() throws Exception {
        Long videoProductId = 300L;
        String providerVideoId = "32d140e2-e4f4-4eec-9d53-20371e9be607";

        VideoProductFacts facts = new VideoProductFacts(
                videoProductId,
                "Test Video",
                "test-video",
                null,
                1800,
                "Desc",
                ProductStatus.PUBLISHED,
                providerVideoId
        );
        when(catalogService.getVideoProductFacts(videoProductId)).thenReturn(Optional.of(facts));
        when(videoPlaybackProvider.signEmbedUrl(any(), any(Duration.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "VIDEO_PLAYBACK_NOT_CONFIGURED"));
        when(videoPlaybackProvider.signEmbedUrl(any(), anyInt()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "VIDEO_PLAYBACK_NOT_CONFIGURED"));

        Order paidOrder = createOrder(student.getUserId(), PaymentStatus.PAID, videoProductId, "VIDEO");
        Payment payment = new Payment(paidOrder.getOrderId(), "PAYHERE", new BigDecimal("1000.00"));
        payment.setPaymentStatus(PaymentRecordStatus.PAID);
        payment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/my/videos/" + videoProductId).header("Authorization", studentToken))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Requesting playback for non-existent product or physical note returns 404")
    void nonVideoProduct_returns404() throws Exception {
        when(catalogService.getVideoProductFacts(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/my/videos/999").header("Authorization", studentToken))
                .andExpect(status().isNotFound());
    }

    private Order createOrder(Long userId, PaymentStatus status, Long productId, String productType) {
        Order order = new Order();
        order.setUserId(userId);
        order.setPaymentStatus(status);
        order.setSubtotal(new BigDecimal("2500.00"));
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("2500.00"));
        order.setExpiresAt(OffsetDateTime.now().plusMinutes(30));

        OrderItem item = new OrderItem(
                productId,
                "Sample Video Item",
                productType,
                1,
                new BigDecimal("2500.00"),
                new BigDecimal("2500.00")
        );
        order.addItem(item);
        return orderRepository.save(order);
    }
}
