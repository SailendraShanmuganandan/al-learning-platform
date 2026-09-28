package com.allearning.backend.user.repository;

import com.allearning.backend.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the {@link Address} entity.
 */
public interface AddressRepository extends JpaRepository<Address, Long> {

    /** All addresses belonging to a user, newest first. */
    List<Address> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Find a specific address owned by a given user (for ownership checks). */
    Optional<Address> findByAddressIdAndUserId(Long addressId, Long userId);

    /** Count how many addresses a user has. */
    long countByUserId(Long userId);

    /**
     * Clear the default flag on all addresses for a user.
     * Called before setting a new default to avoid violating the partial unique index.
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId AND a.isDefault = true")
    void clearDefaultForUser(@Param("userId") Long userId);

    /**
     * Promotes the most recently created non-default address to default.
     * Used when deleting the current default address and at least one other address exists.
     */
    @Query("SELECT a FROM Address a WHERE a.userId = :userId AND a.addressId <> :excludeId ORDER BY a.createdAt DESC")
    List<Address> findOtherAddressesNewestFirst(@Param("userId") Long userId, @Param("excludeId") Long excludeId);
}
