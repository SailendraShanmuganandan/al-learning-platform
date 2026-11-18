package com.allearning.backend.integration.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PayHere implementation of {@link PaymentGatewayProvider}.
 * Encapsulates all PayHere field names, URLs, MD5 hashing, and signature validation.
 */
@Service
public class PayHerePaymentGateway implements PaymentGatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(PayHerePaymentGateway.class);

    private final PayHereProperties properties;
    private final PayHereHashService hashService;

    public PayHerePaymentGateway(PayHereProperties properties, PayHereHashService hashService) {
        this.properties = properties;
        this.hashService = hashService;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public PaymentFormResponse createPaymentForm(PaymentOrderDetails orderDetails) {
        String formattedAmount = hashService.formatAmount(orderDetails.amount());
        String orderIdStr = orderDetails.orderId().toString();
        String currency = (orderDetails.currency() != null && !orderDetails.currency().isBlank())
                ? orderDetails.currency()
                : "LKR";

        String hash = hashService.generateCheckoutHash(
                properties.getMerchantId(),
                orderIdStr,
                formattedAmount,
                currency,
                properties.getMerchantSecret()
        );

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", properties.getMerchantId());
        fields.put("return_url", properties.getReturnUrl(orderIdStr));
        fields.put("cancel_url", properties.getCancelUrl(orderIdStr));
        fields.put("notify_url", properties.getNotifyUrl());
        fields.put("order_id", orderIdStr);
        fields.put("items", orderDetails.itemsDescription());
        fields.put("currency", currency);
        fields.put("amount", formattedAmount);
        fields.put("first_name", orderDetails.customerFirstName());
        fields.put("last_name", orderDetails.customerLastName());
        fields.put("email", orderDetails.customerEmail());
        fields.put("phone", orderDetails.customerPhone());
        fields.put("address", orderDetails.deliveryAddress());
        fields.put("city", orderDetails.deliveryCity());
        fields.put("country", orderDetails.deliveryCountry());
        fields.put("hash", hash);

        return new PaymentFormResponse(properties.getCheckoutUrl(), fields);
    }

    @Override
    public PaymentNotificationResult verifyAndParseNotification(Map<String, String> params) {
        String merchantId = getParam(params, "merchant_id");
        String orderId = getParam(params, "order_id");
        String paymentId = getParam(params, "payment_id");
        String payhereAmount = getParam(params, "payhere_amount");
        String payhereCurrency = getParam(params, "payhere_currency");
        String statusCodeStr = getParam(params, "status_code");
        String md5sig = getParam(params, "md5sig");
        String statusMessage = getParam(params, "status_message");
        String method = getParam(params, "method");

        boolean signatureValid = hashService.verifyNotificationSignature(
                merchantId,
                orderId,
                payhereAmount,
                payhereCurrency,
                statusCodeStr,
                properties.getMerchantSecret(),
                md5sig
        );

        boolean merchantIdMatches = properties.getMerchantId().equals(merchantId);

        BigDecimal parsedAmount = null;
        if (payhereAmount != null && !payhereAmount.isBlank()) {
            try {
                parsedAmount = new BigDecimal(payhereAmount);
            } catch (Exception e) {
                log.warn("Unable to parse payhere_amount: {}", payhereAmount);
            }
        }

        Integer statusCode = null;
        if (statusCodeStr != null && !statusCodeStr.isBlank()) {
            try {
                statusCode = Integer.parseInt(statusCodeStr);
            } catch (Exception e) {
                log.warn("Unable to parse status_code: {}", statusCodeStr);
            }
        }

        return new PaymentNotificationResult(
                signatureValid,
                merchantIdMatches,
                orderId,
                paymentId,
                parsedAmount,
                payhereCurrency,
                statusCode,
                statusMessage,
                method
        );
    }

    private String getParam(Map<String, String> params, String key) {
        if (params == null) {
            return null;
        }
        return params.get(key);
    }
}
