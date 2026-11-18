package com.allearning.backend.commerce;

import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.catalog.dto.ProductPurchaseFacts;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.dto.CartItemAddRequest;
import com.allearning.backend.commerce.dto.PaymentCreateRequest;
import com.allearning.backend.commerce.entity.*;
import com.allearning.backend.commerce.repository.*;
import com.allearning.backend.commerce.service.VideoOwnershipService;
import com.allearning.backend.integration.payment.PayHereHashService;
import com.allearning.backend.integration.payment.PayHereProperties;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:paymentintegrationtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "payhere.merchant-id=1211143",
        "payhere.merchant-secret=testsecret456",
        "payhere.mode=sandbox",
        "app.public.backend-url=http://localhost:8080",
        "app.public.frontend-url=http://localhost:5173"
})
class PaymentIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentNotificationRepository paymentNotificationRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private PayHereHashService payHereHashService;

    @Autowired
    private PayHereProperties payHereProperties;

    @Autowired
    private VideoOwnershipService videoOwnershipService;

    @MockitoBean
    private CatalogService catalogService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User studentA;
    private User studentB;
    private User adminUser;

    private String tokenA;
    private String tokenB;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        paymentNotificationRepository.deleteAll();
        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        studentA = new User("John Doe", "johndoe@test.com", "hash", "0771234567", UserRole.STUDENT, AccountStatus.ACTIVE);
        studentA.setEmailVerified(true);
        studentA = userRepository.save(studentA);
        tokenA = "Bearer " + jwtService.generateToken(studentA);

        studentB = new User("Jane Smith", "janesmith@test.com", "hash", "0777654321", UserRole.STUDENT, AccountStatus.ACTIVE);
        studentB.setEmailVerified(true);
        studentB = userRepository.save(studentB);
        tokenB = "Bearer " + jwtService.generateToken(studentB);

        adminUser = new User("Admin", "admin@test.com", "hash", null, UserRole.ADMIN, AccountStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser = userRepository.save(adminUser);
        tokenAdmin = "Bearer " + jwtService.generateToken(adminUser);
    }

    private Order createTestOrder(User user, PaymentStatus paymentStatus, OffsetDateTime expiresAt) {
        Order order = new Order();
        order.setUserId(user.getUserId());
        order.setPaymentStatus(paymentStatus);
        order.setFulfillmentStatus(FulfillmentStatus.NOT_REQUIRED);
        order.setSubtotal(new BigDecimal("2500.00"));
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("2500.00"));
        order.setExpiresAt(expiresAt != null ? expiresAt : OffsetDateTime.now().plusMinutes(30));
        order.setShipRecipientName("John Doe");
        order.setShipPhone("0771234567");
        order.setShipAddressLine1("123 Galle Road");
        order.setShipCity("Colombo");
        order.setShipDistrict("Colombo");
        order.setShipPostalCode("00300");

        OrderItem item = new OrderItem(100L, "Physics Video Course", "VIDEO", 1,
                new BigDecimal("2500.00"), new BigDecimal("2500.00"));
        order.addItem(item);

        return orderRepository.save(order);
    }

    @Test
    @DisplayName("POST /api/payments/create: authentication and authorization controls")
    void testCreatePaymentAuth() throws Exception {
        Order order = createTestOrder(studentA, PaymentStatus.PENDING, null);

        // 1. Guest -> 401
        mockMvc.perform(post("/api/payments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": " + order.getOrderId() + "}"))
                .andExpect(status().isUnauthorized());

        // 2. Admin -> 403
        mockMvc.perform(post("/api/payments/create")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": " + order.getOrderId() + "}"))
                .andExpect(status().isForbidden());

        // 3. Other student -> 404
        mockMvc.perform(post("/api/payments/create")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": " + order.getOrderId() + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/payments/create: valid student request generates PayHere checkout parameters")
    void testCreatePaymentSuccess() throws Exception {
        Order order = createTestOrder(studentA, PaymentStatus.PENDING, null);

        mockMvc.perform(post("/api/payments/create")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": " + order.getOrderId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://sandbox.payhere.lk/pay/checkout"))
                .andExpect(jsonPath("$.fields.merchant_id").value("1211143"))
                .andExpect(jsonPath("$.fields.order_id").value(order.getOrderId().toString()))
                .andExpect(jsonPath("$.fields.amount").value("2500.00"))
                .andExpect(jsonPath("$.fields.currency").value("LKR"))
                .andExpect(jsonPath("$.fields.first_name").value("John"))
                .andExpect(jsonPath("$.fields.last_name").value("Doe"))
                .andExpect(jsonPath("$.fields.email").value("johndoe@test.com"))
                .andExpect(jsonPath("$.fields.items").value("Order #" + order.getOrderId()))
                .andExpect(jsonPath("$.fields.hash").isString())
                // Ensure merchant secret never appears in response
                .andExpect(jsonPath("$.fields.merchant_secret").doesNotExist())
                .andExpect(jsonPath("$.merchantSecret").doesNotExist());

        // Verify PENDING payment row created
        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getOrderId());
        assertEquals(1, payments.size());
        assertEquals(PaymentRecordStatus.PENDING, payments.get(0).getPaymentStatus());
        assertEquals(new BigDecimal("2500.00"), payments.get(0).getAmount());

        // Calling create again reuses the pending row
        mockMvc.perform(post("/api/payments/create")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": " + order.getOrderId() + "}"))
                .andExpect(status().isOk());

        payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getOrderId());
        assertEquals(1, payments.size(), "Pending payment record should be reused without creating duplicates");
    }

    @Test
    @DisplayName("POST /api/payments/create: order not payable or expired returns 409")
    void testCreatePaymentNotPayableOrExpired() throws Exception {
        // Already PAID order -> 409 ORDER_NOT_PAYABLE
        Order paidOrder = createTestOrder(studentA, PaymentStatus.PAID, null);
        mockMvc.perform(post("/api/payments/create")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": " + paidOrder.getOrderId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_PAYABLE"));

        // EXPIRED order -> 409 ORDER_EXPIRED
        Order expiredOrder = createTestOrder(studentA, PaymentStatus.PENDING, OffsetDateTime.now().minusMinutes(5));
        mockMvc.perform(post("/api/payments/create")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": " + expiredOrder.getOrderId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_EXPIRED"));
    }

    @Test
    @DisplayName("POST /api/payments/webhook: invalid signature returns 400 and changes no data")
    void testWebhookInvalidSignature() throws Exception {
        Order order = createTestOrder(studentA, PaymentStatus.PENDING, null);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", "PH123456")
                        .param("payhere_amount", "2500.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", "INVALID_TAMPERED_SIG"))
                .andExpect(status().isBadRequest());

        // Verify order remained PENDING
        Order freshOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, freshOrder.getPaymentStatus());

        // Verify audit log has REJECTED_SIGNATURE
        List<PaymentNotification> notifications = paymentNotificationRepository.findAll();
        assertEquals(1, notifications.size());
        assertFalse(notifications.get(0).isSignatureValid());
        assertEquals("REJECTED_SIGNATURE", notifications.get(0).getOutcome());
    }

    @Test
    @DisplayName("POST /api/payments/webhook: valid signature with status 2 marks order and payment PAID")
    void testWebhookValidPaymentSuccess() throws Exception {
        Order order = createTestOrder(studentA, PaymentStatus.PENDING, null);

        String paymentId = "PAYHERE_TXN_001";
        String sig = payHereHashService.generateNotificationSignature(
                "1211143",
                order.getOrderId().toString(),
                "2500.00",
                "LKR",
                "2",
                "testsecret456"
        );

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", paymentId)
                        .param("payhere_amount", "2500.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", sig))
                .andExpect(status().isOk());

        // Verify order is PAID
        Order updatedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, updatedOrder.getPaymentStatus());

        // Verify payment is PAID with transaction reference
        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getOrderId()).orElseThrow();
        assertEquals(PaymentRecordStatus.PAID, payment.getPaymentStatus());
        assertEquals(paymentId, payment.getTransactionReference());
        assertNotNull(payment.getPaidAt());
        assertFalse(payment.isNeedsReview());

        // Duplicate delivery changes nothing (idempotent, BR-13)
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", paymentId)
                        .param("payhere_amount", "2500.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", sig))
                .andExpect(status().isOk());

        Order unchangedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, unchangedOrder.getPaymentStatus());

        // Check audit logs
        List<PaymentNotification> notifications = paymentNotificationRepository.findAll();
        assertEquals(2, notifications.size());
        assertEquals("APPLIED", notifications.get(0).getOutcome());
        assertEquals("DUPLICATE_IGNORED", notifications.get(1).getOutcome());
    }

        @Test
        @DisplayName("POST /api/payments/webhook: concurrent duplicate success notifications apply once")
        void testConcurrentDuplicateSuccessNotifications() throws Exception {
                Order order = createTestOrder(studentA, PaymentStatus.PENDING, null);
                String paymentId = "CONCURRENT_SUCCESS_001";
                String sig = payHereHashService.generateNotificationSignature(
                                "1211143", order.getOrderId().toString(), "2500.00", "LKR", "2", "testsecret456"
                );

                ExecutorService executor = Executors.newFixedThreadPool(2);
                try {
                        var first = CompletableFuture.runAsync(() -> postSuccessNotification(order, paymentId, sig), executor);
                        var second = CompletableFuture.runAsync(() -> postSuccessNotification(order, paymentId, sig), executor);
                        CompletableFuture.allOf(first, second).join();
                } finally {
                        executor.shutdownNow();
                }

                assertEquals(PaymentStatus.PAID, orderRepository.findById(order.getOrderId()).orElseThrow().getPaymentStatus());
                List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getOrderId());
                assertEquals(1, payments.size());
                assertEquals(PaymentRecordStatus.PAID, payments.get(0).getPaymentStatus());
                assertEquals(1, payments.stream().filter(p -> paymentId.equals(p.getTransactionReference())).count());
                assertEquals(2, paymentNotificationRepository.findAll().size());
        }

        private void postSuccessNotification(Order order, String paymentId, String signature) {
                try {
                        mockMvc.perform(post("/api/payments/webhook")
                                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                                        .param("merchant_id", "1211143")
                                                        .param("order_id", order.getOrderId().toString())
                                                        .param("payment_id", paymentId)
                                                        .param("payhere_amount", "2500.00")
                                                        .param("payhere_currency", "LKR")
                                                        .param("status_code", "2")
                                                        .param("md5sig", signature))
                                        .andExpect(status().isOk());
                } catch (Exception e) {
                        throw new IllegalStateException(e);
                }
        }

    @Test
    @DisplayName("POST /api/payments/webhook: failed/cancelled status handling")
    void testWebhookFailedOrCancelledNotifications() throws Exception {
        Order order = createTestOrder(studentA, PaymentStatus.PENDING, null);

        // Pre-create PENDING payment record
        Payment pendingPayment = new Payment(order.getOrderId(), "PAYHERE", order.getTotalAmount());
        paymentRepository.save(pendingPayment);

        // Status -2 (failed) before PAID -> marks payment FAILED, order stays PENDING
        String sigFailed = payHereHashService.generateNotificationSignature(
                "1211143", order.getOrderId().toString(), "2500.00", "LKR", "-2", "testsecret456"
        );

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", "FAIL_001")
                        .param("payhere_amount", "2500.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "-2")
                        .param("md5sig", sigFailed))
                .andExpect(status().isOk());

        Order freshOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, freshOrder.getPaymentStatus(), "Order remains PENDING so student can retry");

        Payment freshPayment = paymentRepository.findById(pendingPayment.getPaymentId()).orElseThrow();
        assertEquals(PaymentRecordStatus.FAILED, freshPayment.getPaymentStatus());

        // Now pay successfully
        String sigSuccess = payHereHashService.generateNotificationSignature(
                "1211143", order.getOrderId().toString(), "2500.00", "LKR", "2", "testsecret456"
        );

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", "SUCCESS_001")
                        .param("payhere_amount", "2500.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", sigSuccess))
                .andExpect(status().isOk());

        freshOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, freshOrder.getPaymentStatus());

        // Later failed or cancelled notification arrives out of order -> MUST NOT downgrade PAID order or payment
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", "OLD_FAIL_002")
                        .param("payhere_amount", "2500.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "-2")
                        .param("md5sig", sigFailed))
                .andExpect(status().isOk());

        freshOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, freshOrder.getPaymentStatus(), "PAID order must not be downgraded");
    }

    @Test
    @DisplayName("POST /api/payments/webhook: amount mismatch flags needs_review and never marks PAID")
    void testWebhookAmountMismatch() throws Exception {
        Order order = createTestOrder(studentA, PaymentStatus.PENDING, null);

        // Sign with tampered lower amount "1000.00" instead of "2500.00"
        String sig = payHereHashService.generateNotificationSignature(
                "1211143", order.getOrderId().toString(), "1000.00", "LKR", "2", "testsecret456"
        );

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", "MISMATCH_001")
                        .param("payhere_amount", "1000.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", sig))
                .andExpect(status().isOk());

        Order freshOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, freshOrder.getPaymentStatus());

        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getOrderId()).orElseThrow();
        assertNotEquals(PaymentRecordStatus.PAID, payment.getPaymentStatus());
        assertTrue(payment.isNeedsReview());
        assertEquals("AMOUNT_MISMATCH", payment.getReviewReason());
    }

    @Test
    @DisplayName("POST /api/payments/webhook: late payment on EXPIRED order revives when fulfillable")
    void testLatePaymentRevived() throws Exception {
        Order order = createTestOrder(studentA, PaymentStatus.EXPIRED, OffsetDateTime.now().minusHours(1));

        String sig = payHereHashService.generateNotificationSignature(
                "1211143", order.getOrderId().toString(), "2500.00", "LKR", "2", "testsecret456"
        );

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", "LATE_REVIVE_001")
                        .param("payhere_amount", "2500.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", sig))
                .andExpect(status().isOk());

        Order freshOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, freshOrder.getPaymentStatus());

        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getOrderId()).orElseThrow();
        assertEquals(PaymentRecordStatus.PAID, payment.getPaymentStatus());
        assertFalse(payment.isNeedsReview());
    }

    @Test
    @DisplayName("POST /api/payments/webhook: late payment on EXPIRED order flags needs_review when stock unavailable")
    void testLatePaymentNeedsReviewWhenStockUnavailable() throws Exception {
        Order order = new Order();
        order.setUserId(studentA.getUserId());
        order.setPaymentStatus(PaymentStatus.EXPIRED);
        order.setFulfillmentStatus(FulfillmentStatus.PENDING);
        order.setSubtotal(new BigDecimal("1500.00"));
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("1500.00"));
        order.setExpiresAt(OffsetDateTime.now().minusHours(1));

        OrderItem physicalItem = new OrderItem(200L, "Chemistry Physical Note", "PHYSICAL_NOTE", 2,
                new BigDecimal("750.00"), new BigDecimal("1500.00"));
        order.addItem(physicalItem);
        order = orderRepository.save(order);

        // Catalog stock reservation fails
        when(catalogService.reserveStock(eq(200L), anyInt())).thenReturn(false);

        String sig = payHereHashService.generateNotificationSignature(
                "1211143", order.getOrderId().toString(), "1500.00", "LKR", "2", "testsecret456"
        );

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", "LATE_UNFULFILLABLE_001")
                        .param("payhere_amount", "1500.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", sig))
                .andExpect(status().isOk());

                mockMvc.perform(post("/api/payments/webhook")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("merchant_id", "1211143")
                                .param("order_id", order.getOrderId().toString())
                                .param("payment_id", "LATE_UNFULFILLABLE_001")
                                .param("payhere_amount", "1500.00")
                                .param("payhere_currency", "LKR")
                                .param("status_code", "2")
                                .param("md5sig", sig))
                        .andExpect(status().isOk());

        Order freshOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.EXPIRED, freshOrder.getPaymentStatus(), "Order must remain EXPIRED");

        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getOrderId()).orElseThrow();
        assertEquals(PaymentRecordStatus.PAID, payment.getPaymentStatus());
        assertTrue(payment.isNeedsReview());
        assertEquals("LATE_PAYMENT_NOT_FULFILLABLE", payment.getReviewReason());
        assertEquals("DUPLICATE_IGNORED", paymentNotificationRepository.findAll().get(1).getOutcome());
    }

    @Test
    @DisplayName("POST /api/payments/webhook: late physical payment reserves stock in ascending product order")
    void testLatePhysicalPaymentReservesStockInAscendingOrder() throws Exception {
        Order order = new Order();
        order.setUserId(studentA.getUserId());
        order.setPaymentStatus(PaymentStatus.EXPIRED);
        order.setFulfillmentStatus(FulfillmentStatus.PENDING);
        order.setSubtotal(new BigDecimal("2000.00"));
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("2000.00"));
        order.setExpiresAt(OffsetDateTime.now().minusHours(1));
        order.addItem(new OrderItem(400L, "Notes 400", "PHYSICAL_NOTE", 1,
                new BigDecimal("1000.00"), new BigDecimal("1000.00")));
        order.addItem(new OrderItem(300L, "Notes 300", "PHYSICAL_NOTE", 1,
                new BigDecimal("1000.00"), new BigDecimal("1000.00")));
        order = orderRepository.save(order);

        when(catalogService.reserveStock(300L, 1)).thenReturn(true);
        when(catalogService.reserveStock(400L, 1)).thenReturn(true);
        String sig = payHereHashService.generateNotificationSignature(
                "1211143", order.getOrderId().toString(), "2000.00", "LKR", "2", "testsecret456"
        );

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", "LATE_PHYSICAL_001")
                        .param("payhere_amount", "2000.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", sig))
                .andExpect(status().isOk());

        assertEquals(PaymentStatus.PAID, orderRepository.findById(order.getOrderId()).orElseThrow().getPaymentStatus());
        var stockReservations = inOrder(catalogService);
        stockReservations.verify(catalogService).reserveStock(300L, 1);
        stockReservations.verify(catalogService).reserveStock(400L, 1);
    }

    @Test
    @DisplayName("GET /api/payments/{id}: student sees own payment; admin sees review info; stranger gets 404")
    void testGetPaymentDetails() throws Exception {
        Order order = createTestOrder(studentA, PaymentStatus.PENDING, null);
        Payment payment = new Payment(order.getOrderId(), "PAYHERE", order.getTotalAmount());
        payment.setNeedsReview(true);
        payment.setReviewReason("AMOUNT_MISMATCH");
        payment = paymentRepository.save(payment);

        // 1. Guest -> 401
        mockMvc.perform(get("/api/payments/" + payment.getPaymentId()))
                .andExpect(status().isUnauthorized());

        // 2. Stranger student -> 404 (SEC-06)
        mockMvc.perform(get("/api/payments/" + payment.getPaymentId())
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // 3. Owner student -> 200 (needs_review omitted)
        mockMvc.perform(get("/api/payments/" + payment.getPaymentId())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getPaymentId()))
                .andExpect(jsonPath("$.orderId").value(order.getOrderId()))
                .andExpect(jsonPath("$.amount").value(2500.00))
                .andExpect(jsonPath("$.needsReview").doesNotExist())
                .andExpect(jsonPath("$.reviewReason").doesNotExist());

        // 4. Admin -> 200 (needs_review and review_reason included)
        mockMvc.perform(get("/api/payments/" + payment.getPaymentId())
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getPaymentId()))
                .andExpect(jsonPath("$.needsReview").value(true))
                .andExpect(jsonPath("$.reviewReason").value("AMOUNT_MISMATCH"));
    }

    @Test
    @DisplayName("End-to-end: order -> paid notification -> VideoOwnershipService true -> add to cart returns 409")
    void testVideoOwnershipAfterPaymentBlocksCartAdd() throws Exception {
        Long videoProductId = 55L;

        // Setup catalog facts for the video product
        when(catalogService.getProductPurchaseFacts(videoProductId)).thenReturn(Optional.of(
                new ProductPurchaseFacts(
                        videoProductId,
                        "Advanced Biology Video Course",
                        "advanced-biology-video",
                        ProductType.VIDEO,
                        ProductStatus.PUBLISHED,
                        new BigDecimal("3000.00"),
                        null,
                        null
                )
        ));

        // Create order containing this video
        Order order = new Order();
        order.setUserId(studentA.getUserId());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setFulfillmentStatus(FulfillmentStatus.NOT_REQUIRED);
        order.setSubtotal(new BigDecimal("3000.00"));
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("3000.00"));
        order.setExpiresAt(OffsetDateTime.now().plusMinutes(30));

        OrderItem item = new OrderItem(videoProductId, "Advanced Biology Video Course", "VIDEO", 1,
                new BigDecimal("3000.00"), new BigDecimal("3000.00"));
        order.addItem(item);
        order = orderRepository.save(order);

        // Before payment -> user does not own video
        assertFalse(videoOwnershipService.userOwnsVideo(studentA.getUserId(), videoProductId));

        // Send valid signed payment notification (status 2)
        String sig = payHereHashService.generateNotificationSignature(
                "1211143", order.getOrderId().toString(), "3000.00", "LKR", "2", "testsecret456"
        );

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1211143")
                        .param("order_id", order.getOrderId().toString())
                        .param("payment_id", "PH_OWNERSHIP_001")
                        .param("payhere_amount", "3000.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", sig))
                .andExpect(status().isOk());

        // After payment -> video ownership is now TRUE!
        assertTrue(videoOwnershipService.userOwnsVideo(studentA.getUserId(), videoProductId));

        // Adding this video to cart returns 409 VIDEO_ALREADY_OWNED
        CartItemAddRequest cartReq = new CartItemAddRequest(videoProductId, 1);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VIDEO_ALREADY_OWNED"));
    }

    @Test
    @DisplayName("POST /api/payments/create: returns 503 with PAYMENTS_NOT_CONFIGURED when gateway not configured")
    void testCreatePaymentNotConfigured() {
        PayHereProperties unconfiguredProps = new PayHereProperties("", "", "sandbox", "http://localhost:8080", "http://localhost:5173");
        com.allearning.backend.integration.payment.PayHerePaymentGateway unconfiguredGateway =
                new com.allearning.backend.integration.payment.PayHerePaymentGateway(unconfiguredProps, payHereHashService);
        com.allearning.backend.commerce.service.PaymentServiceImpl unconfiguredService =
                new com.allearning.backend.commerce.service.PaymentServiceImpl(
                        unconfiguredGateway, paymentRepository, paymentNotificationRepository,
                        orderRepository, userRepository, catalogService, videoOwnershipService
                );

        Order order = createTestOrder(studentA, PaymentStatus.PENDING, null);
        com.allearning.backend.commerce.exception.PaymentException ex = assertThrows(
                com.allearning.backend.commerce.exception.PaymentException.class,
                () -> unconfiguredService.createPayment(studentA.getUserId(), order.getOrderId())
        );

        assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("PAYMENTS_NOT_CONFIGURED", ex.getErrorCode());

        PayHereProperties missingPublicUrls = new PayHereProperties(
                "1211143", "testsecret456", "sandbox", "", ""
        );
        assertFalse(missingPublicUrls.isConfigured(), "Checkout must not use localhost or empty callback URLs");
    }

        @Test
        @DisplayName("POST /api/payments/create: missing required phone does not create a payment")
        void testCreatePaymentRequiresPhone() throws Exception {
                studentA.setPhone(null);
                userRepository.save(studentA);
                Order order = createTestOrder(studentA, PaymentStatus.PENDING, null);
                order.setShipPhone(null);
                orderRepository.save(order);

                mockMvc.perform(post("/api/payments/create")
                                                .header("Authorization", tokenA)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"orderId\": " + order.getOrderId() + "}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("CUSTOMER_PHONE_REQUIRED"));

                assertTrue(paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getOrderId()).isEmpty());
        }
}
