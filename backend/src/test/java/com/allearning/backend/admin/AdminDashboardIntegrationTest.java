package com.allearning.backend.admin;

import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.catalog.entity.Note;
import com.allearning.backend.catalog.entity.Product;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.repository.NoteRepository;
import com.allearning.backend.catalog.repository.ProductRepository;
import com.allearning.backend.classes.entity.ClassEntity;
import com.allearning.backend.classes.entity.ClassRegistration;
import com.allearning.backend.classes.entity.ClassStatus;
import com.allearning.backend.classes.entity.ClassType;
import com.allearning.backend.classes.entity.RegistrationStatus;
import com.allearning.backend.classes.repository.ClassRegistrationRepository;
import com.allearning.backend.classes.repository.ClassRepository;
import com.allearning.backend.commerce.entity.*;
import com.allearning.backend.commerce.repository.OrderItemRepository;
import com.allearning.backend.commerce.repository.OrderRepository;
import com.allearning.backend.commerce.repository.PaymentRepository;
import com.allearning.backend.communication.entity.Announcement;
import com.allearning.backend.communication.entity.AnnouncementStatus;
import com.allearning.backend.communication.entity.ContactMessage;
import com.allearning.backend.communication.entity.ContactMessageStatus;
import com.allearning.backend.communication.repository.AnnouncementRepository;
import com.allearning.backend.communication.repository.ContactMessageRepository;
import com.allearning.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admindashboardintegrationtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "app.timezone=Asia/Colombo",
        "app.dashboard.low-stock-threshold=5"
})
public class AdminDashboardIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ClassRegistrationRepository classRegistrationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    private User adminUser;
    private User studentUser;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        // Clear in FK-safe order
        contactMessageRepository.deleteAll();
        announcementRepository.deleteAll();
        classRegistrationRepository.deleteAll();
        classRepository.deleteAll();
        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        noteRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User("Admin Dash", "admin.dash@example.com", "hash", "0771234567", UserRole.ADMIN, AccountStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateToken(adminUser);

        studentUser = new User("Student Sam", "sam.dash@example.com", "hash", "0777654321", UserRole.STUDENT, AccountStatus.ACTIVE);
        studentUser.setEmailVerified(false); // unverified for metric testing
        studentUser = userRepository.save(studentUser);
        studentToken = jwtService.generateToken(studentUser);
    }

    @Test
    @DisplayName("Security: Guest gets 401, student gets 403, admin gets 200 on all dashboard endpoints")
    void securityGuardsTest() throws Exception {
        // Summary
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Revenue
        mockMvc.perform(get("/api/admin/dashboard/revenue?days=30"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/dashboard/revenue?days=30")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard/revenue?days=30")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Top products
        mockMvc.perform(get("/api/admin/dashboard/top-products?days=30&limit=5"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/dashboard/top-products?days=30&limit=5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard/top-products?days=30&limit=5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Validation: Invalid days (15) or limit (50) returns 400 Bad Request")
    void validationInvalidParamsTest() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/revenue?days=15")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/dashboard/top-products?days=15&limit=5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/dashboard/top-products?days=30&limit=50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/dashboard/top-products?days=30&limit=0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Business definitions: Revenue only includes PAID orders with PAID payments; excludes expired, refunded, failed, pending")
    void revenueBusinessDefinitionsTest() throws Exception {
        ZoneId zone = ZoneId.of("Asia/Colombo");
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime todayNow = today.atTime(12, 0).atZone(zone).toOffsetDateTime();

        // 1. Order 1: PAID order, total 2500 (PAID payment at today 12:00) -> MUST count in revenue
        Order o1 = createOrder(studentUser.getUserId(), PaymentStatus.PAID, FulfillmentStatus.PENDING, new BigDecimal("2500.00"), todayNow);
        createPayment(o1.getOrderId(), PaymentRecordStatus.PAID, new BigDecimal("2500.00"), todayNow, false);

        // 2. Order 2: PENDING order, total 1500 (payment PENDING) -> MUST NOT count in revenue
        Order o2 = createOrder(studentUser.getUserId(), PaymentStatus.PENDING, FulfillmentStatus.NOT_REQUIRED, new BigDecimal("1500.00"), todayNow);
        o2.setExpiresAt(OffsetDateTime.now().plusHours(2));
        orderRepository.save(o2);
        createPayment(o2.getOrderId(), PaymentRecordStatus.PENDING, new BigDecimal("1500.00"), null, false);

        // 3. Order 3: EXPIRED order, total 3000, but has a late PAID payment needing review -> MUST NOT count in revenue
        Order o3 = createOrder(studentUser.getUserId(), PaymentStatus.EXPIRED, FulfillmentStatus.NOT_REQUIRED, new BigDecimal("3000.00"), todayNow.minusHours(5));
        createPayment(o3.getOrderId(), PaymentRecordStatus.PAID, new BigDecimal("3000.00"), todayNow, true);

        // 4. Order 4: FAILED order, total 1000 -> MUST NOT count
        Order o4 = createOrder(studentUser.getUserId(), PaymentStatus.FAILED, FulfillmentStatus.NOT_REQUIRED, new BigDecimal("1000.00"), todayNow);
        createPayment(o4.getOrderId(), PaymentRecordStatus.FAILED, new BigDecimal("1000.00"), null, false);

        // 5. Order 5: REFUNDED order, total 4000 -> MUST NOT count
        Order o5 = createOrder(studentUser.getUserId(), PaymentStatus.REFUNDED, FulfillmentStatus.NOT_REQUIRED, new BigDecimal("4000.00"), todayNow);
        Payment p5 = createPayment(o5.getOrderId(), PaymentRecordStatus.REFUNDED, new BigDecimal("4000.00"), todayNow, false);
        p5.setRefundedAt(todayNow.plusMinutes(10));
        paymentRepository.save(p5);

        // Query summary
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sales.revenueToday", is(2500.00)))
                .andExpect(jsonPath("$.sales.revenueAllTime", is(2500.00)))
                .andExpect(jsonPath("$.sales.paidOrdersCount", is(1)))
                .andExpect(jsonPath("$.sales.awaitingPaymentCount", is(1)))
                .andExpect(jsonPath("$.pendingActions.ordersAwaitingFulfillment", is(1)))
                .andExpect(jsonPath("$.pendingActions.paymentsNeedingReview", is(1)));

        // Refunding order 1 removes it from revenue
        o1.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(o1);

        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sales.revenueToday", is(0.00)))
                .andExpect(jsonPath("$.sales.revenueAllTime", is(0.00)))
                .andExpect(jsonPath("$.sales.paidOrdersCount", is(0)));
    }

    @Test
    @DisplayName("Timezone & Daily Revenue: Payment at 23:30 Asia/Colombo is counted on that local day; exactly requested days zero-filled")
    void dailyRevenueTimezoneAndZeroFillTest() throws Exception {
        ZoneId zone = ZoneId.of("Asia/Colombo");
        LocalDate today = LocalDate.now(zone);

        // Create a payment at 23:30 Asia/Colombo time today
        OffsetDateTime todayLate = today.atTime(23, 30).atZone(zone).toOffsetDateTime();
        Order o = createOrder(studentUser.getUserId(), PaymentStatus.PAID, FulfillmentStatus.NOT_REQUIRED, new BigDecimal("5000.00"), todayLate);
        createPayment(o.getOrderId(), PaymentRecordStatus.PAID, new BigDecimal("5000.00"), todayLate, false);

        mockMvc.perform(get("/api/admin/dashboard/revenue?days=7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries", hasSize(7)))
                .andExpect(jsonPath("$.totalRevenue", is(5000.00)))
                .andExpect(jsonPath("$.totalPaidOrders", is(1)))
                // The last entry is today's local date
                .andExpect(jsonPath("$.entries[6].date", is(today.toString())))
                .andExpect(jsonPath("$.entries[6].revenue", is(5000.00)))
                .andExpect(jsonPath("$.entries[6].paidOrderCount", is(1)))
                // Previous days are zero-filled
                .andExpect(jsonPath("$.entries[0].revenue", is(0.00)))
                .andExpect(jsonPath("$.entries[0].paidOrderCount", is(0)));
    }

    @Test
    @DisplayName("Low stock panel: lists only PUBLISHED notes at or below threshold (<= 5), lowest first, at most 5 shown")
    void lowStockCatalogTest() throws Exception {
        // Product 1: PUBLISHED note with stock 2 (low stock)
        Product p1 = createNoteProduct("Biology Vol 1", "bio-vol-1", ProductStatus.PUBLISHED, 2);
        // Product 2: PUBLISHED note with stock 5 (low stock, boundary)
        Product p2 = createNoteProduct("Chemistry Vol 1", "chem-vol-1", ProductStatus.PUBLISHED, 5);
        // Product 3: PUBLISHED note with stock 10 (sufficient stock -> excluded)
        createNoteProduct("Physics Vol 1", "phys-vol-1", ProductStatus.PUBLISHED, 10);
        // Product 4: DRAFT note with stock 1 (DRAFT -> excluded)
        createNoteProduct("Maths Vol 1", "maths-vol-1", ProductStatus.DRAFT, 1);
        // Product 5: PUBLISHED note with stock 0 (out of stock, lowest)
        Product p5 = createNoteProduct("English Vol 1", "eng-vol-1", ProductStatus.PUBLISHED, 0);

        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalog.lowStockCount", is(3)))
                .andExpect(jsonPath("$.catalog.lowStockItems", hasSize(3)))
                // Lowest stock first: 0 -> 2 -> 5
                .andExpect(jsonPath("$.catalog.lowStockItems[0].productId", is(p5.getProductId().intValue())))
                .andExpect(jsonPath("$.catalog.lowStockItems[0].stock", is(0)))
                .andExpect(jsonPath("$.catalog.lowStockItems[1].productId", is(p1.getProductId().intValue())))
                .andExpect(jsonPath("$.catalog.lowStockItems[1].stock", is(2)))
                .andExpect(jsonPath("$.catalog.lowStockItems[2].productId", is(p2.getProductId().intValue())))
                .andExpect(jsonPath("$.catalog.lowStockItems[2].stock", is(5)));
    }

    @Test
    @DisplayName("Privacy: No email, phone, address, or message text appears anywhere in dashboard responses")
    void privacyGuardsTest() throws Exception {
        ZoneId zone = ZoneId.of("Asia/Colombo");
        OffsetDateTime now = OffsetDateTime.now(zone);

        // Seed an order with customer details
        Order o = createOrder(studentUser.getUserId(), PaymentStatus.PAID, FulfillmentStatus.PENDING, new BigDecimal("1200.00"), now);
        o.setShipAddressLine1("123 Secret St, Colombo");
        o.setShipPhone("0719998888");
        orderRepository.save(o);
        createPayment(o.getOrderId(), PaymentRecordStatus.PAID, new BigDecimal("1200.00"), now, false);

        // Seed a class registration
        ClassEntity cl = new ClassEntity(1L, "Combined Maths 2026", "Desc", ClassType.ONLINE, "Saturdays 8am", new BigDecimal("3500.00"), ClassStatus.ACTIVE);
        cl = classRepository.save(cl);
        ClassRegistration reg = new ClassRegistration(studentUser.getUserId(), cl, RegistrationStatus.PENDING);
        classRegistrationRepository.save(reg);

        // Seed a contact message with secret text
        ContactMessage msg = new ContactMessage("Secret Sender", "secret.sender@example.com", "0770001111", "SUPER_CONFIDENTIAL_MESSAGE_CONTENT");
        contactMessageRepository.save(msg);

        // Execute GET /summary
        String responseContent = mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Strictly verify that sensitive strings are absent
        assertFalse(responseContent.contains("secret.sender@example.com"), "Dashboard must not leak sender email");
        assertFalse(responseContent.contains("0770001111"), "Dashboard must not leak sender phone");
        assertFalse(responseContent.contains("SUPER_CONFIDENTIAL_MESSAGE_CONTENT"), "Dashboard must not leak contact message text");
        assertFalse(responseContent.contains("123 Secret St"), "Dashboard must not leak shipping address");
        assertFalse(responseContent.contains("0719998888"), "Dashboard must not leak order ship phone");
        assertFalse(responseContent.contains("sam.dash@example.com"), "Dashboard must not leak student email");
    }

    @Test
    @DisplayName("Top products: returns top products by quantity sold in PAID orders within period")
    void topProductsTest() throws Exception {
        ZoneId zone = ZoneId.of("Asia/Colombo");
        OffsetDateTime now = OffsetDateTime.now(zone);

        Product p1 = createNoteProduct("Item A", "item-a", ProductStatus.PUBLISHED, 50);
        Product p2 = createNoteProduct("Item B", "item-b", ProductStatus.PUBLISHED, 50);

        Order o = createOrder(studentUser.getUserId(), PaymentStatus.PAID, FulfillmentStatus.DELIVERED, new BigDecimal("5500.00"), now);
        createPayment(o.getOrderId(), PaymentRecordStatus.PAID, new BigDecimal("5500.00"), now, false);

        // Add 3 of p1 and 5 of p2 to order
        OrderItem oi1 = new OrderItem(p1.getProductId(), p1.getName(), p1.getProductType().name(), 3, new BigDecimal("1000.00"), new BigDecimal("3000.00"));
        oi1.setOrder(o);
        OrderItem oi2 = new OrderItem(p2.getProductId(), p2.getName(), p2.getProductType().name(), 5, new BigDecimal("500.00"), new BigDecimal("2500.00"));
        oi2.setOrder(o);
        orderItemRepository.save(oi1);
        orderItemRepository.save(oi2);

        mockMvc.perform(get("/api/admin/dashboard/top-products?days=30&limit=5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // Item B sold 5 (top)
                .andExpect(jsonPath("$[0].productId", is(p2.getProductId().intValue())))
                .andExpect(jsonPath("$[0].quantitySold", is(5)))
                .andExpect(jsonPath("$[0].revenue", is(2500.00)))
                // Item A sold 3
                .andExpect(jsonPath("$[1].productId", is(p1.getProductId().intValue())))
                .andExpect(jsonPath("$[1].quantitySold", is(3)))
                .andExpect(jsonPath("$[1].revenue", is(3000.00)));
    }

    // --- Helper Methods ---

    private Order createOrder(Long userId, PaymentStatus paymentStatus, FulfillmentStatus fulfillmentStatus, BigDecimal total, OffsetDateTime createdAt) {
        Order o = new Order();
        o.setUserId(userId);
        o.setPaymentStatus(paymentStatus);
        o.setFulfillmentStatus(fulfillmentStatus);
        o.setSubtotal(total);
        o.setDeliveryFee(BigDecimal.ZERO);
        o.setTotalAmount(total);
        o.setExpiresAt(createdAt.plusMinutes(30));
        o.setShipRecipientName("Recipient " + userId);
        o.setShipCity("Colombo");
        o = orderRepository.save(o);
        return o;
    }

    private Payment createPayment(Long orderId, PaymentRecordStatus status, BigDecimal amount, OffsetDateTime paidAt, boolean needsReview) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setPaymentStatus(status);
        p.setAmount(amount);
        p.setPaidAt(paidAt);
        p.setNeedsReview(needsReview);
        p.setPaymentMethod("PAYHERE");
        return paymentRepository.save(p);
    }

    private Product createNoteProduct(String name, String slug, ProductStatus status, int stock) {
        Product p = new Product();
        p.setName(name);
        p.setSlug(slug);
        p.setPrice(new BigDecimal("1000.00"));
        p.setProductType(ProductType.PHYSICAL_NOTE);
        p.setStatus(status);
        p = productRepository.save(p);

        Note n = new Note(p, 100, stock);
        noteRepository.save(n);
        return p;
    }
}
