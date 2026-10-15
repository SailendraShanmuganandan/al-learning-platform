package com.allearning.backend.commerce;

import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.catalog.dto.ProductPurchaseFacts;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.entity.*;
import com.allearning.backend.commerce.repository.CartItemRepository;
import com.allearning.backend.commerce.repository.CartRepository;
import com.allearning.backend.commerce.repository.OrderItemRepository;
import com.allearning.backend.commerce.repository.OrderRepository;
import com.allearning.backend.commerce.service.OrderExpiryService;
import com.allearning.backend.commerce.service.OrderService;
import com.allearning.backend.security.JwtService;
import com.allearning.backend.user.entity.Address;
import com.allearning.backend.user.repository.AddressRepository;
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
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:orderintegrationtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "app.order.payment-timeout-minutes=30",
        "app.delivery.flat-fee=150.00"
})
class OrderIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderExpiryService orderExpiryService;

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private CatalogService catalogService;

    private MockMvc mockMvc;
    private User verifiedStudentA;
    private User verifiedStudentB;
    private User unverifiedStudent;
    private User adminUser;

    private String tokenA;
    private String tokenB;
    private String tokenUnverified;
    private String tokenAdmin;

    private Address addressA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();

        // Users
        verifiedStudentA = new User("Student A", "studenta@orders.com", "hash", "0771234567", UserRole.STUDENT, AccountStatus.ACTIVE);
        verifiedStudentA.setEmailVerified(true);
        verifiedStudentA = userRepository.save(verifiedStudentA);
        tokenA = "Bearer " + jwtService.generateToken(verifiedStudentA);

        verifiedStudentB = new User("Student B", "studentb@orders.com", "hash", "0777654321", UserRole.STUDENT, AccountStatus.ACTIVE);
        verifiedStudentB.setEmailVerified(true);
        verifiedStudentB = userRepository.save(verifiedStudentB);
        tokenB = "Bearer " + jwtService.generateToken(verifiedStudentB);

        unverifiedStudent = new User("Unverified Student", "unverified@orders.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        unverifiedStudent.setEmailVerified(false);
        unverifiedStudent = userRepository.save(unverifiedStudent);
        tokenUnverified = "Bearer " + jwtService.generateToken(unverifiedStudent);

        adminUser = new User("Admin", "admin@orders.com", "hash", null, UserRole.ADMIN, AccountStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser = userRepository.save(adminUser);
        tokenAdmin = "Bearer " + jwtService.generateToken(adminUser);

        // Address for Student A
        addressA = new Address(
                verifiedStudentA.getUserId(),
                "John Doe",
                "+94771234567",
                "123 Galle Road",
                "Apt 4B",
                "Colombo",
                "Colombo",
                "00300",
                true
        );
        addressA = addressRepository.save(addressA);
    }

    @Test
    @DisplayName("Checkout with Note + Video creates order with snapshots, delivery fee, reserves stock, and converts cart")
    void testCheckoutNoteAndVideo() throws Exception {
        Long noteId = 101L;
        Long videoId = 202L;

        ProductPurchaseFacts noteFacts = new ProductPurchaseFacts(
                noteId, "Combined Science Notes", "science-notes", ProductType.PHYSICAL_NOTE,
                ProductStatus.PUBLISHED, new BigDecimal("1000.00"), "/img/notes.jpg", 10
        );
        ProductPurchaseFacts videoFacts = new ProductPurchaseFacts(
                videoId, "Calculus Masterclass Video", "calc-video", ProductType.VIDEO,
                ProductStatus.PUBLISHED, new BigDecimal("2500.00"), "/img/calc.jpg", null
        );

        when(catalogService.getProductPurchaseFacts(noteId)).thenReturn(Optional.of(noteFacts));
        when(catalogService.getProductPurchaseFacts(videoId)).thenReturn(Optional.of(videoFacts));
        when(catalogService.reserveStock(eq(noteId), eq(2))).thenReturn(true);

        // Populate active cart
        Cart cart = new Cart(verifiedStudentA.getUserId(), CartStatus.ACTIVE);
        CartItem item1 = new CartItem(cart, noteId, 2, new BigDecimal("1000.00"));
        CartItem item2 = new CartItem(cart, videoId, 1, new BigDecimal("2500.00"));
        cart.addItem(item1);
        cart.addItem(item2);
        cartRepository.save(cart);

        // Checkout request
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\": " + addressA.getAddressId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order_id", notNullValue()))
                .andExpect(jsonPath("$.user_id", is(verifiedStudentA.getUserId().intValue())))
                .andExpect(jsonPath("$.payment_status", is("PENDING")))
                .andExpect(jsonPath("$.fulfillment_status", is("PENDING")))
                .andExpect(jsonPath("$.subtotal", is(4500.00)))
                .andExpect(jsonPath("$.delivery_fee", is(150.00)))
                .andExpect(jsonPath("$.total_amount", is(4650.00)))
                .andExpect(jsonPath("$.ship_recipient_name", is("John Doe")))
                .andExpect(jsonPath("$.ship_address_line_1", is("123 Galle Road")))
                .andExpect(jsonPath("$.items", hasSize(2)));

        verify(catalogService).reserveStock(noteId, 2);

        // Cart is marked CONVERTED
        Cart updatedCart = cartRepository.findById(cart.getCartId()).orElseThrow();
        assertEquals(CartStatus.CONVERTED, updatedCart.getStatus());
    }

    @Test
    @DisplayName("Video-only order: fulfillment NOT_REQUIRED, 0 delivery fee, and requires no address")
    void testVideoOnlyCheckout() throws Exception {
        Long videoId = 303L;
        ProductPurchaseFacts videoFacts = new ProductPurchaseFacts(
                videoId, "Organic Chemistry Video", "org-chem-video", ProductType.VIDEO,
                ProductStatus.PUBLISHED, new BigDecimal("3200.00"), "/img/chem.jpg", null
        );

        when(catalogService.getProductPurchaseFacts(videoId)).thenReturn(Optional.of(videoFacts));

        Cart cart = new Cart(verifiedStudentA.getUserId(), CartStatus.ACTIVE);
        CartItem item = new CartItem(cart, videoId, 1, new BigDecimal("3200.00"));
        cart.addItem(item);
        cartRepository.save(cart);

        // No addressId sent
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fulfillment_status", is("NOT_REQUIRED")))
                .andExpect(jsonPath("$.delivery_fee", is(0.00)))
                .andExpect(jsonPath("$.total_amount", is(3200.00)))
                .andExpect(jsonPath("$.ship_recipient_name").doesNotExist())
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @DisplayName("Empty cart returns 409 EMPTY_CART")
    void testEmptyCartCheckout() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("EMPTY_CART")));
    }

    @Test
    @DisplayName("Missing address when physical note in cart returns 400 ADDRESS_REQUIRED")
    void testMissingAddressForPhysicalNote() throws Exception {
        Long noteId = 404L;
        ProductPurchaseFacts noteFacts = new ProductPurchaseFacts(
                noteId, "Physics Notes", "physics-notes", ProductType.PHYSICAL_NOTE,
                ProductStatus.PUBLISHED, new BigDecimal("1200.00"), "/img/p.jpg", 10
        );
        when(catalogService.getProductPurchaseFacts(noteId)).thenReturn(Optional.of(noteFacts));

        Cart cart = new Cart(verifiedStudentA.getUserId(), CartStatus.ACTIVE);
        cart.addItem(new CartItem(cart, noteId, 1, new BigDecimal("1200.00")));
        cartRepository.save(cart);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("ADDRESS_REQUIRED")));
    }

    @Test
    @DisplayName("Using another user's addressId returns 404")
    void testForeignAddressReturns404() throws Exception {
        Long noteId = 405L;
        ProductPurchaseFacts noteFacts = new ProductPurchaseFacts(
                noteId, "Physics Notes", "physics-notes", ProductType.PHYSICAL_NOTE,
                ProductStatus.PUBLISHED, new BigDecimal("1200.00"), "/img/p.jpg", 10
        );
        when(catalogService.getProductPurchaseFacts(noteId)).thenReturn(Optional.of(noteFacts));

        Cart cart = new Cart(verifiedStudentB.getUserId(), CartStatus.ACTIVE);
        cart.addItem(new CartItem(cart, noteId, 1, new BigDecimal("1200.00")));
        cartRepository.save(cart);

        // Student B tries to use Student A's address
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\": " + addressA.getAddressId() + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Price change returns 409 PRICE_CHANGED and refreshes cart item price")
    void testPriceChangedConflict() throws Exception {
        Long noteId = 505L;
        // Current price is now 1500.00 instead of 1000.00 in cart
        ProductPurchaseFacts updatedFacts = new ProductPurchaseFacts(
                noteId, "Math Revision Notes", "math-notes", ProductType.PHYSICAL_NOTE,
                ProductStatus.PUBLISHED, new BigDecimal("1500.00"), "/img/m.jpg", 10
        );
        when(catalogService.getProductPurchaseFacts(noteId)).thenReturn(Optional.of(updatedFacts));

        Cart cart = new Cart(verifiedStudentA.getUserId(), CartStatus.ACTIVE);
        CartItem cartItem = new CartItem(cart, noteId, 1, new BigDecimal("1000.00"));
        cart.addItem(cartItem);
        cartRepository.save(cart);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\": " + addressA.getAddressId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("PRICE_CHANGED")))
                .andExpect(jsonPath("$.oldPrice", is(1000.00)))
                .andExpect(jsonPath("$.newPrice", is(1500.00)));

        assertEquals(0, orderRepository.count());

        // Verify cart line unit price was updated
        Cart refreshedCart = cartRepository.findByUserIdAndStatus(verifiedStudentA.getUserId(), CartStatus.ACTIVE).orElseThrow();
        assertEquals(new BigDecimal("1500.00"), refreshedCart.getItems().get(0).getUnitPrice());
    }

    @Test
    @DisplayName("Unpublished product returns 409 PRODUCT_UNAVAILABLE")
    void testUnpublishedProductUnavailable() throws Exception {
        Long noteId = 606L;
        ProductPurchaseFacts draftFacts = new ProductPurchaseFacts(
                noteId, "Draft Notes", "draft-notes", ProductType.PHYSICAL_NOTE,
                ProductStatus.DRAFT, new BigDecimal("1000.00"), null, 10
        );
        when(catalogService.getProductPurchaseFacts(noteId)).thenReturn(Optional.of(draftFacts));

        Cart cart = new Cart(verifiedStudentA.getUserId(), CartStatus.ACTIVE);
        cart.addItem(new CartItem(cart, noteId, 1, new BigDecimal("1000.00")));
        cartRepository.save(cart);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\": " + addressA.getAddressId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("PRODUCT_UNAVAILABLE")));
    }

    @Test
    @DisplayName("Video already held by a PENDING order returns 409 VIDEO_PENDING_ORDER with pendingOrderId")
    void testVideoPendingOrderConflict() throws Exception {
        Long videoId = 707L;
        ProductPurchaseFacts videoFacts = new ProductPurchaseFacts(
                videoId, "Mechanics Video", "mech-video", ProductType.VIDEO,
                ProductStatus.PUBLISHED, new BigDecimal("2000.00"), null, null
        );
        when(catalogService.getProductPurchaseFacts(videoId)).thenReturn(Optional.of(videoFacts));

        // Create an existing PENDING order with this video
        Order existingOrder = new Order();
        existingOrder.setUserId(verifiedStudentA.getUserId());
        existingOrder.setPaymentStatus(PaymentStatus.PENDING);
        existingOrder.setFulfillmentStatus(FulfillmentStatus.NOT_REQUIRED);
        existingOrder.setSubtotal(new BigDecimal("2000.00"));
        existingOrder.setDeliveryFee(BigDecimal.ZERO);
        existingOrder.setTotalAmount(new BigDecimal("2000.00"));
        existingOrder.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
        existingOrder.addItem(new OrderItem(videoId, "Mechanics Video", "VIDEO", 1, new BigDecimal("2000.00"), new BigDecimal("2000.00")));
        existingOrder = orderRepository.save(existingOrder);

        // Now user adds it to active cart again and tries to checkout
        Cart cart = new Cart(verifiedStudentA.getUserId(), CartStatus.ACTIVE);
        cart.addItem(new CartItem(cart, videoId, 1, new BigDecimal("2000.00")));
        cartRepository.save(cart);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("VIDEO_PENDING_ORDER")))
                .andExpect(jsonPath("$.pendingOrderId", is(existingOrder.getOrderId().intValue())));
    }

    @Test
    @DisplayName("Insufficient stock on reservation failure returns 409 INSUFFICIENT_STOCK and rolls back")
    void testInsufficientStockRollsBack() throws Exception {
        Long noteId = 808L;
        ProductPurchaseFacts noteFacts = new ProductPurchaseFacts(
                noteId, "Limited Notes", "limited-notes", ProductType.PHYSICAL_NOTE,
                ProductStatus.PUBLISHED, new BigDecimal("1000.00"), null, 1
        );
        when(catalogService.getProductPurchaseFacts(noteId)).thenReturn(Optional.of(noteFacts));
        when(catalogService.reserveStock(noteId, 2)).thenReturn(false);

        Cart cart = new Cart(verifiedStudentA.getUserId(), CartStatus.ACTIVE);
        cart.addItem(new CartItem(cart, noteId, 2, new BigDecimal("1000.00")));
        cartRepository.save(cart);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\": " + addressA.getAddressId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_STOCK")));

        // No order created, cart still ACTIVE
        assertEquals(0, orderRepository.count());
        Cart cartAfter = cartRepository.findById(cart.getCartId()).orElseThrow();
        assertEquals(CartStatus.ACTIVE, cartAfter.getStatus());
    }

    @Test
    @DisplayName("Expiry job expires pending orders and restores stock exactly once")
    void testExpiryJobReleasesStock() {
        Long noteId = 909L;

        Order expiredOrder = new Order();
        expiredOrder.setUserId(verifiedStudentA.getUserId());
        expiredOrder.setPaymentStatus(PaymentStatus.PENDING);
        expiredOrder.setFulfillmentStatus(FulfillmentStatus.PENDING);
        expiredOrder.setSubtotal(new BigDecimal("1000.00"));
        expiredOrder.setDeliveryFee(new BigDecimal("150.00"));
        expiredOrder.setTotalAmount(new BigDecimal("1150.00"));
        // Expired 5 minutes ago
        expiredOrder.setExpiresAt(OffsetDateTime.now().minusMinutes(5));

        OrderItem item = new OrderItem(noteId, "History Notes", "PHYSICAL_NOTE", 3, new BigDecimal("1000.00"), new BigDecimal("3000.00"));
        expiredOrder.addItem(item);
        expiredOrder = orderRepository.save(expiredOrder);

        // Run expiry job at current time
        int expiredCount = orderExpiryService.expireOrdersAt(OffsetDateTime.now());
        assertEquals(1, expiredCount);

        verify(catalogService, times(1)).releaseStock(noteId, 3);

        Order reloaded = orderRepository.findById(expiredOrder.getOrderId()).orElseThrow();
        assertEquals(PaymentStatus.EXPIRED, reloaded.getPaymentStatus());

        // Run job again: should not expire or release stock again
        int secondRunCount = orderExpiryService.expireOrdersAt(OffsetDateTime.now());
        assertEquals(0, secondRunCount);
        verify(catalogService, times(1)).releaseStock(noteId, 3);
    }

    @Test
    @DisplayName("Security & Role access: unverified student -> 403 EMAIL_NOT_VERIFIED, guest -> 401, admin -> 403")
    void testSecurityAccess() throws Exception {
        // Unverified student
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenUnverified)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("EMAIL_NOT_VERIFIED")));

        // Guest (no token)
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        // Admin
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Order details ownership: student cannot view another student's order (404)")
    void testOrderDetailsOwnership() throws Exception {
        Order order = new Order();
        order.setUserId(verifiedStudentA.getUserId());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setFulfillmentStatus(FulfillmentStatus.NOT_REQUIRED);
        order.setSubtotal(new BigDecimal("500.00"));
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("500.00"));
        order.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
        order = orderRepository.save(order);

        // Student A can view
        mockMvc.perform(get("/api/orders/" + order.getOrderId())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id", is(order.getOrderId().intValue())));

        // Student B gets 404 (prevent ID probing)
        mockMvc.perform(get("/api/orders/" + order.getOrderId())
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Editing an address afterward does not change order's shipping snapshot")
    void testAddressEditDoesNotAffectOrderSnapshot() {
        Order order = new Order();
        order.setUserId(verifiedStudentA.getUserId());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setFulfillmentStatus(FulfillmentStatus.PENDING);
        order.setSubtotal(new BigDecimal("1000.00"));
        order.setDeliveryFee(new BigDecimal("150.00"));
        order.setTotalAmount(new BigDecimal("1150.00"));
        order.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
        order.setShipRecipientName("Original Name");
        order.setShipAddressLine1("123 Original St");
        order = orderRepository.save(order);

        // Update address entity in database
        addressA.setRecipientName("Changed Name");
        addressA.setAddressLine1("999 New Road");
        addressRepository.save(addressA);

        Order reloadedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals("Original Name", reloadedOrder.getShipRecipientName());
        assertEquals("123 Original St", reloadedOrder.getShipAddressLine1());
    }

    @Test
    @DisplayName("Two simultaneous checkouts of the same user's cart create exactly one order")
    void testSimultaneousSameUserCartCheckout() throws Exception {
        Long videoId = 999L;
        ProductPurchaseFacts videoFacts = new ProductPurchaseFacts(
                videoId, "Concurrent Video", "conc-video", ProductType.VIDEO,
                ProductStatus.PUBLISHED, new BigDecimal("1000.00"), null, null
        );
        when(catalogService.getProductPurchaseFacts(videoId)).thenReturn(Optional.of(videoFacts));

        Cart cart = new Cart(verifiedStudentA.getUserId(), CartStatus.ACTIVE);
        cart.addItem(new CartItem(cart, videoId, 1, new BigDecimal("1000.00")));
        cartRepository.save(cart);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    mockMvc.perform(post("/api/orders")
                                    .header("Authorization", tokenA)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                            .andDo(result -> {
                                int status = result.getResponse().getStatus();
                                if (status == 201) {
                                    successCount.incrementAndGet();
                                } else if (status == 409) {
                                    conflictCount.incrementAndGet();
                                }
                            });
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one checkout must succeed");
        assertEquals(1, conflictCount.get(), "The concurrent checkout must fail with 409 conflict");
        assertEquals(1, orderRepository.count(), "Exactly one order must exist in DB");
    }
}
