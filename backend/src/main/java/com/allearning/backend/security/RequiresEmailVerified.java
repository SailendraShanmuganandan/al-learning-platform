package com.allearning.backend.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated endpoint method or controller class requires
 * the authenticated user to have a verified email address (SRS FR-AUTH-14).
 * If the user's {@code is_email_verified} flag is false, the request is blocked
 * with HTTP 403 Forbidden and error code {@code EMAIL_NOT_VERIFIED}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresEmailVerified {
}
