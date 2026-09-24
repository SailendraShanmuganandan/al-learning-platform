package com.allearning.backend.security;

import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.common.exception.EmailNotVerifiedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Reusable guard component for enforcing email verification requirements (SRS FR-AUTH-14).
 * Can be called programmatically or via {@link RequiresEmailVerified} annotation.
 */
@Component
public class EmailVerificationGuard {

    private final UserRepository userRepository;

    public EmailVerificationGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Programmatic check on a User entity.
     *
     * @param user the user to check
     * @throws EmailNotVerifiedException if user's email is not verified
     */
    public void checkVerified(User user) {
        if (user == null || !user.isEmailVerified()) {
            throw new EmailNotVerifiedException("EMAIL_NOT_VERIFIED", "Email verification is required to perform this action.");
        }
    }

    /**
     * Checks whether the currently authenticated principal in SecurityContextHolder has a verified email.
     *
     * @throws EmailNotVerifiedException if user's email is not verified
     * @throws BadCredentialsException if no authenticated user is found in security context
     */
    public void checkCurrentAuthenticationVerified() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Authentication required");
        }

        Object principal = authentication.getPrincipal();
        String email;
        if (principal instanceof User u) {
            checkVerified(u);
            return;
        } else if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else if (principal instanceof String str) {
            email = str;
        } else {
            throw new BadCredentialsException("Unable to determine authenticated user");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        checkVerified(user);
    }
}
