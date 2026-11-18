package com.allearning.backend.communication.repository;

import com.allearning.backend.communication.entity.ContactMessage;
import com.allearning.backend.communication.entity.ContactMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    long countByStatus(ContactMessageStatus status);

    @Query("SELECT m FROM ContactMessage m WHERE " +
           "(:status IS NULL OR m.status = :status) AND " +
           "(:q IS NULL OR :q = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(m.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<ContactMessage> findMessages(
            @Param("status") ContactMessageStatus status,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("SELECT new com.allearning.backend.communication.dto.RecentContactMessageDto(m.messageId, m.name, CAST(m.status AS string), m.createdAt) " +
           "FROM ContactMessage m ORDER BY m.createdAt DESC")
    java.util.List<com.allearning.backend.communication.dto.RecentContactMessageDto> findRecentMessages(Pageable pageable);
}
