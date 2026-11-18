package com.allearning.backend.commerce.service;

import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.dto.PaymentCreateResponse;
import com.allearning.backend.commerce.dto.PaymentDetailResponse;
import com.allearning.backend.commerce.entity.*;
import com.allearning.backend.commerce.exception.PaymentException;
import com.allearning.backend.commerce.repository.OrderRepository;
import com.allearning.backend.commerce.repository.PaymentNotificationRepository;
import com.allearning.backend.commerce.repository.PaymentRepository;
import com.allearning.backend.integration.payment.PaymentFormResponse;
import com.allearning.backend.integration.payment.PaymentGatewayProvider;
import com.allearning.backend.integration.payment.PaymentNotificationResult;
import com.allearning.backend.integration.payment.PaymentOrderDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentGatewayProvider paymentGatewayProvider;
    private final PaymentRepository paymentRepository;
    private final PaymentNotificationRepository paymentNotificationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CatalogService catalogService;
    private final VideoOwnershipService videoOwnershipService;

    public PaymentServiceImpl(
            PaymentGatewayProvider paymentGatewayProvider,
            PaymentRepository paymentRepository,
            PaymentNotificationRepository paymentNotificationRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            CatalogService catalogService,
            VideoOwnershipService videoOwnershipService
    ) {
        this.paymentGatewayProvider = paymentGatewayProvider;
        this.paymentRepository = paymentRepository;
        this.paymentNotificationRepository = paymentNotificationRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.catalogService = catalogService;
        this.videoOwnershipService = videoOwnershipService;
    }

    @Override
    @Transactional
    public PaymentCreateResponse createPayment(Long currentUserId, Long orderId) {
        if (!paymentGatewayProvider.isConfigured()) {
            throw new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENTS_NOT_CONFIGURED",
                    "Payment provider credentials are not configured.");
        }

        if (orderId == null) {
            throw new PaymentException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order ID is required.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found."));

        if (!order.getUserId().equals(currentUserId)) {
            throw new PaymentException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found.");
        }

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new PaymentException(HttpStatus.CONFLICT, "ORDER_NOT_PAYABLE",
                    "Order payment status is " + order.getPaymentStatus() + ", not payable.");
        }

        if (order.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new PaymentException(HttpStatus.CONFLICT, "ORDER_EXPIRED", "Order has expired.");
        }

        // Reuse existing PENDING payment row or create a new one
        Payment payment = paymentRepository.findFirstByOrderIdAndPaymentStatusOrderByCreatedAtDesc(
                order.getOrderId(), PaymentRecordStatus.PENDING
        ).orElseGet(() -> {
            Payment newPayment = new Payment(order.getOrderId(), "PAYHERE", order.getTotalAmount());
            return paymentRepository.save(newPayment);
        });

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));

        // Name split
        String fullName = user.getFullName() != null ? user.getFullName().trim() : "";
        if (fullName.isEmpty()) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "CUSTOMER_NAME_REQUIRED",
                    "A customer name is required to start a PayHere payment.");
        }
        String[] nameParts = fullName.split("\\s+", 2);
        if (nameParts.length < 2 || nameParts[1].isBlank()) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "CUSTOMER_NAME_REQUIRED",
                "A first and last name are required to start a PayHere payment.");
        }
        String firstName = nameParts[0];
        String lastName = nameParts[1].trim();

        // PayHere requires a valid phone number; do not invent one for checkout.
        String phone = order.getShipPhone();
        if (phone == null || phone.isBlank()) {
            phone = user.getPhone();
        }
        if (phone == null || phone.isBlank()) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "CUSTOMER_PHONE_REQUIRED",
                    "Add a phone number to your profile to start a PayHere payment.");
        }

        // Address & city snapshot fallback
        String address = order.getShipAddressLine1();
        if (address != null && !address.isBlank()) {
            if (order.getShipAddressLine2() != null && !order.getShipAddressLine2().isBlank()) {
                address = address + ", " + order.getShipAddressLine2().trim();
            }
        } else {
            address = "N/A";
        }

        String city = order.getShipCity();
        if (city == null || city.isBlank()) {
            city = "N/A";
        }

        String country = "Sri Lanka";
        String itemsDesc = "Order #" + order.getOrderId();

        PaymentOrderDetails details = new PaymentOrderDetails(
                order.getOrderId(),
                order.getTotalAmount(),
                "LKR",
                firstName,
                lastName,
                user.getEmail(),
                phone,
                address,
                city,
                country,
                itemsDesc
        );

        PaymentFormResponse form = paymentGatewayProvider.createPaymentForm(details);
        return new PaymentCreateResponse(form.checkoutUrl(), form.fields());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPayment(Long paymentId, Long currentUserId, boolean isAdmin) {
        if (paymentId == null) {
            throw new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found.");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found."));

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Order not found."));

        if (!isAdmin && !order.getUserId().equals(currentUserId)) {
            // Anyone else gets 404 per FR-PAY-09 and SEC-06
            throw new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found.");
        }

        Boolean needsReview = isAdmin ? payment.isNeedsReview() : null;
        String reviewReason = isAdmin ? payment.getReviewReason() : null;

        return new PaymentDetailResponse(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus().name(),
                payment.getAmount(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                needsReview,
                reviewReason
        );
    }

    @Override
    @Transactional
    public int processWebhookNotification(Map<String, String> params) {
        // Step 1: Verify md5sig
        PaymentNotificationResult result = paymentGatewayProvider.verifyAndParseNotification(params);

        Long parsedOrderId = null;
        if (result.orderId() != null) {
            try {
                parsedOrderId = Long.parseLong(result.orderId());
            } catch (NumberFormatException ignored) {
            }
        }

        if (!result.signatureValid()) {
            log.warn("Invalid signature on payment notification for orderId={}", result.orderId());
            paymentNotificationRepository.save(new PaymentNotification(
                    parsedOrderId,
                    result.transactionReference(),
                    result.statusCode(),
                    result.amount(),
                    result.currency(),
                    false,
                    "REJECTED_SIGNATURE",
                    OffsetDateTime.now()
            ));
            return 400;
        }

        // Step 2: Verify merchant ID
        if (!result.merchantIdMatches()) {
            log.warn("Merchant ID mismatch on payment notification for orderId={}", result.orderId());
            paymentNotificationRepository.save(new PaymentNotification(
                    parsedOrderId,
                    result.transactionReference(),
                    result.statusCode(),
                    result.amount(),
                    result.currency(),
                    true,
                    "INVALID_MERCHANT_ID",
                    OffsetDateTime.now()
            ));
            return 200;
        }

        // Step 3: Look up order by order_id
        if (parsedOrderId == null) {
            log.warn("Unknown order ID in payment notification: {}", result.orderId());
            paymentNotificationRepository.save(new PaymentNotification(
                    null,
                    result.transactionReference(),
                    result.statusCode(),
                    result.amount(),
                    result.currency(),
                    true,
                    "UNKNOWN_ORDER",
                    OffsetDateTime.now()
            ));
            return 200;
        }

        // Step 4 & 5: In one transaction that locks the order row with SELECT ... FOR UPDATE
        return processValidNotificationInTransaction(parsedOrderId, result);
    }

    @Transactional
    protected int processValidNotificationInTransaction(Long orderId, PaymentNotificationResult result) {
        Optional<Order> orderOpt = orderRepository.findByOrderIdWithLock(orderId);
        if (orderOpt.isEmpty()) {
            log.warn("Order not found with lock: orderId={}", orderId);
            paymentNotificationRepository.save(new PaymentNotification(
                    orderId,
                    result.transactionReference(),
                    result.statusCode(),
                    result.amount(),
                    result.currency(),
                    true,
                    "UNKNOWN_ORDER",
                    OffsetDateTime.now()
            ));
            return 200;
        }

        Order order = orderOpt.get();

        // Check currency and amount
        boolean currencyMatches = "LKR".equalsIgnoreCase(result.currency());
        boolean amountMatches = result.amount() != null && result.amount().compareTo(order.getTotalAmount()) == 0;

        if (!currencyMatches || !amountMatches) {
            log.warn("Amount or currency mismatch for orderId={}", orderId);
            Payment payment = getOrCreatePayment(order);
            payment.setNeedsReview(true);
            payment.setReviewReason("AMOUNT_MISMATCH");
            paymentRepository.save(payment);

            paymentNotificationRepository.save(new PaymentNotification(
                    orderId,
                    result.transactionReference(),
                    result.statusCode(),
                    result.amount(),
                    result.currency(),
                    true,
                    "AMOUNT_MISMATCH",
                    OffsetDateTime.now()
            ));
            return 200;
        }

        Integer statusCode = result.statusCode();
        if (statusCode == null) {
            paymentNotificationRepository.save(new PaymentNotification(
                    orderId,
                    result.transactionReference(),
                    null,
                    result.amount(),
                    result.currency(),
                    true,
                    "MISSING_STATUS_CODE",
                    OffsetDateTime.now()
            ));
            return 200;
        }

        // Handle status codes: 2 (success), 0 (pending), -1 (cancelled), -2 (failed), -3 (chargeback)
        switch (statusCode) {
            case 2 -> handleStatusSuccess(order, result);
            case 0 -> {
                paymentNotificationRepository.save(new PaymentNotification(
                        orderId,
                        result.transactionReference(),
                        statusCode,
                        result.amount(),
                        result.currency(),
                        true,
                        "PENDING_NOTIFICATION",
                        OffsetDateTime.now()
                ));
            }
            case -1, -2 -> handleStatusFailedOrCancelled(order, statusCode, result);
            case -3 -> handleStatusChargeback(order, statusCode, result);
            default -> {
                paymentNotificationRepository.save(new PaymentNotification(
                        orderId,
                        result.transactionReference(),
                        statusCode,
                        result.amount(),
                        result.currency(),
                        true,
                        "UNHANDLED_STATUS_CODE",
                        OffsetDateTime.now()
                ));
            }
        }

        return 200;
    }

    private void handleStatusSuccess(Order order, PaymentNotificationResult result) {
        Long orderId = order.getOrderId();
        String txRef = result.transactionReference();

        // 1. Order already PAID -> duplicate; change nothing (idempotent, BR-13, SEC-09)
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            paymentNotificationRepository.save(new PaymentNotification(
                    orderId, txRef, 2, result.amount(), result.currency(), true, "DUPLICATE_IGNORED", OffsetDateTime.now()
            ));
            return;
        }

        if (txRef != null && paymentRepository.existsByTransactionReference(txRef)) {
            paymentNotificationRepository.save(new PaymentNotification(
                    orderId, txRef, 2, result.amount(), result.currency(), true, "DUPLICATE_IGNORED", OffsetDateTime.now()
            ));
            return;
        }

        // 2. Order PENDING
        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            Payment payment = paymentRepository.findFirstByOrderIdAndPaymentStatusOrderByCreatedAtDesc(
                    orderId, PaymentRecordStatus.PENDING
            ).orElseGet(() -> new Payment(orderId, "PAYHERE", order.getTotalAmount()));

            payment.setPaymentStatus(PaymentRecordStatus.PAID);
            payment.setTransactionReference(txRef);
            payment.setPaidAt(OffsetDateTime.now());
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            paymentNotificationRepository.save(new PaymentNotification(
                    orderId, txRef, 2, result.amount(), result.currency(), true, "APPLIED", OffsetDateTime.now()
            ));
            return;
        }

        // 3. Order EXPIRED (late payment) -> attempt to revive
        if (order.getPaymentStatus() == PaymentStatus.EXPIRED) {
            List<OrderItem> successfullyReserved = new ArrayList<>();
            boolean fulfillable = true;

            // Re-reserve stock for physical note items in ascending product_id order
            List<OrderItem> physicalItems = order.getItems().stream()
                    .filter(i -> "PHYSICAL_NOTE".equalsIgnoreCase(i.getProductType()))
                    .sorted(Comparator.comparing(OrderItem::getProductId))
                    .toList();

            for (OrderItem item : physicalItems) {
                boolean reserved = catalogService.reserveStock(item.getProductId(), item.getQuantity());
                if (reserved) {
                    successfullyReserved.add(item);
                } else {
                    fulfillable = false;
                    break;
                }
            }

            // Confirm no video in the order is now owned or held by another PENDING order
            if (fulfillable) {
                for (OrderItem item : order.getItems()) {
                    if ("VIDEO".equalsIgnoreCase(item.getProductType())) {
                        if (videoOwnershipService.userOwnsVideo(order.getUserId(), item.getProductId())) {
                            fulfillable = false;
                            break;
                        }

                        List<Long> pendingIds = new ArrayList<>(
                                orderRepository.findPendingOrderIdsByUserIdAndProductId(order.getUserId(), item.getProductId())
                        );
                        pendingIds.remove(order.getOrderId());
                        if (!pendingIds.isEmpty()) {
                            fulfillable = false;
                            break;
                        }
                    }
                }
            }

            if (fulfillable) {
                // Succeeded: order and payment become PAID
                order.setPaymentStatus(PaymentStatus.PAID);
                orderRepository.save(order);

                Payment payment = getOrCreatePayment(order);
                payment.setPaymentStatus(PaymentRecordStatus.PAID);
                payment.setTransactionReference(txRef);
                payment.setPaidAt(OffsetDateTime.now());
                paymentRepository.save(payment);

                paymentNotificationRepository.save(new PaymentNotification(
                        orderId, txRef, 2, result.amount(), result.currency(), true, "LATE_PAYMENT_REVIVED", OffsetDateTime.now()
                ));
            } else {
                // Failed: roll back re-reservations, keep order EXPIRED, flag payment for review
                for (OrderItem reserved : successfullyReserved) {
                    catalogService.releaseStock(reserved.getProductId(), reserved.getQuantity());
                }

                Payment payment = getOrCreatePayment(order);
                payment.setPaymentStatus(PaymentRecordStatus.PAID);
                payment.setTransactionReference(txRef);
                payment.setPaidAt(OffsetDateTime.now());
                payment.setNeedsReview(true);
                payment.setReviewReason("LATE_PAYMENT_NOT_FULFILLABLE");
                paymentRepository.save(payment);

                paymentNotificationRepository.save(new PaymentNotification(
                        orderId, txRef, 2, result.amount(), result.currency(), true, "LATE_PAYMENT_NEEDS_REVIEW", OffsetDateTime.now()
                ));
                log.info("Late payment for expired orderId={} could not be fulfilled; flagged for review", orderId);
            }
            return;
        }

        // Order in other status
        paymentNotificationRepository.save(new PaymentNotification(
                orderId, txRef, 2, result.amount(), result.currency(), true, "IGNORED_ORDER_STATUS", OffsetDateTime.now()
        ));
    }

    private void handleStatusFailedOrCancelled(Order order, int statusCode, PaymentNotificationResult result) {
        Long orderId = order.getOrderId();
        String outcome = statusCode == -1 ? "PAYMENT_CANCELLED" : "PAYMENT_FAILED";

        // NEVER downgrade a PAID order or payment
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            paymentNotificationRepository.save(new PaymentNotification(
                    orderId, result.transactionReference(), statusCode, result.amount(), result.currency(),
                    true, "IGNORED_AFTER_PAID", OffsetDateTime.now()
            ));
            return;
        }

        // Set latest PENDING payment to FAILED; leave order PENDING so student can retry until it expires
        paymentRepository.findFirstByOrderIdAndPaymentStatusOrderByCreatedAtDesc(orderId, PaymentRecordStatus.PENDING)
                .ifPresent(p -> {
                    p.setPaymentStatus(PaymentRecordStatus.FAILED);
                    paymentRepository.save(p);
                });

        paymentNotificationRepository.save(new PaymentNotification(
                orderId, result.transactionReference(), statusCode, result.amount(), result.currency(),
                true, outcome, OffsetDateTime.now()
        ));
    }

    private void handleStatusChargeback(Order order, int statusCode, PaymentNotificationResult result) {
        Long orderId = order.getOrderId();

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            Payment payment = getOrCreatePayment(order);
            payment.setNeedsReview(true);
            payment.setReviewReason("CHARGEBACK");
            paymentRepository.save(payment);
        }

        paymentNotificationRepository.save(new PaymentNotification(
                orderId, result.transactionReference(), statusCode, result.amount(), result.currency(),
                true, "CHARGEBACK", OffsetDateTime.now()
        ));
    }

    private Payment getOrCreatePayment(Order order) {
        return paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getOrderId())
                .orElseGet(() -> {
                    Payment p = new Payment(order.getOrderId(), "PAYHERE", order.getTotalAmount());
                    return paymentRepository.save(p);
                });
    }
}
