package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.entity.FulfillmentStatus;
import com.allearning.backend.commerce.entity.Order;
import com.allearning.backend.commerce.entity.OrderItem;
import com.allearning.backend.commerce.entity.PaymentStatus;
import com.allearning.backend.commerce.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:videoownershipdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Transactional
class VideoOwnershipServiceTest {

    @Autowired
    private VideoOwnershipService videoOwnershipService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Video ownership: returns true only for PAID orders containing the video")
    void testVideoOwnershipWithPaidOrder() {
        Long studentId = 1001L;
        Long videoProductId = 50L;

        // Initially no orders -> false
        assertFalse(videoOwnershipService.userOwnsVideo(studentId, videoProductId));

        // Insert a PENDING order with video -> still false
        Order pendingOrder = new Order();
        pendingOrder.setUserId(studentId);
        pendingOrder.setPaymentStatus(PaymentStatus.PENDING);
        pendingOrder.setFulfillmentStatus(FulfillmentStatus.NOT_REQUIRED);
        pendingOrder.setSubtotal(new BigDecimal("1500.00"));
        pendingOrder.setDeliveryFee(BigDecimal.ZERO);
        pendingOrder.setTotalAmount(new BigDecimal("1500.00"));
        pendingOrder.setExpiresAt(OffsetDateTime.now().plusMinutes(30));

        OrderItem pendingItem = new OrderItem(videoProductId, "Biology Unit 1 Video", "VIDEO", 1,
                new BigDecimal("1500.00"), new BigDecimal("1500.00"));
        pendingOrder.addItem(pendingItem);
        orderRepository.save(pendingOrder);

        assertFalse(videoOwnershipService.userOwnsVideo(studentId, videoProductId));

        // Insert a PAID order with video -> now true
        Order paidOrder = new Order();
        paidOrder.setUserId(studentId);
        paidOrder.setPaymentStatus(PaymentStatus.PAID);
        paidOrder.setFulfillmentStatus(FulfillmentStatus.NOT_REQUIRED);
        paidOrder.setSubtotal(new BigDecimal("1500.00"));
        paidOrder.setDeliveryFee(BigDecimal.ZERO);
        paidOrder.setTotalAmount(new BigDecimal("1500.00"));
        paidOrder.setExpiresAt(OffsetDateTime.now().plusMinutes(30));

        OrderItem paidItem = new OrderItem(videoProductId, "Biology Unit 1 Video", "VIDEO", 1,
                new BigDecimal("1500.00"), new BigDecimal("1500.00"));
        paidOrder.addItem(paidItem);
        orderRepository.save(paidOrder);

        assertTrue(videoOwnershipService.userOwnsVideo(studentId, videoProductId));

        // Another user does not own it
        assertFalse(videoOwnershipService.userOwnsVideo(9999L, videoProductId));

        // Another video product is not owned
        assertFalse(videoOwnershipService.userOwnsVideo(studentId, 999L));

        // Null checks
        assertFalse(videoOwnershipService.userOwnsVideo(null, videoProductId));
        assertFalse(videoOwnershipService.userOwnsVideo(studentId, null));
    }
}
