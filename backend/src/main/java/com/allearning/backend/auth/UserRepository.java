package com.allearning.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for the {@link User} entity.
 * The {@code findByEmailIgnoreCase} method uses a JPQL lower() comparison
 * to perform a case-insensitive email lookup that aligns with the
 * {@code uq_users_email_lower} unique index in the database.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);
}
