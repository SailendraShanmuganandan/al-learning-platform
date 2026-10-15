package com.allearning.backend.catalog.repository;

import com.allearning.backend.catalog.entity.Product;
import com.allearning.backend.catalog.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySlug(String slug);

    Optional<Product> findBySlug(String slug);

    /**
     * Returns all PUBLISHED products that are linked to at least one of the given chapter IDs.
     * Used for the public chapter-products endpoint and for subject/chapter filter resolution.
     */
    @Query("SELECT DISTINCT p FROM Product p JOIN p.chapterIds c " +
           "WHERE c IN :chapterIds AND p.status = :status")
    List<Product> findByChapterIdInAndStatus(
            @Param("chapterIds") Collection<Long> chapterIds,
            @Param("status") ProductStatus status);
}
