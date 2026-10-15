package com.allearning.backend.commerce.service;

import com.allearning.backend.catalog.dto.ProductPurchaseFacts;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.dto.CheckoutRequest;
import com.allearning.backend.commerce.dto.OrderItemResponse;
import com.allearning.backend.commerce.dto.OrderResponse;
import com.allearning.backend.commerce.entity.*;
import com.allearning.backend.commerce.exception.OrderException;
import com.allearning.backend.commerce.repository.CartRepository;
import com.allearning.backend.commerce.repository.OrderRepository;
import com.allearning.backend.security.EmailVerificationGuard;
import com.allearning.backend.user.dto.AddressResponse;
import com.allearning.backend.user.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartInternalService cartInternalService;
    private final OrderRepository orderRepository;
    private final CatalogService catalogService;
    private final UserProfileService userProfileService;
    private final VideoOwnershipService videoOwnershipService;
    private final EmailVerificationGuard emailVerificationGuard;

    @Value("${app.order.payment-timeout-minutes:30}")
    private long paymentTimeoutMinutes;

    @Value("${app.delivery.flat-fee:0.00}")
    private BigDecimal deliveryFlatFee;

    public OrderServiceImpl(CartRepository cartRepository,
                            CartInternalService cartInternalService,
                            OrderRepository orderRepository,
                            CatalogService catalogService,
                            UserProfileService userProfileService,
                            VideoOwnershipService videoOwnershipService,
                            EmailVerificationGuard emailVerificationGuard) {
        this.cartRepository = cartRepository;
        this.cartInternalService = cartInternalService;
        this.orderRepository = orderRepository;
        this.catalogService = catalogService;
        this.userProfileService = userProfileService;
        this.videoOwnershipService = videoOwnershipService;
        this.emailVerificationGuard = emailVerificationGuard;
    }

    @Override
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        // Enforce email verification (FR-AUTH-14)
        emailVerificationGuard.checkCurrentAuthenticationVerified();

        // 1. Lock user's ACTIVE cart row with a pessimistic write lock (SELECT ... FOR UPDATE)
        Cart cart = cartRepository.findByUserIdAndStatusWithLock(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new OrderException(HttpStatus.CONFLICT, "EMPTY_CART", "Your cart is empty."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new OrderException(HttpStatus.CONFLICT, "EMPTY_CART", "Your cart is empty.");
        }

        // 2. Fetch facts and validate every line against current catalog facts
        Map<Long, ProductPurchaseFacts> factsMap = new HashMap<>();
        boolean hasPhysicalNotes = false;

        for (CartItem item : cart.getItems()) {
            ProductPurchaseFacts facts = catalogService.getProductPurchaseFacts(item.getProductId())
                    .orElseThrow(() -> new OrderException(
                            HttpStatus.CONFLICT,
                            "PRODUCT_UNAVAILABLE",
                            "Product is no longer available: #" + item.getProductId(),
                            Map.of("productName", "Product #" + item.getProductId(), "productId", item.getProductId())
                    ));

            // Product must be PUBLISHED
            if (facts.status() != ProductStatus.PUBLISHED) {
                throw new OrderException(
                        HttpStatus.CONFLICT,
                        "PRODUCT_UNAVAILABLE",
                        facts.name() + " is no longer available.",
                        Map.of("productName", facts.name(), "productId", facts.id())
                );
            }

            // Current price validation
            if (facts.currentPrice().compareTo(item.getUnitPrice()) != 0) {
                BigDecimal oldPrice = item.getUnitPrice();
                BigDecimal newPrice = facts.currentPrice();
                // Refresh unit_price in an isolated transaction so the updated price is saved immediately
                cartInternalService.refreshCartItemPrice(item.getCartItemId(), newPrice);
                throw new OrderException(
                        HttpStatus.CONFLICT,
                        "PRICE_CHANGED",
                        "Price for " + facts.name() + " has changed from " + oldPrice + " to " + newPrice + ". Please review your cart.",
                        Map.of(
                                "productName", facts.name(),
                                "productId", facts.id(),
                                "oldPrice", oldPrice,
                                "newPrice", newPrice
                        )
                );
            }

            // Video validations
            if (facts.type() == ProductType.VIDEO) {
                if (videoOwnershipService.userOwnsVideo(userId, facts.id())) {
                    throw new OrderException(
                            HttpStatus.CONFLICT,
                            "VIDEO_ALREADY_OWNED",
                            "You already own this video: " + facts.name(),
                            Map.of("productName", facts.name(), "productId", facts.id())
                    );
                }

                List<Long> pendingOrderIds = orderRepository.findPendingOrderIdsByUserIdAndProductId(userId, facts.id());
                if (!pendingOrderIds.isEmpty()) {
                    Long pendingId = pendingOrderIds.get(0);
                    throw new OrderException(
                            HttpStatus.CONFLICT,
                            "VIDEO_PENDING_ORDER",
                            "Video " + facts.name() + " is held by an existing pending order (#" + pendingId + ").",
                            Map.of("productName", facts.name(), "productId", facts.id(), "pendingOrderId", pendingId)
                    );
                }
            } else if (facts.type() == ProductType.PHYSICAL_NOTE) {
                hasPhysicalNotes = true;
            }

            factsMap.put(item.getProductId(), facts);
        }

        // 3. Address validation and shipping snapshot (before stock reservation to avoid lock waste)
        AddressResponse address = null;
        if (hasPhysicalNotes) {
            Long addressId = request != null ? request.addressId() : null;
            if (addressId == null) {
                throw new OrderException(HttpStatus.BAD_REQUEST, "ADDRESS_REQUIRED", "Shipping address is required for physical items.");
            }
            // getUserAddress throws 404 if not found or not owned by user
            address = userProfileService.getUserAddress(userId, addressId);
        }

        // 4. Physical notes: reserve stock in ascending product_id order (deadlock prevention)
        if (hasPhysicalNotes) {
            List<CartItem> noteLines = cart.getItems().stream()
                    .filter(i -> factsMap.get(i.getProductId()).type() == ProductType.PHYSICAL_NOTE)
                    .sorted(Comparator.comparing(CartItem::getProductId))
                    .toList();

            for (CartItem noteItem : noteLines) {
                ProductPurchaseFacts facts = factsMap.get(noteItem.getProductId());
                boolean success = catalogService.reserveStock(noteItem.getProductId(), noteItem.getQuantity());
                if (!success) {
                    throw new OrderException(
                            HttpStatus.CONFLICT,
                            "INSUFFICIENT_STOCK",
                            "Insufficient stock for " + facts.name() + ".",
                            Map.of("productName", facts.name(), "productId", facts.id())
                    );
                }
            }
        }

        // 5. Compute subtotal, delivery fee, total amount
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (CartItem item : cart.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal deliveryFee = hasPhysicalNotes
                ? deliveryFlatFee.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = subtotal.add(deliveryFee).setScale(2, RoundingMode.HALF_UP);

        // 6. Build and save Order
        Order order = new Order();
        order.setUserId(userId);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setFulfillmentStatus(hasPhysicalNotes ? FulfillmentStatus.PENDING : FulfillmentStatus.NOT_REQUIRED);
        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(totalAmount);
        order.setExpiresAt(OffsetDateTime.now().plusMinutes(paymentTimeoutMinutes));

        if (address != null) {
            order.setShipRecipientName(address.recipientName());
            order.setShipPhone(address.phone());
            order.setShipAddressLine1(address.addressLine1());
            order.setShipAddressLine2(address.addressLine2());
            order.setShipCity(address.city());
            order.setShipDistrict(address.district());
            order.setShipPostalCode(address.postalCode());
        }

        for (CartItem item : cart.getItems()) {
            ProductPurchaseFacts facts = factsMap.get(item.getProductId());
            BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = new OrderItem(
                    facts.id(),
                    facts.name(),
                    facts.type().name(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    lineTotal
            );
            order.addItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // 7. Mark active cart as CONVERTED so next cart-add creates a fresh ACTIVE cart
        cart.setStatus(CartStatus.CONVERTED);
        cart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(cart);

        log.info("Order created successfully: orderId={}, userId={}, total={}",
                savedOrder.getOrderId(), userId, savedOrder.getTotalAmount());

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getUserOrder(Long userId, Long orderId) {
        return orderRepository.findByOrderIdAndUserId(orderId, userId)
                .map(this::mapToOrderResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getOrderItemId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getProductType(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getPaymentStatus(),
                order.getFulfillmentStatus(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getExpiresAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getShipRecipientName(),
                order.getShipPhone(),
                order.getShipAddressLine1(),
                order.getShipAddressLine2(),
                order.getShipCity(),
                order.getShipDistrict(),
                order.getShipPostalCode(),
                itemResponses
        );
    }
}
