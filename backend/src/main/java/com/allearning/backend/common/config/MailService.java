package com.allearning.backend.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String mailHost;
    private final String frontendUrl;

    public MailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl
    ) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Returns true only when SMTP host is explicitly configured.
     * Spring Boot always auto-configures a JavaMailSender bean even with an empty
     * spring.mail.host, so we cannot rely on mailSender == null to detect the
     * "no SMTP" case — we must check the host string directly.
     */
    private boolean isSmtpConfigured() {
        return mailHost != null && !mailHost.isBlank() && mailSender != null;
    }

    // ---------------------------------------------------------------------------
    // Link builders
    // ---------------------------------------------------------------------------

    /**
     * Builds the full frontend email verification link from the raw token.
     */
    public String buildVerificationLink(String token) {
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return base + "/verify-email?token=" + token;
    }

    /**
     * Builds the full frontend password reset link from the raw token.
     */
    public String buildPasswordResetLink(String token) {
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return base + "/reset-password?token=" + token;
    }

    // ---------------------------------------------------------------------------
    // Email sending (with console fallback)
    // ---------------------------------------------------------------------------

    /**
     * Sends a verification email to the user, or logs it to the console if SMTP is not configured.
     * Both registration and resend flows call this single method — never throw on missing SMTP config.
     */
    public void sendVerificationEmail(String toEmail, String token) {
        String verificationLink = buildVerificationLink(token);

        if (!isSmtpConfigured()) {
            log.info("===============================================================");
            log.info("[DEV MODE — no SMTP configured] Verification link for: {}", toEmail);
            log.info("Verification Link: {}", verificationLink);
            log.info("===============================================================");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Verify your AL Learning Platform account");
            message.setText("Welcome to AL Learning Platform!\n\n"
                    + "Please verify your email address by clicking the link below:\n"
                    + verificationLink + "\n\n"
                    + "This link will expire in 24 hours.\n"
                    + "If you did not register for an account, please ignore this email.");

            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send verification email via SMTP to {}. Cause: {}", toEmail, e.getMessage(), e);
            log.info("===============================================================");
            log.info("[SMTP SEND FAILED — fallback] Verification link for: {}", toEmail);
            log.info("Verification Link: {}", verificationLink);
            log.info("===============================================================");
        }
    }

    /**
     * Sends a password reset email to the user, or logs it to the console if SMTP is not configured.
     * Uses the exact same fallback strategy as {@link #sendVerificationEmail}.
     */
    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = buildPasswordResetLink(token);

        if (!isSmtpConfigured()) {
            log.info("===============================================================");
            log.info("[DEV MODE — no SMTP configured] Password reset link for: {}", toEmail);
            log.info("Reset Link: {}", resetLink);
            log.info("===============================================================");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Reset your AL Learning Platform password");
            message.setText("You requested a password reset for your AL Learning Platform account.\n\n"
                    + "Click the link below to set a new password:\n"
                    + resetLink + "\n\n"
                    + "This link will expire in 1 hour.\n"
                    + "If you did not request a password reset, please ignore this email.");

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send password reset email via SMTP to {}. Cause: {}", toEmail, e.getMessage(), e);
            log.info("===============================================================");
            log.info("[SMTP SEND FAILED — fallback] Password reset link for: {}", toEmail);
            log.info("Reset Link: {}", resetLink);
            log.info("===============================================================");
        }
    }
}
