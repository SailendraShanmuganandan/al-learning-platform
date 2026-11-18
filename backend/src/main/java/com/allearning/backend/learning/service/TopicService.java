package com.allearning.backend.learning.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.learning.dto.TopicCreateRequest;
import com.allearning.backend.learning.dto.TopicResponse;
import com.allearning.backend.learning.dto.TopicUpdateRequest;
import com.allearning.backend.learning.entity.ChapterTopic;
import com.allearning.backend.learning.entity.TopicStatus;
import com.allearning.backend.learning.exception.LearningConflictException;
import com.allearning.backend.learning.repository.ChapterTopicRepository;
import com.allearning.backend.learning.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class TopicService {

    private final ChapterTopicRepository chapterTopicRepository;
    private final QuestionRepository questionRepository;
    private final ChapterValidationService chapterValidationService;

    public TopicService(
            ChapterTopicRepository chapterTopicRepository,
            QuestionRepository questionRepository,
            ChapterValidationService chapterValidationService
    ) {
        this.chapterTopicRepository = chapterTopicRepository;
        this.questionRepository = questionRepository;
        this.chapterValidationService = chapterValidationService;
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsForChapter(Long chapterId) {
        verifyChapterExists(chapterId);
        List<ChapterTopic> topics = chapterTopicRepository.findByChapterIdOrderByNameAsc(chapterId);
        return topics.stream()
                .map(t -> {
                    long count = questionRepository.countByTopicId(t.getTopicId());
                    return toTopicResponse(t, count);
                })
                .toList();
    }

    public TopicResponse createTopic(Long chapterId, TopicCreateRequest request) {
        verifyChapterExists(chapterId);
        String trimmedName = request.name().trim();
        if (chapterTopicRepository.existsByChapterIdAndNameIgnoreCase(chapterId, trimmedName)) {
            throw new LearningConflictException("DUPLICATE_TOPIC_NAME", "A topic with name '" + trimmedName + "' already exists in this chapter");
        }

        ChapterTopic topic = new ChapterTopic(chapterId, trimmedName);
        topic.setStatus(TopicStatus.ACTIVE);
        ChapterTopic saved = chapterTopicRepository.save(topic);
        return toTopicResponse(saved, 0L);
    }

    public TopicResponse updateTopic(Long topicId, TopicUpdateRequest request) {
        ChapterTopic topic = chapterTopicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: " + topicId));

        if (request.name() != null && !request.name().isBlank()) {
            String trimmedName = request.name().trim();
            if (!trimmedName.equalsIgnoreCase(topic.getName())) {
                if (chapterTopicRepository.existsByChapterIdAndNameIgnoreCaseAndTopicIdNot(topic.getChapterId(), trimmedName, topicId)) {
                    throw new LearningConflictException("DUPLICATE_TOPIC_NAME", "A topic with name '" + trimmedName + "' already exists in this chapter");
                }
                topic.setName(trimmedName);
            }
        }

        if (request.status() != null) {
            topic.setStatus(request.status());
        }

        ChapterTopic saved = chapterTopicRepository.save(topic);
        long count = questionRepository.countByTopicId(saved.getTopicId());
        return toTopicResponse(saved, count);
    }

    @Transactional(readOnly = true)
    public ChapterTopic getTopicById(Long topicId) {
        return chapterTopicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: " + topicId));
    }

    private void verifyChapterExists(Long chapterId) {
        if (chapterId == null || chapterValidationService.getChapterSummary(chapterId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found: " + chapterId);
        }
    }

    private TopicResponse toTopicResponse(ChapterTopic topic, long questionCount) {
        return new TopicResponse(
                topic.getTopicId(),
                topic.getChapterId(),
                topic.getName(),
                topic.getStatus(),
                questionCount,
                topic.getCreatedAt(),
                topic.getUpdatedAt()
        );
    }
}

