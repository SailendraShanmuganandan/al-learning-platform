package com.allearning.backend.commerce.repository;

import com.allearning.backend.commerce.entity.PaymentNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentNotificationRepository extends JpaRepository<PaymentNotification, Long> {

    List<PaymentNotification> findByOrderIdOrderByReceivedAtDesc(Long orderId);
}
