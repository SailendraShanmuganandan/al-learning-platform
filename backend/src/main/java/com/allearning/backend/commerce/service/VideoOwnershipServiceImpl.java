package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.entity.Order;
import com.allearning.backend.commerce.entity.OrderItem;
import com.allearning.backend.commerce.entity.Payment;
import com.allearning.backend.commerce.repository.OrderRepository;
import com.allearning.backend.commerce.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Implementation of {@link VideoOwnershipService} that checks whether
 * the user has an order with {@code payment_status = 'PAID'} containing the video product.
 */
@Service
public class VideoOwnershipServiceImpl implements VideoOwnershipService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public VideoOwnershipServiceImpl(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userOwnsVideo(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        return orderRepository.existsPaidOrderByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnedVideoPurchase> getOwnedVideos(Long userId) {
        if (userId == null) {
            return List.of();
        }

        List<Order> paidOrders = orderRepository.findPaidOrdersWithItemsByUserId(userId);
        if (paidOrders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = paidOrders.stream().map(Order::getOrderId).toList();
        List<Payment> paidPayments = paymentRepository.findPaidPaymentsByOrderIds(orderIds);
        Map<Long, OffsetDateTime> orderPaidAtMap = new HashMap<>();
        for (Payment payment : paidPayments) {
            if (!orderPaidAtMap.containsKey(payment.getOrderId()) && payment.getPaidAt() != null) {
                orderPaidAtMap.put(payment.getOrderId(), payment.getPaidAt());
            }
        }

        Map<Long, OffsetDateTime> latestPurchases = new HashMap<>();

        for (Order order : paidOrders) {
            OffsetDateTime paidDate = orderPaidAtMap.get(order.getOrderId());
            if (paidDate == null) {
                paidDate = (order.getUpdatedAt() != null) ? order.getUpdatedAt() : order.getCreatedAt();
            }

            for (OrderItem item : order.getItems()) {
                if ("VIDEO".equalsIgnoreCase(item.getProductType())) {
                    Long productId = item.getProductId();
                    OffsetDateTime existing = latestPurchases.get(productId);
                    if (existing == null || paidDate.isAfter(existing)) {
                        latestPurchases.put(productId, paidDate);
                    }
                }
            }
        }

        return latestPurchases.entrySet().stream()
                .map(e -> new OwnedVideoPurchase(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(OwnedVideoPurchase::purchasedAt).reversed())
                .toList();
    }
}
