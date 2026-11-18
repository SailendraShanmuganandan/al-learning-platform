package com.allearning.backend.learning.repository;

import com.allearning.backend.learning.entity.ChapterTopic;
import com.allearning.backend.learning.entity.TopicStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterTopicRepository extends JpaRepository<ChapterTopic, Long> {

    List<ChapterTopic> findByChapterIdOrderByNameAsc(Long chapterId);

    List<ChapterTopic> findByChapterIdAndStatusOrderByNameAsc(Long chapterId, TopicStatus status);

    Optional<ChapterTopic> findByTopicIdAndChapterId(Long topicId, Long chapterId);

    @Query("SELECT COUNT(t) > 0 FROM ChapterTopic t WHERE t.chapterId = :chapterId AND lower(t.name) = lower(:name)")
    boolean existsByChapterIdAndNameIgnoreCase(@Param("chapterId") Long chapterId, @Param("name") String name);

    @Query("SELECT COUNT(t) > 0 FROM ChapterTopic t WHERE t.chapterId = :chapterId AND lower(t.name) = lower(:name) AND t.topicId <> :excludeTopicId")
    boolean existsByChapterIdAndNameIgnoreCaseAndTopicIdNot(
            @Param("chapterId") Long chapterId,
            @Param("name") String name,
            @Param("excludeTopicId") Long excludeTopicId
    );

    List<ChapterTopic> findByTopicIdIn(Collection<Long> topicIds);
}
