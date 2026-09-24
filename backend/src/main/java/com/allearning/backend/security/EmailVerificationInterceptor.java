package com.allearning.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC HandlerInterceptor that intercepts requests destined for handler methods
 * or controllers annotated with {@link RequiresEmailVerified}, delegating check to
 * {@link EmailVerificationGuard}.
 */
@Component
public class EmailVerificationInterceptor implements HandlerInterceptor {

    private final EmailVerificationGuard guard;

    public EmailVerificationInterceptor(EmailVerificationGuard guard) {
        this.guard = guard;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean hasAnnotation = AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), RequiresEmailVerified.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RequiresEmailVerified.class);

        if (hasAnnotation) {
            guard.checkCurrentAuthenticationVerified();
        }

        return true;
    }
}
