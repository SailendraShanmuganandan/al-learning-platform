package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.ChapterDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterDocumentRepository extends JpaRepository<ChapterDocument, Long> {
    List<ChapterDocument> findByChapterIdOrderByCreatedAtDesc(Long chapterId);
}

