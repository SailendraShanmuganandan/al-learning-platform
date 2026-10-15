package com.allearning.backend.commerce.repository;

import com.allearning.backend.commerce.entity.Order;
import com.allearning.backend.commerce.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items"})
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findByOrderIdAndUserId(Long orderId, Long userId);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items i " +
           "WHERE o.userId = :userId AND o.paymentStatus = 'PAID' AND i.productId = :productId")
    boolean existsPaidOrderByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT o.orderId FROM Order o JOIN o.items i " +
           "WHERE o.userId = :userId AND o.paymentStatus = 'PENDING' AND i.productId = :productId")
    List<Long> findPendingOrderIdsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = :status AND o.expiresAt < :now")
    List<Order> findExpiredPendingOrdersWithLock(@Param("status") PaymentStatus status, @Param("now") OffsetDateTime now);
}
