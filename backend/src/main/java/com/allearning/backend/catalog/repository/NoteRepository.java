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

    @Query("SELECT COUNT(n) FROM Note n JOIN n.product p WHERE p.status = com.allearning.backend.catalog.entity.ProductStatus.PUBLISHED AND n.stockQuantity <= :threshold")
    long countLowStockNotes(@Param("threshold") int threshold);

    @Query("SELECT new com.allearning.backend.catalog.dto.LowStockItemDto(p.productId, p.name, n.stockQuantity) " +
           "FROM Note n JOIN n.product p " +
           "WHERE p.status = com.allearning.backend.catalog.entity.ProductStatus.PUBLISHED AND n.stockQuantity <= :threshold " +
           "ORDER BY n.stockQuantity ASC, p.productId ASC")
    java.util.List<com.allearning.backend.catalog.dto.LowStockItemDto> findLowStockItems(@Param("threshold") int threshold, org.springframework.data.domain.Pageable pageable);
}
