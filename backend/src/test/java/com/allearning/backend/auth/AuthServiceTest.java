package com.allearning.backend.auth;

import com.allearning.backend.common.config.MailService;
import com.allearning.backend.common.exception.AccountDisabledException;
import com.allearning.backend.common.exception.EmailAlreadyExistsException;
import com.allearning.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private MailService mailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, tokenRepository, resetTokenRepository, passwordEncoder, jwtService, mailService, 24);
    }

    @Test
    void register_Success_CreatesTokenAndSendsEmail() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "Password123!", null);

        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jane@example.com", response.email());
        assertFalse(response.isEmailVerified());

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        EmailVerificationToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getToken());
        assertEquals("jane@example.com", savedToken.getUser().getEmail());

        verify(mailService).sendVerificationEmail(eq("jane@example.com"), eq(savedToken.getToken()));
    }

    @Test
    void register_DuplicateEmail_ThrowsEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "Password123!", null);
        User existing = new User("Jane Doe", "jane@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(existing));

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    void verifyEmail_Success() {
        User user = new User("Jane Doe", "jane@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        assertFalse(user.isEmailVerified());

        EmailVerificationToken token = new EmailVerificationToken(user, "valid-token", OffsetDateTime.now().plusHours(1));
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        authService.verifyEmail("valid-token");

        assertTrue(user.isEmailVerified());
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    @Test
    void verifyEmail_TokenNotFoundOrAlreadyUsed_ThrowsIllegalArgumentException() {
        when(tokenRepository.findByToken("missing-or-used")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.verifyEmail("missing-or-used"));
        assertTrue(ex.getMessage().contains("invalid, expired, or has already been used"));
    }

    @Test
    void verifyEmail_ExpiredToken_ThrowsAndDeletesToken() {
        User user = new User("Jane Doe", "jane@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        EmailVerificationToken expiredToken = new EmailVerificationToken(user, "expired-token", OffsetDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.verifyEmail("expired-token"));
        assertTrue(ex.getMessage().contains("expired"));
        verify(tokenRepository).delete(expiredToken);
        assertFalse(user.isEmailVerified());
    }

    @Test
    void resendVerification_UnverifiedUser_InvalidatesOldAndSendsNew() {
        User user = new User("Jane Doe", "jane@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        user.setEmailVerified(false);
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));

        authService.resendVerification("jane@example.com");

        verify(tokenRepository).deleteByUser(user);
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(mailService).sendVerificationEmail(eq("jane@example.com"), anyString());
    }

    @Test
    void resendVerification_AlreadyVerifiedUser_GenericSuccessWithoutSending() {
        User user = new User("Jane Doe", "jane@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));

        authService.resendVerification("jane@example.com");

        verify(tokenRepository, never()).deleteByUser(any());
        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    void login_Success() {
        User user = new User("Jane Doe", "jane@example.com", "encodedHash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass123!", "encodedHash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("mock-jwt-token");

        LoginRequest request = new LoginRequest("jane@example.com", "Pass123!");
        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals("jane@example.com", response.user().email());
    }

    @Test
    void login_WrongPassword_ThrowsBadCredentials() {
        User user = new User("Jane Doe", "jane@example.com", "encodedHash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "encodedHash")).thenReturn(false);

        LoginRequest request = new LoginRequest("jane@example.com", "WrongPass");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_UserNotFound_ThrowsBadCredentials() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("unknown@example.com", "Pass123!");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_SuspendedAccount_ThrowsAccountDisabled() {
        User user = new User("Jane Doe", "jane@example.com", "encodedHash", null, UserRole.STUDENT, AccountStatus.SUSPENDED);
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass123!", "encodedHash")).thenReturn(true);

        LoginRequest request = new LoginRequest("jane@example.com", "Pass123!");
        assertThrows(AccountDisabledException.class, () -> authService.login(request));
    }

    // ---------------------------------------------------------------------------
    // Forgot / reset password
    // ---------------------------------------------------------------------------

    @Test
    void forgotPassword_ExistingAccount_SendsResetEmail() {
        User user = new User("Jane Doe", "jane@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("jane@example.com");

        verify(resetTokenRepository).deleteByUser(user);
        verify(resetTokenRepository).save(any(PasswordResetToken.class));
        verify(mailService).sendPasswordResetEmail(eq("jane@example.com"), anyString());
    }

    @Test
    void forgotPassword_UnknownEmail_GenericSuccessNoSend() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        // Must not throw — generic return prevents user-enumeration
        assertDoesNotThrow(() -> authService.forgotPassword("ghost@example.com"));
        verify(resetTokenRepository, never()).save(any());
        verify(mailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void resetPassword_ValidToken_ChangesPassword() {
        User user = new User("Jane Doe", "jane@example.com", "oldHash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        PasswordResetToken prt = new PasswordResetToken(user, "valid-token", OffsetDateTime.now().plusHours(1));
        when(resetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(prt));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("newHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword("valid-token", "NewPass123!");

        assertEquals("newHash", user.getPasswordHash());
        assertTrue(prt.isUsed());
        verify(userRepository).save(user);
        verify(resetTokenRepository).save(prt);
        // Invalidate remaining tokens
        verify(resetTokenRepository).deleteByUser(user);
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsIllegalArgument() {
        User user = new User("Jane Doe", "jane@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        PasswordResetToken prt = new PasswordResetToken(user, "expired-token", OffsetDateTime.now().minusHours(2));
        when(resetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(prt));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword("expired-token", "NewPass123!"));
        assertTrue(ex.getMessage().toLowerCase().contains("expired"));
    }

    @Test
    void resetPassword_AlreadyUsedToken_ThrowsIllegalArgument() {
        User user = new User("Jane Doe", "jane@example.com", "hash", null, UserRole.STUDENT, AccountStatus.ACTIVE);
        PasswordResetToken prt = new PasswordResetToken(user, "used-token", OffsetDateTime.now().plusHours(1));
        prt.markUsed();
        when(resetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(prt));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword("used-token", "NewPass123!"));
        assertTrue(ex.getMessage().toLowerCase().contains("already been used"));
    }

    @Test
    void resetPassword_TokenNotFound_ThrowsIllegalArgument() {
        when(resetTokenRepository.findByToken("ghost-token")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.resetPassword("ghost-token", "NewPass123!"));
    }
}
