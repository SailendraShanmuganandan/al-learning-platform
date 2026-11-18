package com.allearning.backend.commerce.repository;

import com.allearning.backend.commerce.entity.FulfillmentStatus;
import com.allearning.backend.commerce.entity.Order;
import com.allearning.backend.commerce.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findByOrderIdWithLock(@Param("orderId") Long orderId);

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT DISTINCT o FROM Order o WHERE o.userId = :userId AND o.paymentStatus = 'PAID' ORDER BY o.updatedAt DESC")
    List<Order> findPaidOrdersWithItemsByUserId(@Param("userId") Long userId);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    long countByPaymentStatusAndFulfillmentStatus(PaymentStatus paymentStatus, FulfillmentStatus fulfillmentStatus);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus = com.allearning.backend.commerce.entity.PaymentStatus.PENDING")
    long countAwaitingPayment();

    @Query(value = "SELECT COALESCE(SUM(o.total_amount), 0) " +
                   "FROM orders o " +
                   "JOIN payments p ON o.order_id = p.order_id " +
                   "WHERE o.payment_status = 'PAID' " +
                   "  AND p.payment_status = 'PAID' " +
                   "  AND p.paid_at >= :start " +
                   "  AND p.paid_at < :end", nativeQuery = true)
    java.math.BigDecimal calculateRevenueBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query(value = "SELECT COALESCE(SUM(o.total_amount), 0) " +
                   "FROM orders o " +
                   "JOIN payments p ON o.order_id = p.order_id " +
                   "WHERE o.payment_status = 'PAID' " +
                   "  AND p.payment_status = 'PAID'", nativeQuery = true)
    java.math.BigDecimal calculateRevenueAllTime();

    @Query(value = "SELECT CAST(p.paid_at AT TIME ZONE :tz AS date) AS bucket_date, " +
                   "       COALESCE(SUM(o.total_amount), 0) AS revenue, " +
                   "       COUNT(DISTINCT o.order_id) AS order_count " +
                   "FROM orders o " +
                   "JOIN payments p ON o.order_id = p.order_id " +
                   "WHERE o.payment_status = 'PAID' " +
                   "  AND p.payment_status = 'PAID' " +
                   "  AND p.paid_at >= :start " +
                   "  AND p.paid_at < :end " +
                   "GROUP BY CAST(p.paid_at AT TIME ZONE :tz AS date)", nativeQuery = true)
    List<Object[]> findDailyRevenueBetween(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("tz") String tz);

    @Query(value = "SELECT oi.product_id, " +
                   "       oi.product_name, " +
                   "       oi.product_type, " +
                   "       COALESCE(SUM(oi.quantity), 0) AS quantity_sold, " +
                   "       COALESCE(SUM(oi.subtotal), 0) AS revenue " +
                   "FROM order_items oi " +
                   "JOIN orders o ON oi.order_id = o.order_id " +
                   "JOIN payments p ON o.order_id = p.order_id " +
                   "WHERE o.payment_status = 'PAID' " +
                   "  AND p.payment_status = 'PAID' " +
                   "  AND p.paid_at >= :start " +
                   "  AND p.paid_at < :end " +
                   "GROUP BY oi.product_id, oi.product_name, oi.product_type " +
                   "ORDER BY quantity_sold DESC, revenue DESC, oi.product_id ASC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopProductsBetween(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("limit") int limit);
}
