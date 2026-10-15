package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.entity.Cart;
import com.allearning.backend.commerce.entity.CartStatus;
import com.allearning.backend.commerce.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper service to isolate cart row creation in an independent transaction.
 * This guarantees that unique constraint violations from concurrent first-adds
 * can be caught and recovered from without poisoning the caller's transaction.
 */
@Service
public class CartInternalService {

    private final CartRepository cartRepository;
    private final com.allearning.backend.commerce.repository.CartItemRepository cartItemRepository;

    public CartInternalService(CartRepository cartRepository,
                               com.allearning.backend.commerce.repository.CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Cart createActiveCart(Long userId) {
        Cart cart = new Cart(userId, CartStatus.ACTIVE);
        return cartRepository.saveAndFlush(cart);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshCartItemPrice(Long cartItemId, java.math.BigDecimal newPrice) {
        cartItemRepository.findById(cartItemId).ifPresent(item -> {
            item.setUnitPrice(newPrice);
            item.setUpdatedAt(java.time.OffsetDateTime.now());
            cartItemRepository.saveAndFlush(item);
        });
    }
}
