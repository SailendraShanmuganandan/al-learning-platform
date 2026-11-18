package com.allearning.backend.communication.repository;

import com.allearning.backend.communication.entity.Announcement;
import com.allearning.backend.communication.entity.AnnouncementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findByStatus(AnnouncementStatus status, Pageable pageable);

    Optional<Announcement> findByAnnouncementIdAndStatus(Long announcementId, AnnouncementStatus status);

    long countByStatus(AnnouncementStatus status);
}
