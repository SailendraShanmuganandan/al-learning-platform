package com.allearning.backend.commerce;

import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cartsectestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
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
class CartSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        userRepository.deleteAll();

        User admin = new User("Admin User", "admin@carttest.com", "hash", null, UserRole.ADMIN, AccountStatus.ACTIVE);
        admin.setEmailVerified(true);
        userRepository.save(admin);
        adminToken = "Bearer " + jwtService.generateToken(admin);

        User student = new User("Student User", "student@carttest.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        student.setEmailVerified(false); // Unverified student is allowed for cart!
        userRepository.save(student);
        studentToken = "Bearer " + jwtService.generateToken(student);
    }

    @Test
    @DisplayName("Guest (no token) -> 401 on GET /api/cart")
    void guestGets401OnGetCart() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guest (no token) -> 401 on POST /api/cart/items")
    void guestGets401OnPostCartItems() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 1, \"quantity\": 1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guest (no token) -> 401 on PATCH /api/cart/items/1")
    void guestGets401OnPatchCartItem() throws Exception {
        mockMvc.perform(patch("/api/cart/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guest (no token) -> 401 on DELETE /api/cart/items/1")
    void guestGets401OnDeleteCartItem() throws Exception {
        mockMvc.perform(delete("/api/cart/items/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guest (no token) -> 401 on DELETE /api/cart/items")
    void guestGets401OnDeleteAllCartItems() throws Exception {
        mockMvc.perform(delete("/api/cart/items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN token -> 403 on GET /api/cart")
    void adminGets403OnGetCart() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN token -> 403 on POST /api/cart/items")
    void adminGets403OnPostCartItems() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 1, \"quantity\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STUDENT token (even unverified) -> 200 on GET /api/cart")
    void studentGets200OnGetCart() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());
    }
}
