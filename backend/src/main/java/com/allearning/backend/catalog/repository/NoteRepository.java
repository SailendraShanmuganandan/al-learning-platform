package com.allearning.backend.catalog.repository;

import com.allearning.backend.catalog.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * Atomically decrements the note's stock quantity only if stock_quantity >= quantity.
     * Returns 1 if updated, 0 if insufficient stock or note not found.
     */
    @Modifying
    @Query("UPDATE Note n SET n.stockQuantity = n.stockQuantity - :quantity WHERE n.productId = :productId AND n.stockQuantity >= :quantity")
    int decrementStockIfAvailable(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * Atomically increments the note's stock quantity upon order expiry or cancellation.
     */
    @Modifying
    @Query("UPDATE Note n SET n.stockQuantity = n.stockQuantity + :quantity WHERE n.productId = :productId")
    int incrementStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
