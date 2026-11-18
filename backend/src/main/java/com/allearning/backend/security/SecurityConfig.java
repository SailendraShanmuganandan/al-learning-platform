package com.allearning.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.OffsetDateTime;

/**
 * Central Spring Security configuration.
 * <p>
 * Public endpoints (no authentication required):
 * <ul>
 *   <li>GET  /api/health</li>
 *   <li>POST /api/auth/register</li>
 *   <li>POST /api/auth/login</li>
 *   <li>GET  /api/subjects (active subjects only)</li>
 *   <li>GET  /api/subjects/{id}</li>
 * </ul>
 * Admin endpoints (/api/admin/**) require ROLE_ADMIN.
 * All other requests require a valid JWT bearer token.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/verify-email",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()
                        // Public academic endpoints — active subjects and their chapters only
                        .requestMatchers(HttpMethod.GET, "/api/subjects", "/api/subjects/**").permitAll()
                        // /api/chapters/{id} and /api/chapters/{chapterId}/products both covered here
                        .requestMatchers(HttpMethod.GET, "/api/chapters", "/api/chapters/**").permitAll()
                        // Public catalog browsing — PUBLISHED products only (enforced in service)
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                        // Public classes browsing — ACTIVE classes under ACTIVE subjects
                        .requestMatchers(HttpMethod.GET, "/api/classes", "/api/classes/**").permitAll()
                        // Public announcements browsing — PUBLISHED announcements only
                        .requestMatchers(HttpMethod.GET, "/api/announcements", "/api/announcements/**").permitAll()
                        // Public quiz availability (NO questions exposed)
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/*/my-attempts").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/*").permitAll()
                        // Public contact message submissions (works with or without login token)
                        .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()
                        // Student class registration — authenticated STUDENT only
                        .requestMatchers(HttpMethod.POST, "/api/classes/*/register").hasRole("STUDENT")
                        // Admin-only area — authenticated STUDENT gets 403
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Shopping cart — authenticated STUDENT only; ADMIN gets 403, unauthenticated gets 401
                        .requestMatchers("/api/cart", "/api/cart/**").hasRole("STUDENT")
                        // Orders & Checkout — authenticated STUDENT only; ADMIN gets 403, unauthenticated gets 401
                        .requestMatchers("/api/orders", "/api/orders/**").hasRole("STUDENT")
                        // Student videos & protected playback — authenticated STUDENT only; ADMIN gets 403, unauthenticated gets 401
                        .requestMatchers("/api/my", "/api/my/**").hasRole("STUDENT")
                        // Payments — PayHere webhook is public; create is STUDENT only; detail lookup is authenticated
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/create").hasRole("STUDENT")
                        .requestMatchers("/api/payments", "/api/payments/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(String.format(
                                    "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                                    OffsetDateTime.now(),
                                    "Full authentication is required to access this resource"
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(String.format(
                                    "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"%s\"}",
                                    OffsetDateTime.now(),
                                    "You do not have permission to access this resource"
                            ));
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder used across the application.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides an explicit {@link UserDetailsService} bean so that Spring Boot's
     * {@code UserDetailsServiceAutoConfiguration} does not generate a random
     * in-memory password at startup. This app authenticates exclusively via JWT —
     * the UserDetailsService is never invoked at runtime.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        // Empty manager — no in-memory users. Real auth goes through JwtAuthenticationFilter.
        return new InMemoryUserDetailsManager();
    }
}