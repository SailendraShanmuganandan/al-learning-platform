package com.allearning.backend.common.config;

import com.allearning.backend.security.EmailVerificationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final EmailVerificationInterceptor emailVerificationInterceptor;

    public WebMvcConfig(EmailVerificationInterceptor emailVerificationInterceptor) {
        this.emailVerificationInterceptor = emailVerificationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(emailVerificationInterceptor);
    }
}
