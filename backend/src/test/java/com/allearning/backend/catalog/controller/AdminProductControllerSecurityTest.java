package com.allearning.backend.catalog.controller;

import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
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
class AdminProductControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CatalogService catalogService;

    private MockMvc mockMvc;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        userRepository.deleteAll();

        User admin = new User("Admin", "admin@example.com", "hash", null, UserRole.ADMIN, AccountStatus.ACTIVE);
        admin.setEmailVerified(true);
        admin = userRepository.save(admin);
        adminToken = "Bearer " + jwtService.generateToken(admin);

        User student = new User("Student", "student@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        student.setEmailVerified(true);
        student = userRepository.save(student);
        studentToken = "Bearer " + jwtService.generateToken(student);
    }

    @Test
    void getProducts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProducts_withStudentToken_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProducts_withAdminToken_returns200() throws Exception {
        when(catalogService.listAdminProducts(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void postProducts_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postProducts_withStudentToken_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchProducts_withStudentToken_returns403() throws Exception {
        mockMvc.perform(patch("/api/admin/products/1")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchProductStatus_withStudentToken_returns403() throws Exception {
        mockMvc.perform(patch("/api/admin/products/1/status")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden());
    }
}
