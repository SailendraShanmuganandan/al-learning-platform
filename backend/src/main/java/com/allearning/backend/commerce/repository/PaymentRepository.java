package com.allearning.backend.commerce.repository;

import com.allearning.backend.commerce.entity.Payment;
import com.allearning.backend.commerce.entity.PaymentRecordStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findFirstByOrderIdAndPaymentStatusOrderByCreatedAtDesc(Long orderId, PaymentRecordStatus status);

    boolean existsByTransactionReference(String transactionReference);

    Optional<Payment> findByTransactionReference(String transactionReference);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Query("SELECT p.paymentStatus FROM Payment p WHERE p.orderId = :orderId ORDER BY p.createdAt DESC LIMIT 1")
    Optional<PaymentRecordStatus> findLatestStatusByOrderId(@Param("orderId") Long orderId);

    /** Count of payments currently flagged for admin review (used for sidebar badge). */
    long countByNeedsReviewTrue();

    /** Returns order IDs from the given set that have at least one payment needing review. */
    @Query("SELECT DISTINCT p.orderId FROM Payment p WHERE p.orderId IN :orderIds AND p.needsReview = true")
    List<Long> findOrderIdsWithNeedsReview(@Param("orderIds") java.util.Collection<Long> orderIds);

    /** Pessimistic lock on a single payment row (for resolve-review / refund mutations). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :id")
    Optional<Payment> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT p FROM Payment p WHERE p.orderId IN :orderIds AND p.paymentStatus = 'PAID' ORDER BY p.createdAt DESC")
    List<Payment> findPaidPaymentsByOrderIds(@Param("orderIds") java.util.Collection<Long> orderIds);
}
