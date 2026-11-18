package com.allearning.backend.commerce;

import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.catalog.dto.ProductPurchaseFacts;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.dto.FulfillmentUpdateRequest;
import com.allearning.backend.commerce.dto.PaymentReviewRequest;
import com.allearning.backend.commerce.entity.*;
import com.allearning.backend.commerce.repository.OrderItemRepository;
import com.allearning.backend.commerce.repository.OrderRepository;
import com.allearning.backend.commerce.repository.PaymentRepository;
import com.allearning.backend.commerce.service.VideoOwnershipService;
import com.allearning.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admincommerceintegrationtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "app.order.payment-timeout-minutes=30",
        "app.delivery.flat-fee=150.00"
})
public class AdminCommerceIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private VideoOwnershipService videoOwnershipService;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private CatalogService catalogService;

    private MockMvc mockMvc;

    private User adminUser;
    private User studentUser;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)
                .build();

        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User("Admin Officer", "admin@example.com", "$2a$10$dummyHash", null, UserRole.ADMIN, AccountStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateToken(adminUser);

        studentUser = new User("Student Learner", "student@example.com", "$2a$10$dummyHash", "0771234567", UserRole.STUDENT, AccountStatus.ACTIVE);
        studentUser.setEmailVerified(true);
        studentUser = userRepository.save(studentUser);
        studentToken = jwtService.generateToken(studentUser);

        when(catalogService.getProductPurchaseFacts(anyLong()))
                .thenReturn(Optional.of(new ProductPurchaseFacts(1L, "Test Note", "test-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.PUBLISHED, new BigDecimal("1000.00"), null, 50)));
    }

    private Order createTestOrder(PaymentStatus paymentStatus, FulfillmentStatus fulfillmentStatus) {
        Order order = new Order();
        order.setUserId(studentUser.getUserId());
        order.setPaymentStatus(paymentStatus);
        order.setFulfillmentStatus(fulfillmentStatus);
        order.setSubtotal(new BigDecimal("1000.00"));
        order.setDeliveryFee(new BigDecimal("150.00"));
        order.setTotalAmount(new BigDecimal("1150.00"));
        order.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
        order = orderRepository.save(order);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(1L);
        item.setProductName("Test Note");
        item.setProductType("PHYSICAL_NOTE");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("1000.00"));
        item.setSubtotal(new BigDecimal("1000.00"));
        orderItemRepository.save(item);

        return order;
    }

    private Payment createTestPayment(Long orderId, PaymentRecordStatus status, boolean needsReview, String reviewReason) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setPaymentMethod("PAYHERE");
        p.setPaymentStatus(status);
        p.setAmount(new BigDecimal("1150.00"));
        p.setTransactionReference("PH-REF-" + System.nanoTime());
        p.setNeedsReview(needsReview);
        p.setReviewReason(reviewReason);
        if (status == PaymentRecordStatus.PAID) {
            p.setPaidAt(OffsetDateTime.now());
        }
        return paymentRepository.save(p);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Security tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin endpoints require ADMIN role: guest -> 401, student -> 403, admin -> 200")
    void testSecurityEnforcement() throws Exception {
        // Guest (no token) -> 401
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/payments"))
                .andExpect(status().isUnauthorized());

        // Student token -> 403
        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Admin token -> 200
        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fulfillment lifecycle tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Fulfillment transition PENDING -> PROCESSING -> SHIPPED -> DELIVERED works")
    void testFulfillmentValidTransitions() throws Exception {
        Order order = createTestOrder(PaymentStatus.PAID, FulfillmentStatus.PENDING);

        // 1. PENDING -> PROCESSING
        mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", order.getOrderId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("PROCESSING", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fulfillment_status").value("PROCESSING"));

        // 2. PROCESSING -> SHIPPED with trackingNote
        mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", order.getOrderId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("SHIPPED", "PROMPT-X-9988"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fulfillment_status").value("SHIPPED"))
                .andExpect(jsonPath("$.tracking_note").value("PROMPT-X-9988"));

        // Verify student can see trackingNote
        mockMvc.perform(get("/api/orders/{id}", order.getOrderId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracking_note").value("PROMPT-X-9988"))
                .andExpect(jsonPath("$.fulfillment_status").value("SHIPPED"));

        // 3. SHIPPED -> DELIVERED
        mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", order.getOrderId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("DELIVERED", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fulfillment_status").value("DELIVERED"));
    }

    @Test
    @DisplayName("Invalid fulfillment transitions: skipping or going backward -> 409 INVALID_TRANSITION")
    void testInvalidTransitions() throws Exception {
        Order order = createTestOrder(PaymentStatus.PAID, FulfillmentStatus.PENDING);

        // Skipping PENDING -> SHIPPED directly -> 409
        mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", order.getOrderId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("SHIPPED", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));

        // Move to PROCESSING
        mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", order.getOrderId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("PROCESSING", null))))
                .andExpect(status().isOk());

        // Going backward PROCESSING -> PENDING -> 409
        mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", order.getOrderId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("PENDING", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    @DisplayName("Non-fulfillable orders (NOT_REQUIRED, unpaid, EXPIRED) -> 409 ORDER_NOT_FULFILLABLE")
    void testNonFulfillableOrders() throws Exception {
        // Unpaid order (PENDING payment)
        Order unpaidOrder = createTestOrder(PaymentStatus.PENDING, FulfillmentStatus.PENDING);
        mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", unpaidOrder.getOrderId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("PROCESSING", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FULFILLABLE"));

        // Video-only order (NOT_REQUIRED)
        Order videoOrder = createTestOrder(PaymentStatus.PAID, FulfillmentStatus.NOT_REQUIRED);
        mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", videoOrder.getOrderId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("PROCESSING", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FULFILLABLE"));

        // Expired order
        Order expiredOrder = createTestOrder(PaymentStatus.EXPIRED, FulfillmentStatus.PENDING);
        mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", expiredOrder.getOrderId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("PROCESSING", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FULFILLABLE"));
    }

    @Test
    @DisplayName("trackingNote is rejected on any status except SHIPPED (400)")
    void testTrackingNoteRejectedOnNonShipped() throws Exception {
        Order order = createTestOrder(PaymentStatus.PAID, FulfillmentStatus.PENDING);

        // Passing trackingNote when moving to PROCESSING -> 400
        mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", order.getOrderId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("PROCESSING", "INVALID_TRACKING"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRACKING_NOTE_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("Concurrency: two simultaneous fulfillment updates on one order result in a single valid transition")
    void testFulfillmentConcurrency() throws Exception {
        Order order = createTestOrder(PaymentStatus.PAID, FulfillmentStatus.PENDING);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = List.of(
                () -> {
                    barrier.await();
                    int code = mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", order.getOrderId())
                                    .header("Authorization", "Bearer " + adminToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("PROCESSING", null))))
                            .andReturn().getResponse().getStatus();
                    if (code == 200) successCount.incrementAndGet();
                    else if (code == 409) conflictCount.incrementAndGet();
                    return null;
                },
                () -> {
                    barrier.await();
                    int code = mockMvc.perform(patch("/api/admin/orders/{id}/fulfillment-status", order.getOrderId())
                                    .header("Authorization", "Bearer " + adminToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(new FulfillmentUpdateRequest("PROCESSING", null))))
                            .andReturn().getResponse().getStatus();
                    if (code == 200) successCount.incrementAndGet();
                    else if (code == 409) conflictCount.incrementAndGet();
                    return null;
                }
        );

        for (Future<Void> f : executor.invokeAll(tasks)) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one update should succeed");
        assertEquals(1, conflictCount.get(), "The concurrent duplicate transition should receive 409");

        Order updated = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(FulfillmentStatus.PROCESSING, updated.getFulfillmentStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Payment review tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolve-review requires note (400) and flagged payment (409 if not flagged)")
    void testResolveReviewValidation() throws Exception {
        Order order = createTestOrder(PaymentStatus.PAID, FulfillmentStatus.PENDING);

        // 1. Payment not flagged -> 409
        Payment unflaggedPayment = createTestPayment(order.getOrderId(), PaymentRecordStatus.PAID, false, null);
        mockMvc.perform(patch("/api/admin/payments/{id}/resolve-review", unflaggedPayment.getPaymentId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentReviewRequest("Resolved manually"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FLAGGED"));

        // 2. Flagged payment without note -> 400
        Payment flaggedPayment = createTestPayment(order.getOrderId(), PaymentRecordStatus.PAID, true, "LATE_PAYMENT_NOT_FULFILLABLE");
        mockMvc.perform(patch("/api/admin/payments/{id}/resolve-review", flaggedPayment.getPaymentId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentReviewRequest("   "))))
                .andExpect(status().isBadRequest());

        // 3. Valid resolve-review -> 200 and audit stored
        mockMvc.perform(patch("/api/admin/payments/{id}/resolve-review", flaggedPayment.getPaymentId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentReviewRequest("Customer agreed to manual delivery"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needs_review").value(false))
                .andExpect(jsonPath("$.review_note").value("Customer agreed to manual delivery"))
                .andExpect(jsonPath("$.reviewed_at").isNotEmpty());

        Payment saved = paymentRepository.findById(flaggedPayment.getPaymentId()).orElseThrow();
        assertFalse(saved.isNeedsReview());
        assertEquals("Customer agreed to manual delivery", saved.getReviewNote());
        assertEquals(adminUser.getUserId(), saved.getReviewedBy());
        assertNotNull(saved.getReviewedAt());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Payment refund tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Refund: PAID payment -> REFUNDED, order -> REFUNDED, video access revoked")
    void testRefundWorkflow() throws Exception {
        // Setup order with PAID payment and a video item to verify video ownership removal
        Order order = createTestOrder(PaymentStatus.PAID, FulfillmentStatus.NOT_REQUIRED);

        // Add a video item to order
        OrderItem videoItem = new OrderItem();
        videoItem.setOrder(order);
        videoItem.setProductId(99L);
        videoItem.setProductName("Math Video Course");
        videoItem.setProductType("VIDEO");
        videoItem.setQuantity(1);
        videoItem.setUnitPrice(new BigDecimal("500.00"));
        videoItem.setSubtotal(new BigDecimal("500.00"));
        orderItemRepository.save(videoItem);

        // Verify video ownership exists while order is PAID
        assertTrue(videoOwnershipService.userOwnsVideo(studentUser.getUserId(), 99L));

        Payment payment = createTestPayment(order.getOrderId(), PaymentRecordStatus.PAID, true, "CHARGEBACK");

        // Refund request without note -> 400
        mockMvc.perform(patch("/api/admin/payments/{id}/refund", payment.getPaymentId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentReviewRequest(""))))
                .andExpect(status().isBadRequest());

        // Perform valid refund
        mockMvc.perform(patch("/api/admin/payments/{id}/refund", payment.getPaymentId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentReviewRequest("Refund processed in PayHere"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment_status").value("REFUNDED"))
                .andExpect(jsonPath("$.needs_review").value(false))
                .andExpect(jsonPath("$.refunded_at").isNotEmpty());

        // Order is now REFUNDED
        Order refundedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, refundedOrder.getPaymentStatus());

        // Video ownership is revoked automatically!
        assertFalse(videoOwnershipService.userOwnsVideo(studentUser.getUserId(), 99L));
    }

    @Test
    @DisplayName("Refund non-PAID payment -> 409; refund on EXPIRED order leaves order EXPIRED")
    void testRefundEdgeCases() throws Exception {
        // 1. Payment that is PENDING (not PAID) -> 409
        Order pendingOrder = createTestOrder(PaymentStatus.PENDING, FulfillmentStatus.PENDING);
        Payment pendingPayment = createTestPayment(pendingOrder.getOrderId(), PaymentRecordStatus.PENDING, false, null);

        mockMvc.perform(patch("/api/admin/payments/{id}/refund", pendingPayment.getPaymentId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentReviewRequest("Refund attempt"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_REFUNDABLE"));

        // 2. Late payment scenario: Payment is PAID with needsReview=true, but order is EXPIRED
        Order expiredOrder = createTestOrder(PaymentStatus.EXPIRED, FulfillmentStatus.PENDING);
        Payment latePayment = createTestPayment(expiredOrder.getOrderId(), PaymentRecordStatus.PAID, true, "LATE_PAYMENT_NOT_FULFILLABLE");

        mockMvc.perform(patch("/api/admin/payments/{id}/refund", latePayment.getPaymentId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentReviewRequest("PayHere refund issued"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment_status").value("REFUNDED"));

        // Expired order must stay EXPIRED!
        Order afterRefund = orderRepository.findById(expiredOrder.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.EXPIRED, afterRefund.getPaymentStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filter tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin order filters: q by email/id, needsReview, invalid enum -> 400")
    void testOrderFilters() throws Exception {
        Order order1 = createTestOrder(PaymentStatus.PAID, FulfillmentStatus.PENDING);
        createTestOrder(PaymentStatus.PENDING, FulfillmentStatus.NOT_REQUIRED);
        createTestPayment(order1.getOrderId(), PaymentRecordStatus.PAID, true, "REVIEW_FLAG");

        // Invalid paymentStatus -> 400
        mockMvc.perform(get("/api/admin/orders?paymentStatus=BOGUS_STATUS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // Invalid fulfillmentStatus -> 400
        mockMvc.perform(get("/api/admin/orders?fulfillmentStatus=INVALID")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // Filter by q (customer email)
        mockMvc.perform(get("/api/admin/orders?q=student@example.com")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Filter by q (exact order ID)
        mockMvc.perform(get("/api/admin/orders?q=" + order1.getOrderId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].order_id").value(order1.getOrderId()));

        // Filter by needsReview=true
        mockMvc.perform(get("/api/admin/orders?needsReview=true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].order_id").value(order1.getOrderId()))
                .andExpect(jsonPath("$.content[0].needs_review").value(true));
    }
}
