package com.allearning.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstraps a single ADMIN account on first startup when all three
 * environment variables are present and no ADMIN user exists yet.
 * <p>
 * Rules:
 * <ul>
 *   <li>Runs exactly once during startup (ApplicationRunner).</li>
 *   <li>Does nothing if an ADMIN already exists.</li>
 *   <li>Does nothing if any of the three variables is unset or blank.</li>
 *   <li>Never logs the plain-text password.</li>
 *   <li>Not reachable through any HTTP endpoint.</li>
 * </ul>
 */
@Component
@Transactional
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_BOOTSTRAP_EMAIL:}")
    private String bootstrapEmail;

    @Value("${ADMIN_BOOTSTRAP_PASSWORD:}")
    private String bootstrapPassword;

    @Value("${ADMIN_BOOTSTRAP_NAME:}")
    private String bootstrapName;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean hasVars = isSet(bootstrapEmail) && isSet(bootstrapPassword) && isSet(bootstrapName);

        if (!hasVars) {
            log.info("Admin bootstrap: ADMIN_BOOTSTRAP_* variables not set — skipping.");
            return;
        }

        boolean adminExists = userRepository.existsByRole(UserRole.ADMIN);

        if (adminExists) {
            log.info("Admin bootstrap: an ADMIN account already exists — skipping.");
            return;
        }

        String normalizedEmail = bootstrapEmail.trim().toLowerCase();
        User admin = new User(
                bootstrapName.trim(),
                normalizedEmail,
                passwordEncoder.encode(bootstrapPassword),
                null,
                UserRole.ADMIN,
                AccountStatus.ACTIVE
        );
        admin.setEmailVerified(true);
        userRepository.save(admin);

        log.info("Admin bootstrap: ADMIN account created for email '{}'.", normalizedEmail);
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
