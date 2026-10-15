package com.allearning.backend.user.service;

import com.allearning.backend.auth.AuthService;
import com.allearning.backend.auth.UserResponse;
import com.allearning.backend.user.dto.AddressRequest;
import com.allearning.backend.user.dto.AddressResponse;
import com.allearning.backend.user.entity.Address;
import com.allearning.backend.user.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Application service for user profile and delivery-address operations.
 * <p>
 * Modular-monolith rule: this service does NOT inject {@link com.allearning.backend.auth.UserRepository}
 * directly. All user-entity access goes through {@link AuthService}'s published methods.
 */
@Service
@Transactional
public class UserProfileService {

    private final AuthService authService;
    private final AddressRepository addressRepository;

    public UserProfileService(AuthService authService, AddressRepository addressRepository) {
        this.authService = authService;
        this.addressRepository = addressRepository;
    }

    // -------------------------------------------------------------------------
    // Profile (FR-USR-01)
    // -------------------------------------------------------------------------

    /**
     * Returns the current user's profile.
     *
     * @param email the authenticated user's email (from JWT principal)
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        return authService.getMe(email);
    }

    /**
     * Updates full_name and phone for the current user.
     * Email, role, and account_status are intentionally not modifiable here.
     *
     * @param email    authenticated user's email
     * @param fullName new full name
     * @param phone    new phone (E.164 or blank)
     */
    public UserResponse updateProfile(String email, String fullName, String phone) {
        return authService.updateProfile(email, fullName, phone);
    }

    // -------------------------------------------------------------------------
    // Addresses (FR-USR-02 to FR-USR-04)
    // -------------------------------------------------------------------------

    /**
     * Lists all addresses belonging to the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(String email) {
        Long userId = resolveUserId(email);
        return addressRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    /**
     * Creates a new address.
     * If this is the user's first address it automatically becomes the default (FR-USR-02).
     */
    public AddressResponse createAddress(String email, AddressRequest req) {
        Long userId = resolveUserId(email);
        boolean firstAddress = addressRepository.countByUserId(userId) == 0;
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(req.isDefault());

        if (makeDefault) {
            // Unset any existing default in the same transaction before setting the new one
            addressRepository.clearDefaultForUser(userId);
        }

        Address address = new Address(
                userId,
                req.recipientName(),
                req.phone(),
                req.addressLine1(),
                req.addressLine2(),
                req.city(),
                req.district(),
                req.postalCode(),
                makeDefault
        );
        return AddressResponse.from(addressRepository.save(address));
    }

    /**
     * Returns a single address — only if it belongs to the current user (SEC-06).
     * Returns 404 if not found or not owned (prevents address-ID probing).
     */
    @Transactional(readOnly = true)
    public AddressResponse getAddress(String email, Long addressId) {
        Long userId = resolveUserId(email);
        Address address = findOwnedAddress(userId, addressId);
        return AddressResponse.from(address);
    }

    /**
     * Cross-module internal method returning one of the current user's addresses by ID
     * (owned by that user), as a plain DTO.
     * Returns 404 if not found or not owned.
     */
    @Transactional(readOnly = true)
    public AddressResponse getUserAddress(Long userId, Long addressId) {
        Address address = findOwnedAddress(userId, addressId);
        return AddressResponse.from(address);
    }

    /**
     * Updates a single address (ownership check included).
     * If {@code isDefault} is set to true, previous default is cleared atomically.
     */
    public AddressResponse updateAddress(String email, Long addressId, AddressRequest req) {
        Long userId = resolveUserId(email);
        Address address = findOwnedAddress(userId, addressId);

        address.setRecipientName(req.recipientName());
        address.setPhone(req.phone());
        address.setAddressLine1(req.addressLine1());
        address.setAddressLine2(req.addressLine2());
        address.setCity(req.city());
        address.setDistrict(req.district());
        address.setPostalCode(req.postalCode());

        if (Boolean.TRUE.equals(req.isDefault()) && !address.isDefault()) {
            // Clear existing default, then flag this one
            addressRepository.clearDefaultForUser(userId);
            address.setDefault(true);
        } else if (Boolean.FALSE.equals(req.isDefault()) && address.isDefault()) {
            // Cannot unset default via PATCH; caller must set another address as default instead.
            // Silently ignore to keep the API forgiving.
        }

        return AddressResponse.from(addressRepository.save(address));
    }

    /**
     * Deletes an address (ownership check included).
     * If it was the default and other addresses remain, promotes the most recent one to default (FR-USR-02).
     */
    public void deleteAddress(String email, Long addressId) {
        Long userId = resolveUserId(email);
        Address address = findOwnedAddress(userId, addressId);
        boolean wasDefault = address.isDefault();

        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findOtherAddressesNewestFirst(userId, addressId);
            if (!remaining.isEmpty()) {
                Address promote = remaining.get(0);
                promote.setDefault(true);
                addressRepository.save(promote);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Long resolveUserId(String email) {
        return authService.findUserByEmail(email).getUserId();
    }

    /**
     * Fetches an address and verifies it belongs to the given user.
     * Returns 404 on mismatch (SEC-06: no 403 to prevent ID probing).
     */
    private Address findOwnedAddress(Long userId, Long addressId) {
        return addressRepository.findByAddressIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
    }
}
