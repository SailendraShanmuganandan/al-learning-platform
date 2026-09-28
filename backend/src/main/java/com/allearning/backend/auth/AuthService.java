package com.allearning.backend.auth;

import com.allearning.backend.common.config.MailService;
import com.allearning.backend.common.exception.AccountDisabledException;
import com.allearning.backend.common.exception.EmailAlreadyExistsException;
import com.allearning.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Application service for authentication-related operations.
 * <p>
 * Invariants enforced here:
 * <ul>
 *   <li>Email is normalised to lower-case before persistence.</li>
 *   <li>Duplicate email (case-insensitive) is rejected with 409 Conflict.</li>
 *   <li>Password is hashed with BCrypt — the plain-text value never reaches the database.</li>
 *   <li>Every registered account is always STUDENT — this method cannot create ADMIN accounts.</li>
 *   <li>Only ACTIVE accounts can log in; SUSPENDED or DELETED accounts are rejected.</li>
 * </ul>
 */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final int expiryHours;

    public AuthService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            MailService mailService,
            @Value("${app.email.verification.expiry-hours:24}") int expiryHours
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.expiryHours = expiryHours;
    }

    /**
     * Registers a new user account.
     *
     * @param request the validated registration payload
     * @return a safe response DTO (no password hash)
     * @throws EmailAlreadyExistsException if the email already exists (case-insensitive)
     */
    public UserResponse register(RegisterRequest request) {
        String normalisedEmail = request.email().toLowerCase();

        if (userRepository.findByEmailIgnoreCase(normalisedEmail).isPresent()) {
            throw new EmailAlreadyExistsException(normalisedEmail);
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        User user = new User(
                request.fullName(),
                normalisedEmail,
                hashedPassword,
                request.phone(),
                UserRole.STUDENT,      // registration can never produce an ADMIN
                AccountStatus.ACTIVE
        );

        User saved = userRepository.save(user);

        // Issue verification token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken evt = new EmailVerificationToken(
                saved,
                token,
                OffsetDateTime.now().plusHours(expiryHours)
        );
        tokenRepository.save(evt);

        // Send or log verification email
        mailService.sendVerificationEmail(saved.getEmail(), token);

        return UserResponse.from(saved);
    }

    /**
     * Authenticates a user with email and password, issuing a signed JWT token upon success.
     *
     * @param request the validated login payload
     * @return AuthResponse containing token and user profile
     * @throws BadCredentialsException if credentials do not match
     * @throws AccountDisabledException if user is not in ACTIVE status
     */
    public AuthResponse login(LoginRequest request) {
        String normalisedEmail = request.email().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalisedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AccountDisabledException("Account is " + user.getAccountStatus().name().toLowerCase() + ". Please contact support.");
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, user);
    }

    /**
     * Fetches the profile of an authenticated user by email.
     *
     * @param email the user's email
     * @return safe UserResponse DTO
     */
    @Transactional(readOnly = true)
    public UserResponse getMe(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return UserResponse.from(user);
    }

    /**
     * Verifies a user's email using a one-time token.
     * Idempotent-safe: fails gracefully with 400 if token is invalid, expired, or already used.
     *
     * @param rawToken the verification token
     * @throws IllegalArgumentException if token is missing, expired, or invalid
     */
    public void verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Verification token is required.");
        }

        EmailVerificationToken token = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("Verification link is invalid, expired, or has already been used."));

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new IllegalArgumentException("Verification link has expired. Please request a new verification email.");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.delete(token);
    }

    /**
     * Re-issues a verification email to an unverified user.
     * Always returns generically to prevent state leaking.
     *
     * @param email the user's email
     */
    public void resendVerification(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email.toLowerCase())
                .orElse(null);

        if (user == null) {
            return; // Generic return
        }

        if (user.isEmailVerified()) {
            return; // Generic return, already verified
        }

        // Invalidate any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Issue new token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken evt = new EmailVerificationToken(
                user,
                token,
                OffsetDateTime.now().plusHours(expiryHours)
        );
        tokenRepository.save(evt);

        mailService.sendVerificationEmail(user.getEmail(), token);
    }

    /**
     * Initiates the forgot-password flow for the given email address.
     * <p>
     * Security: always returns the same generic success message whether or not
     * the account exists, so callers cannot infer account existence (prevents
     * user-enumeration attacks per SEC requirements).
     *
     * @param email the email address to send the reset link to
     */
    public void forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            return; // generic return — no leak
        }

        User user = userRepository.findByEmailIgnoreCase(email.toLowerCase())
                .orElse(null);

        if (user == null) {
            return; // generic return — do not reveal the account doesn't exist
        }

        // Invalidate all previous reset tokens for this user so only the newest link works
        resetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = new PasswordResetToken(
                user,
                token,
                OffsetDateTime.now().plusHours(1)  // 1-hour expiry per FR-AUTH-07
        );
        resetTokenRepository.save(prt);

        mailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    /**
     * Completes the password-reset flow: validates the token and sets a new password.
     *
     * @param rawToken    the reset token from the URL
     * @param newPassword the new plain-text password (already validated at the controller level)
     * @throws IllegalArgumentException if the token is missing, not found, expired, or already used
     */
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Reset token is required.");
        }

        PasswordResetToken prt = resetTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("Reset link is invalid or has already been used."));

        if (prt.isUsed()) {
            throw new IllegalArgumentException("This reset link has already been used. Please request a new one.");
        }

        if (prt.isExpired()) {
            throw new IllegalArgumentException("Reset link has expired. Please request a new password reset.");
        }

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark this token as used
        prt.markUsed();
        resetTokenRepository.save(prt);

        // Invalidate any other outstanding reset tokens for this user (second-link invalidation)
        resetTokenRepository.deleteByUser(user);
    }

    /**
     * Looks up a User entity by email. Intended for use by other packages (e.g. user)
     * so they do not need to inject {@link UserRepository} directly.
     *
     * @param email the user's email (case-insensitive)
     * @return the User entity
     * @throws BadCredentialsException if not found
     */
    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }

    /**
     * Updates the mutable profile fields (full_name, phone) of the current user.
     * Email, role, and account_status are intentionally NOT settable here.
     *
     * @param email    the authenticated user's email (from JWT)
     * @param fullName new full name (must not be blank, max 150 chars — validated at controller level)
     * @param phone    new phone (E.164 or blank — validated at controller level)
     * @return updated UserResponse
     */
    public UserResponse updateProfile(String email, String fullName, String phone) {
        User user = findUserByEmail(email);
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        // phone is optional — null means "do not change", empty string means "clear it"
        if (phone != null) {
            user.setPhone(phone.isBlank() ? null : phone.trim());
        }
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }
}
