package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link VideoOwnershipService} that checks whether
 * the user has an order with {@code payment_status = 'PAID'} containing the video product.
 */
@Service
public class VideoOwnershipServiceImpl implements VideoOwnershipService {

    private final OrderRepository orderRepository;

    public VideoOwnershipServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userOwnsVideo(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        return orderRepository.existsPaidOrderByUserIdAndProductId(userId, productId);
    }
}
