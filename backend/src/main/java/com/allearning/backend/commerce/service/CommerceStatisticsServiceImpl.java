package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.dto.*;
import com.allearning.backend.commerce.entity.FulfillmentStatus;
import com.allearning.backend.commerce.entity.Order;
import com.allearning.backend.commerce.entity.PaymentStatus;
import com.allearning.backend.commerce.repository.OrderRepository;
import com.allearning.backend.commerce.repository.PaymentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CommerceStatisticsServiceImpl implements CommerceStatisticsService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerLookupService customerLookupService;

    public CommerceStatisticsServiceImpl(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            CustomerLookupService customerLookupService
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.customerLookupService = customerLookupService;
    }

    @Override
    public SalesStatisticsDto getSalesStatistics(ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime endOfDay = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        OffsetDateTime startOfToday = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime startOf7Days = today.minusDays(6).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime startOf30Days = today.minusDays(29).atStartOfDay(zone).toOffsetDateTime();

        BigDecimal revenueToday = orderRepository.calculateRevenueBetween(startOfToday, endOfDay)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal revenueLast7Days = orderRepository.calculateRevenueBetween(startOf7Days, endOfDay)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal revenueLast30Days = orderRepository.calculateRevenueBetween(startOf30Days, endOfDay)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal revenueAllTime = orderRepository.calculateRevenueAllTime()
                .setScale(2, RoundingMode.HALF_UP);

        long paidOrdersCount = orderRepository.countByPaymentStatus(PaymentStatus.PAID);
        long awaitingPaymentCount = orderRepository.countAwaitingPayment();

        return new SalesStatisticsDto(
                revenueToday,
                revenueLast7Days,
                revenueLast30Days,
                revenueAllTime,
                paidOrdersCount,
                awaitingPaymentCount
        );
    }

    @Override
    public CommercePendingActionsDto getPendingActions() {
        long awaitingFulfillment = orderRepository.countByPaymentStatusAndFulfillmentStatus(
                PaymentStatus.PAID,
                FulfillmentStatus.PENDING
        );
        long needsReview = paymentRepository.countByNeedsReviewTrue();

        return new CommercePendingActionsDto(awaitingFulfillment, needsReview);
    }

    @Override
    public List<RecentOrderDto> getRecentOrders(int limit) {
        List<Order> orders = orderRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = orders.stream().map(Order::getUserId).collect(Collectors.toSet());
        Map<Long, CustomerSummaryDto> customerMap = customerLookupService.getCustomerSummaries(userIds);

        return orders.stream().map(o -> {
            CustomerSummaryDto c = customerMap.get(o.getUserId());
            String customerName = (c != null && c.fullName() != null) ? c.fullName() : "Customer #" + o.getUserId();
            BigDecimal total = o.getTotalAmount() != null ? o.getTotalAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return new RecentOrderDto(
                    o.getOrderId(),
                    customerName,
                    total,
                    o.getPaymentStatus().name(),
                    o.getCreatedAt()
            );
        }).toList();
    }

    @Override
    public RevenueSeriesDto getDailyRevenue(int days, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = today.minusDays(days - 1);

        OffsetDateTime startUtc = startDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endUtc = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        List<Object[]> rawList = orderRepository.findDailyRevenueBetween(startUtc, endUtc, zone.getId());
        Map<LocalDate, DailyRevenueEntryDto> map = new HashMap<>();

        for (Object[] row : rawList) {
            LocalDate d;
            if (row[0] instanceof Date sqlDate) {
                d = sqlDate.toLocalDate();
            } else if (row[0] instanceof LocalDate ld) {
                d = ld;
            } else {
                d = LocalDate.parse(row[0].toString());
            }

            BigDecimal rev = row[1] != null
                    ? new BigDecimal(row[1].toString()).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;

            map.put(d, new DailyRevenueEntryDto(d, rev, count));
        }

        List<DailyRevenueEntryDto> entries = new ArrayList<>(days);
        BigDecimal totalRevenue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long totalOrders = 0L;

        for (int i = 0; i < days; i++) {
            LocalDate cur = startDate.plusDays(i);
            DailyRevenueEntryDto entry = map.get(cur);
            if (entry != null) {
                entries.add(entry);
                totalRevenue = totalRevenue.add(entry.revenue());
                totalOrders += entry.paidOrderCount();
            } else {
                entries.add(new DailyRevenueEntryDto(cur, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0L));
            }
        }

        return new RevenueSeriesDto(entries, totalRevenue, totalOrders);
    }

    @Override
    public List<TopProductDto> getTopProducts(int days, int limit, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = today.minusDays(days - 1);

        OffsetDateTime startUtc = startDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endUtc = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        List<Object[]> rawList = orderRepository.findTopProductsBetween(startUtc, endUtc, limit);
        List<TopProductDto> result = new ArrayList<>();

        for (Object[] row : rawList) {
            Long productId = ((Number) row[0]).longValue();
            String name = (String) row[1];
            String type = (String) row[2];
            long qty = ((Number) row[3]).longValue();
            BigDecimal rev = row[4] != null
                    ? new BigDecimal(row[4].toString()).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            result.add(new TopProductDto(productId, name, type, qty, rev));
        }

        return result;
    }
}
