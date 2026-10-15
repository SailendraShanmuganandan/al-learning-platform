package com.allearning.backend.commerce.repository;

import com.allearning.backend.commerce.entity.CartItem;
import com.allearning.backend.commerce.entity.CartStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartCartIdAndProductId(Long cartId, Long productId);

    @EntityGraph(attributePaths = {"cart", "cart.items"})
    Optional<CartItem> findByCartItemIdAndCartUserIdAndCartStatus(Long cartItemId, Long userId, CartStatus status);
}
