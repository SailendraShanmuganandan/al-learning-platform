package com.allearning.backend.commerce.repository;

import com.allearning.backend.commerce.entity.Cart;
import com.allearning.backend.commerce.entity.CartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND c.status = :status")
    Optional<Cart> findByUserIdAndStatusWithLock(@Param("userId") Long userId, @Param("status") CartStatus status);
}
