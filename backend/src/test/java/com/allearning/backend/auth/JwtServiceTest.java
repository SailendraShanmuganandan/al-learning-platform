package com.allearning.backend.auth;

import com.allearning.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully() {
        User user = new User(
                "Jane Doe",
                "jane@example.com",
                "hashedPass",
                null,
                UserRole.STUDENT,
                AccountStatus.ACTIVE
        );
        ReflectionTestUtils.setField(user, "userId", 42L);

        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("jane@example.com", jwtService.extractEmail(token));
        assertEquals(42L, ((Number) jwtService.extractAllClaims(token).get("userId")).longValue());
        assertEquals("STUDENT", jwtService.extractAllClaims(token).get("role"));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.string"));
    }
}
