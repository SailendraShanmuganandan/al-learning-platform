package com.allearning.backend.security;

import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.common.exception.EmailNotVerifiedException;
import com.allearning.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmailVerificationGuardTest {

    @RestController
    static class DummyProtectedController {
        @GetMapping("/api/test/requires-verified")
        @RequiresEmailVerified
        public String protectedEndpoint() {
            return "access-granted";
        }
    }

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private EmailVerificationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new EmailVerificationGuard(userRepository);
        EmailVerificationInterceptor interceptor = new EmailVerificationInterceptor(guard);

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyProtectedController())
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenUserIsEmailVerified_thenAccessGranted() throws Exception {
        User verifiedUser = new User("Alice", "alice@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        verifiedUser.setEmailVerified(true);

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(verifiedUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice@example.com", null, Collections.emptyList())
        );

        mockMvc.perform(get("/api/test/requires-verified"))
                .andExpect(status().isOk())
                .andExpect(content().string("access-granted"));
    }

    @Test
    void whenUserIsNotEmailVerified_thenForbiddenWithEmailNotVerifiedError() throws Exception {
        User unverifiedUser = new User("Bob", "bob@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        unverifiedUser.setEmailVerified(false);

        when(userRepository.findByEmailIgnoreCase("bob@example.com")).thenReturn(Optional.of(unverifiedUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bob@example.com", null, Collections.emptyList())
        );

        mockMvc.perform(get("/api/test/requires-verified"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void programmaticCheckVerified_throwsWhenUnverified() {
        User unverified = new User("Charlie", "charlie@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        unverified.setEmailVerified(false);

        assertThrows(EmailNotVerifiedException.class, () -> guard.checkVerified(unverified));
    }

    @Test
    void programmaticCheckVerified_succeedsWhenVerified() {
        User verified = new User("Charlie", "charlie@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        verified.setEmailVerified(true);

        // Does not throw
        guard.checkVerified(verified);
    }
}
