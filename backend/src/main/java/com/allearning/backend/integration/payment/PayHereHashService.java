package com.allearning.backend.integration.payment;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Service responsible for PayHere hash and MD5 signature generation and verification.
 * <p>
 * Official PayHere Checkout API rules:
 * <ul>
 *   <li>Checkout hash = UPPERCASE( MD5( merchant_id + order_id + amount + currency + UPPERCASE( MD5( merchant_secret ) ) ) )</li>
 *   <li>Notification md5sig = UPPERCASE( MD5( merchant_id + order_id + payhere_amount + payhere_currency + status_code + UPPERCASE( MD5( merchant_secret ) ) ) )</li>
 * </ul>
 */
@Service
public class PayHereHashService {

    /**
     * Formats a BigDecimal amount as required by PayHere:
     * 2 decimals, '.' separator, no thousands separators (e.g. "2450.00").
     */
    public String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Computes the uppercase MD5 digest of a string using UTF-8 encoding.
     */
    public String md5Hex(String input) {
        if (input == null) {
            input = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available in JVM", e);
        }
    }

    /**
     * Generates the checkout hash for redirect HTML form submission.
     *
     * @param merchantId      PayHere merchant ID
     * @param orderId         Our order ID as string
     * @param formattedAmount Exactly formatted amount string (e.g. "2450.00")
     * @param currency        Currency code (e.g. "LKR")
     * @param merchantSecret  PayHere merchant secret
     * @return Uppercase MD5 hash
     */
    public String generateCheckoutHash(String merchantId,
                                       String orderId,
                                       String formattedAmount,
                                       String currency,
                                       String merchantSecret) {
        String secretHash = md5Hex(merchantSecret);
        String combined = merchantId + orderId + formattedAmount + currency + secretHash;
        return md5Hex(combined);
    }

    /**
     * Generates expected notification signature (md5sig) using the values exactly as received from PayHere.
     *
     * @param merchantId      merchant_id parameter
     * @param orderId         order_id parameter
     * @param payhereAmount   payhere_amount parameter
     * @param payhereCurrency payhere_currency parameter
     * @param statusCode      status_code parameter
     * @param merchantSecret  our merchant secret
     * @return Uppercase MD5 signature
     */
    public String generateNotificationSignature(String merchantId,
                                                String orderId,
                                                String payhereAmount,
                                                String payhereCurrency,
                                                String statusCode,
                                                String merchantSecret) {
        String secretHash = md5Hex(merchantSecret);
        String combined = merchantId + orderId + payhereAmount + payhereCurrency + statusCode + secretHash;
        return md5Hex(combined);
    }

    /**
     * Constant-time comparison between calculated signature and incoming md5sig.
     *
     * @param merchantId        received merchant_id
     * @param orderId           received order_id
     * @param payhereAmount     received payhere_amount
     * @param payhereCurrency   received payhere_currency
     * @param statusCode        received status_code
     * @param merchantSecret    our configured merchant secret
     * @param incomingSignature received md5sig
     * @return true if authentic and untampered, false otherwise
     */
    public boolean verifyNotificationSignature(String merchantId,
                                               String orderId,
                                               String payhereAmount,
                                               String payhereCurrency,
                                               String statusCode,
                                               String merchantSecret,
                                               String incomingSignature) {
        if (incomingSignature == null || incomingSignature.isBlank()) {
            return false;
        }
        if (merchantSecret == null || merchantSecret.isBlank()) {
            return false;
        }

        String expected = generateNotificationSignature(
            merchantId != null ? merchantId : "",
            orderId != null ? orderId : "",
            payhereAmount != null ? payhereAmount : "",
            payhereCurrency != null ? payhereCurrency : "",
            statusCode != null ? statusCode : "",
                merchantSecret
        );

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
            incomingSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
