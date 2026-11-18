package com.allearning.backend.integration.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for PayHere Sri Lanka payment integration.
 * All sensitive values are bound from environment variables.
 */
@Component
public class PayHereProperties {

    private final String merchantId;
    private final String merchantSecret;
    private final String mode;
    private final String backendPublicUrl;
    private final String frontendPublicUrl;

    public PayHereProperties(
            @Value("${payhere.merchant-id:}") String merchantId,
            @Value("${payhere.merchant-secret:}") String merchantSecret,
            @Value("${payhere.mode:sandbox}") String mode,
            @Value("${app.public.backend-url:}") String backendPublicUrl,
            @Value("${app.public.frontend-url:}") String frontendPublicUrl
    ) {
        this.merchantId = merchantId != null ? merchantId.trim() : "";
        this.merchantSecret = merchantSecret != null ? merchantSecret.trim() : "";
        this.mode = mode != null ? mode.trim() : "sandbox";
        this.backendPublicUrl = backendPublicUrl != null ? backendPublicUrl.trim() : "";
        this.frontendPublicUrl = frontendPublicUrl != null ? frontendPublicUrl.trim() : "";
    }

    public boolean isConfigured() {
        return !merchantId.isBlank()
            && !merchantSecret.isBlank()
            && !backendPublicUrl.isBlank()
            && !frontendPublicUrl.isBlank();
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getMerchantSecret() {
        return merchantSecret;
    }

    public String getMode() {
        return mode;
    }

    public String getCheckoutUrl() {
        if ("live".equalsIgnoreCase(mode)) {
            return "https://www.payhere.lk/pay/checkout";
        }
        return "https://sandbox.payhere.lk/pay/checkout";
    }

    public String getNotifyUrl() {
        return stripTrailingSlash(backendPublicUrl) + "/api/payments/webhook";
    }

    public String getReturnUrl(String orderId) {
        return stripTrailingSlash(frontendPublicUrl) + "/orders/" + orderId + "?payment=return";
    }

    public String getCancelUrl(String orderId) {
        return stripTrailingSlash(frontendPublicUrl) + "/orders/" + orderId + "?payment=cancelled";
    }

    private String stripTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
