package com.allearning.backend.commerce;

import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.catalog.dto.ProductPurchaseFacts;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.entity.Cart;
import com.allearning.backend.commerce.entity.CartStatus;
import com.allearning.backend.commerce.repository.CartItemRepository;
import com.allearning.backend.commerce.repository.CartRepository;
import com.allearning.backend.commerce.service.CartService;
import com.allearning.backend.commerce.service.VideoOwnershipService;
import com.allearning.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cartintegrationtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
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
class CartIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CatalogService catalogService;

    @MockitoBean
    private VideoOwnershipService videoOwnershipService;

    private MockMvc mockMvc;
    private User studentA;
    private User studentB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        try {
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_carts_user_active ON carts (user_id) WHERE status = 'ACTIVE'");
        } catch (Exception ignored) {
        }

        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        studentA = new User("Student A", "studenta@carttest.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        studentA.setEmailVerified(true);
        studentA = userRepository.save(studentA);
        tokenA = "Bearer " + jwtService.generateToken(studentA);

        studentB = new User("Student B", "studentb@carttest.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        studentB.setEmailVerified(true);
        studentB = userRepository.save(studentB);
        tokenB = "Bearer " + jwtService.generateToken(studentB);
    }

    @Test
    @DisplayName("GET /api/cart on user with no cart returns empty cart without creating a database row")
    void emptyCartWithoutCreatingRow() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.item_count", is(0)))
                .andExpect(jsonPath("$.subtotal", is(0.0)));

        assertEquals(0, cartRepository.count());
    }

    @Test
    @DisplayName("DRAFT, ARCHIVED, or missing product returns 404 on add")
    void invalidProductReturns404() throws Exception {
        when(catalogService.getProductPurchaseFacts(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 99, \"quantity\": 1}"))
                .andExpect(status().isNotFound());

        // DRAFT product
        when(catalogService.getProductPurchaseFacts(10L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(10L, "Draft Note", "draft-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.DRAFT, new BigDecimal("1500.00"), null, 10)
        ));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 10, \"quantity\": 1}"))
                .andExpect(status().isNotFound());

        // ARCHIVED product
        when(catalogService.getProductPurchaseFacts(11L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(11L, "Archived Note", "archived-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.ARCHIVED, new BigDecimal("1500.00"), null, 10)
        ));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 11, \"quantity\": 1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Note: stock 0 -> 409 OUT_OF_STOCK, quantity > stock -> 409 INSUFFICIENT_STOCK with available amount")
    void noteStockValidation() throws Exception {
        // Stock 0
        when(catalogService.getProductPurchaseFacts(1L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(1L, "Out of stock note", "out-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.PUBLISHED, new BigDecimal("1000.00"), null, 0)
        ));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 1, \"quantity\": 1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("OUT_OF_STOCK")));

        // Stock 3, requested 5
        when(catalogService.getProductPurchaseFacts(2L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(2L, "Low stock note", "low-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.PUBLISHED, new BigDecimal("1000.00"), null, 3)
        ));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 2, \"quantity\": 5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("INSUFFICIENT_STOCK")))
                .andExpect(jsonPath("$.message", containsString("3")));
    }

    @Test
    @DisplayName("Adding same note twice merges into one line; stock rule checks new total")
    void addingNoteTwiceMergesQuantity() throws Exception {
        when(catalogService.getProductPurchaseFacts(20L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(20L, "Physics Note", "physics-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.PUBLISHED, new BigDecimal("1200.00"), "thumb.jpg", 5)
        ));

        // Add 2
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 20, \"quantity\": 2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.items[0].line_total", is(2400.00)))
                .andExpect(jsonPath("$.subtotal", is(2400.00)));

        // Add 2 more (total 4 <= 5)
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 20, \"quantity\": 2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity", is(4)))
                .andExpect(jsonPath("$.items[0].line_total", is(4800.00)));

        // Add 2 more (total 6 > 5) -> 409 INSUFFICIENT_STOCK
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 20, \"quantity\": 2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("INSUFFICIENT_STOCK")));
    }

    @Test
    @DisplayName("Videos: quantity always 1, already owned -> 409 VIDEO_ALREADY_OWNED, added twice -> 409 ALREADY_IN_CART")
    void videoRules() throws Exception {
        when(catalogService.getProductPurchaseFacts(30L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(30L, "Calculus Video", "calc-video", ProductType.VIDEO,
                        ProductStatus.PUBLISHED, new BigDecimal("5000.00"), "vid.jpg", null)
        ));

        // Video quantity != 1 returns 400
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 30, \"quantity\": 2}"))
                .andExpect(status().isBadRequest());

        // Video already owned -> 409 VIDEO_ALREADY_OWNED
        when(videoOwnershipService.userOwnsVideo(studentA.getUserId(), 30L)).thenReturn(true);
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 30, \"quantity\": 1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("VIDEO_ALREADY_OWNED")));

        when(videoOwnershipService.userOwnsVideo(studentA.getUserId(), 30L)).thenReturn(false);

        // First add succeeds
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 30, \"quantity\": 1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity", is(1)));

        // Adding second time returns 409 ALREADY_IN_CART
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 30, \"quantity\": 1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("ALREADY_IN_CART")));
    }

    @Test
    @DisplayName("PATCH /api/cart/items/{id} updates note quantity, refreshes unit price, rejects video quantity != 1")
    void patchQuantityRules() throws Exception {
        when(catalogService.getProductPurchaseFacts(40L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(40L, "Chemistry Note", "chem-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.PUBLISHED, new BigDecimal("1000.00"), null, 10)
        ));

        // Add note
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 40, \"quantity\": 1}"))
                .andExpect(status().isCreated());

        Long itemId = cartItemRepository.findAll().get(0).getCartItemId();

        // Update price in catalog to 1500.00
        when(catalogService.getProductPurchaseFacts(40L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(40L, "Chemistry Note", "chem-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.PUBLISHED, new BigDecimal("1500.00"), null, 10)
        ));

        // PATCH quantity to 3 -> unit_price refreshed to 1500.00, line_total = 4500.00
        mockMvc.perform(patch("/api/cart/items/" + itemId)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity", is(3)))
                .andExpect(jsonPath("$.items[0].unit_price", is(1500.00)))
                .andExpect(jsonPath("$.items[0].line_total", is(4500.00)));
    }

    @Test
    @DisplayName("User A cannot see, edit or delete User B's cart items (returns 404)")
    void userIsolationReturns404() throws Exception {
        when(catalogService.getProductPurchaseFacts(50L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(50L, "Item 50", "item-50", ProductType.PHYSICAL_NOTE,
                        ProductStatus.PUBLISHED, new BigDecimal("1000.00"), null, 10)
        ));

        // User B adds an item
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 50, \"quantity\": 1}"))
                .andExpect(status().isCreated());

        Long itemBId = cartItemRepository.findAll().get(0).getCartItemId();

        // User A tries to PATCH User B's item -> 404
        mockMvc.perform(patch("/api/cart/items/" + itemBId)
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 2}"))
                .andExpect(status().isNotFound());

        // User A tries to DELETE User B's item -> 404
        mockMvc.perform(delete("/api/cart/items/" + itemBId)
                        .header("Authorization", tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Warning flags: price_changed and unavailable reflect catalog changes")
    void warningFlags() throws Exception {
        when(catalogService.getProductPurchaseFacts(60L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(60L, "Dynamic Note", "dynamic-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.PUBLISHED, new BigDecimal("1000.00"), null, 10)
        ));

        // Add note at price 1000.00
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 60, \"quantity\": 1}"))
                .andExpect(status().isCreated());

        // Admin updates price to 1200.00
        when(catalogService.getProductPurchaseFacts(60L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(60L, "Dynamic Note", "dynamic-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.PUBLISHED, new BigDecimal("1200.00"), null, 10)
        ));

        // GET cart should have price_changed = true, unit_price = 1000.00, current_price = 1200.00
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].price_changed", is(true)))
                .andExpect(jsonPath("$.items[0].unit_price", is(1000.00)))
                .andExpect(jsonPath("$.items[0].current_price", is(1200.00)));

        // Admin archives product -> unavailable = true, line is NOT deleted
        when(catalogService.getProductPurchaseFacts(60L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(60L, "Dynamic Note", "dynamic-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.ARCHIVED, new BigDecimal("1200.00"), null, 10)
        ));

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unavailable", is(true)))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @DisplayName("Two concurrent first-add requests for the same new user result in one ACTIVE cart")
    void concurrentFirstAdd() throws Exception {
        when(catalogService.getProductPurchaseFacts(70L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(70L, "Concurrent Note", "concurrent-note", ProductType.PHYSICAL_NOTE,
                        ProductStatus.PUBLISHED, new BigDecimal("1000.00"), null, 50)
        ));
        when(catalogService.getProductPurchaseFacts(71L)).thenReturn(Optional.of(
                new ProductPurchaseFacts(71L, "Concurrent Video", "concurrent-video", ProductType.VIDEO,
                        ProductStatus.PUBLISHED, new BigDecimal("2000.00"), null, null)
        ));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                cartService.addItem(studentA.getUserId(), new com.allearning.backend.commerce.dto.CartItemAddRequest(70L, 1));
                successCount.incrementAndGet();
            } catch (Exception e) {
                // ignore
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                cartService.addItem(studentA.getUserId(), new com.allearning.backend.commerce.dto.CartItemAddRequest(71L, 1));
                successCount.incrementAndGet();
            } catch (Exception e) {
                // ignore
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Exactly 1 ACTIVE cart should exist for studentA
        long activeCarts = cartRepository.findAll().stream()
                .filter(c -> c.getUserId().equals(studentA.getUserId()) && c.getStatus() == CartStatus.ACTIVE)
                .count();
        assertEquals(1, activeCarts);
    }
}
