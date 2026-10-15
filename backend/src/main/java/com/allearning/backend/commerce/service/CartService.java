package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.dto.CartItemAddRequest;
import com.allearning.backend.commerce.dto.CartItemUpdateRequest;
import com.allearning.backend.commerce.dto.CartResponse;

public interface CartService {

    /**
     * Retrieves the active cart for the given user, or returns an empty cart response
     * without creating a database row if no active cart exists.
     */
    CartResponse getCart(Long userId);

    /**
     * Adds an item to the user's active cart. Lazily creates the cart if it does not yet exist.
     * Merges quantity for notes, rejects duplicates for videos.
     */
    CartResponse addItem(Long userId, CartItemAddRequest request);

    /**
     * Updates the quantity of a note line in the active cart, refreshing its unit price.
     * Rejects quantity != 1 for video items with 400.
     * Returns 404 if the item is not in the user's active cart.
     */
    CartResponse updateItemQuantity(Long userId, Long cartItemId, CartItemUpdateRequest request);

    /**
     * Removes a single item from the active cart.
     * Returns 404 if the item is not in the user's active cart.
     */
    void removeItem(Long userId, Long cartItemId);

    /**
     * Clears all items from the active cart.
     */
    void clearCart(Long userId);
}
