package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.dto.CheckoutRequest;
import com.allearning.backend.commerce.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    /**
     * Checks out the user's active cart and creates an order in a single transaction.
     *
     * @param userId  authenticated user's ID
     * @param request checkout request containing addressId if physical items present
     * @return created order response
     */
    OrderResponse checkout(Long userId, CheckoutRequest request);

    /**
     * Retrieves paginated orders for the given user, newest first.
     *
     * @param userId   authenticated user's ID
     * @param pageable pageable parameters
     * @return page of order responses
     */
    Page<OrderResponse> getUserOrders(Long userId, Pageable pageable);

    /**
     * Retrieves a single order by ID for the given user.
     * Returns 404 if the order does not exist or does not belong to the user.
     *
     * @param userId  authenticated user's ID
     * @param orderId order ID
     * @return order response
     */
    OrderResponse getUserOrder(Long userId, Long orderId);
}
