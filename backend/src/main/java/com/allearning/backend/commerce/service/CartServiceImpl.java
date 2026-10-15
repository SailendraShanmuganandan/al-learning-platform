package com.allearning.backend.commerce.service;

import com.allearning.backend.catalog.dto.ProductPurchaseFacts;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.dto.CartItemAddRequest;
import com.allearning.backend.commerce.dto.CartItemResponse;
import com.allearning.backend.commerce.dto.CartItemUpdateRequest;
import com.allearning.backend.commerce.dto.CartResponse;
import com.allearning.backend.commerce.entity.Cart;
import com.allearning.backend.commerce.entity.CartItem;
import com.allearning.backend.commerce.entity.CartStatus;
import com.allearning.backend.commerce.exception.CartConflictException;
import com.allearning.backend.commerce.repository.CartItemRepository;
import com.allearning.backend.commerce.repository.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartInternalService cartInternalService;
    private final CatalogService catalogService;
    private final VideoOwnershipService videoOwnershipService;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           CartInternalService cartInternalService,
                           CatalogService catalogService,
                           VideoOwnershipService videoOwnershipService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartInternalService = cartInternalService;
        this.catalogService = catalogService;
        this.videoOwnershipService = videoOwnershipService;
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE);
        if (cartOpt.isEmpty()) {
            return CartResponse.empty();
        }

        return mapToCartResponse(cartOpt.get());
    }

    @Override
    public CartResponse addItem(Long userId, CartItemAddRequest request) {
        Long productId = request.productId();
        Integer requestedQty = request.quantity();

        if (requestedQty == null || requestedQty < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        }

        // Modular-monolith: query catalog via published internal service method
        ProductPurchaseFacts facts = catalogService.getProductPurchaseFacts(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (facts.status() != ProductStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not available for purchase");
        }

        // Validate type constraints
        if (facts.type() == ProductType.VIDEO) {
            if (requestedQty != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video products must have quantity of 1");
            }
            if (videoOwnershipService.userOwnsVideo(userId, productId)) {
                throw new CartConflictException("VIDEO_ALREADY_OWNED", "You already own this video course.");
            }
        } else if (facts.type() == ProductType.PHYSICAL_NOTE) {
            int stock = facts.stockQuantity() != null ? facts.stockQuantity() : 0;
            if (stock == 0) {
                throw new CartConflictException("OUT_OF_STOCK", "Product is out of stock.");
            }
            if (requestedQty > stock) {
                throw new CartConflictException("INSUFFICIENT_STOCK",
                        "Requested quantity exceeds available stock. Only " + stock + " available.");
            }
        }

        // Lazily get or create the active cart (handling concurrent unique-index collisions)
        Cart cart = getOrCreateActiveCart(userId);

        // Check if product is already in the cart
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartCartIdAndProductId(cart.getCartId(), productId);

        if (existingItemOpt.isPresent()) {
            CartItem existing = existingItemOpt.get();
            if (facts.type() == ProductType.VIDEO) {
                throw new CartConflictException("ALREADY_IN_CART", "Video course is already in your cart.");
            }

            // For notes: merge quantity
            int newTotal = existing.getQuantity() + requestedQty;
            int stock = facts.stockQuantity() != null ? facts.stockQuantity() : 0;
            if (stock == 0) {
                throw new CartConflictException("OUT_OF_STOCK", "Product is out of stock.");
            }
            if (newTotal > stock) {
                throw new CartConflictException("INSUFFICIENT_STOCK",
                        "Requested total quantity exceeds available stock. Only " + stock + " available.");
            }

            existing.setQuantity(newTotal);
            // FR-CART-08: unit_price refreshed when quantity updated
            existing.setUnitPrice(facts.currentPrice());
            existing.setUpdatedAt(OffsetDateTime.now());
            cartItemRepository.save(existing);
        } else {
            CartItem newItem = new CartItem(cart, productId, requestedQty, facts.currentPrice());
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(cart);

        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse updateItemQuantity(Long userId, Long cartItemId, CartItemUpdateRequest request) {
        Integer newQty = request.quantity();
        if (newQty == null || newQty < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        }

        // Ownership: returns 404 (not 403) if item is not in current user's active cart
        CartItem item = cartItemRepository.findByCartItemIdAndCartUserIdAndCartStatus(cartItemId, userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        ProductPurchaseFacts facts = catalogService.getProductPurchaseFacts(item.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (facts.type() == ProductType.VIDEO) {
            // Notes only; for a video, any quantity other than 1 returns 400
            if (newQty != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video products must have quantity of 1");
            }
            // Refresh unit price
            item.setUnitPrice(facts.currentPrice());
        } else if (facts.type() == ProductType.PHYSICAL_NOTE) {
            int stock = facts.stockQuantity() != null ? facts.stockQuantity() : 0;
            if (stock == 0) {
                throw new CartConflictException("OUT_OF_STOCK", "Product is out of stock.");
            }
            if (newQty > stock) {
                throw new CartConflictException("INSUFFICIENT_STOCK",
                        "Requested quantity exceeds available stock. Only " + stock + " available.");
            }
            item.setQuantity(newQty);
            // FR-CART-08: unit_price refreshed when quantity updated
            item.setUnitPrice(facts.currentPrice());
        }

        item.setUpdatedAt(OffsetDateTime.now());
        cartItemRepository.save(item);

        return mapToCartResponse(item.getCart());
    }

    @Override
    public void removeItem(Long userId, Long cartItemId) {
        // Ownership: returns 404 (not 403) if item is not in current user's active cart
        CartItem item = cartItemRepository.findByCartItemIdAndCartUserIdAndCartStatus(cartItemId, userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        Cart cart = item.getCart();
        cart.removeItem(item);
        cartItemRepository.delete(item);
        cart.setUpdatedAt(OffsetDateTime.now());
        cartRepository.save(cart);
    }

    @Override
    public void clearCart(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cart.clearItems();
            cart.setUpdatedAt(OffsetDateTime.now());
            cartRepository.save(cart);
        }
    }

    // ── Helper methods ──

    private final java.util.concurrent.ConcurrentHashMap<Long, Object> userLocks = new java.util.concurrent.ConcurrentHashMap<>();

    private Cart getOrCreateActiveCart(Long userId) {
        synchronized (userLocks.computeIfAbsent(userId, k -> new Object())) {
            return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                    .orElseGet(() -> {
                        try {
                            return cartInternalService.createActiveCart(userId);
                        } catch (DataIntegrityViolationException ex) {
                            log.debug("Concurrent active cart creation for user {}: {}", userId, ex.getMessage());
                            return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                                    .orElseThrow(() -> ex);
                        }
                    });
        }
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int totalItemCount = 0;

        for (CartItem item : cart.getItems()) {
            Optional<ProductPurchaseFacts> factsOpt = catalogService.getProductPurchaseFacts(item.getProductId());

            boolean unavailable;
            boolean priceChanged;
            boolean outOfStock = false;
            boolean insufficientStock = false;
            BigDecimal currentPrice;
            String name;
            String slug;
            ProductType type;
            String thumbnailUrl;

            if (factsOpt.isEmpty()) {
                unavailable = true;
                priceChanged = false;
                currentPrice = item.getUnitPrice();
                name = "Unavailable Product";
                slug = "";
                type = ProductType.PHYSICAL_NOTE;
                thumbnailUrl = null;
            } else {
                ProductPurchaseFacts facts = factsOpt.get();
                name = facts.name();
                slug = facts.slug();
                type = facts.type();
                thumbnailUrl = facts.thumbnailUrl();
                currentPrice = facts.currentPrice();

                unavailable = (facts.status() != ProductStatus.PUBLISHED);
                priceChanged = (currentPrice != null && item.getUnitPrice().compareTo(currentPrice) != 0);

                if (type == ProductType.PHYSICAL_NOTE) {
                    int stock = facts.stockQuantity() != null ? facts.stockQuantity() : 0;
                    if (stock == 0) {
                        outOfStock = true;
                    } else if (item.getQuantity() > stock) {
                        insufficientStock = true;
                    }
                }
            }

            BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(lineTotal);
            totalItemCount += item.getQuantity();

            itemResponses.add(new CartItemResponse(
                    item.getCartItemId(),
                    item.getProductId(),
                    name,
                    slug,
                    type,
                    thumbnailUrl,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    lineTotal,
                    currentPrice,
                    priceChanged,
                    unavailable,
                    outOfStock,
                    insufficientStock
            ));
        }

        return new CartResponse(cart.getCartId(), itemResponses, totalItemCount, subtotal);
    }
}
