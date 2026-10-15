package com.allearning.backend.commerce.service;

import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.entity.Order;
import com.allearning.backend.commerce.entity.OrderItem;
import com.allearning.backend.commerce.entity.PaymentStatus;
import com.allearning.backend.commerce.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrderExpiryServiceImpl implements OrderExpiryService {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryServiceImpl.class);

    private final OrderRepository orderRepository;
    private final CatalogService catalogService;

    public OrderExpiryServiceImpl(OrderRepository orderRepository, CatalogService catalogService) {
        this.orderRepository = orderRepository;
        this.catalogService = catalogService;
    }

    @Override
    @Transactional
    public int expireOrdersAt(OffsetDateTime now) {
        List<Order> candidates = orderRepository.findExpiredPendingOrdersWithLock(PaymentStatus.PENDING, now);
        int count = 0;

        for (Order order : candidates) {
            // Re-check status inside transaction to guard against concurrent status transitions
            if (order.getPaymentStatus() == PaymentStatus.PENDING) {
                order.setPaymentStatus(PaymentStatus.EXPIRED);

                // Release stock for each physical note item
                for (OrderItem item : order.getItems()) {
                    if ("PHYSICAL_NOTE".equalsIgnoreCase(item.getProductType())) {
                        catalogService.releaseStock(item.getProductId(), item.getQuantity());
                    }
                }

                orderRepository.save(order);
                // Log each expiry with order id only (no personal data)
                log.info("Order expired: orderId={}", order.getOrderId());
                count++;
            }
        }

        return count;
    }
}
