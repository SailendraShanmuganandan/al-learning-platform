package com.allearning.backend.commerce.scheduler;

import com.allearning.backend.commerce.service.OrderExpiryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Scheduled job to expire unpaid pending orders whose expiry window has elapsed.
 * Runs every minute.
 */
@Component
public class OrderExpiryScheduler {

    private final OrderExpiryService orderExpiryService;

    public OrderExpiryScheduler(OrderExpiryService orderExpiryService) {
        this.orderExpiryService = orderExpiryService;
    }

    @Scheduled(cron = "${app.order.expiry-cron:0 * * * * *}")
    public void runExpiryJob() {
        orderExpiryService.expireOrdersAt(OffsetDateTime.now());
    }
}
