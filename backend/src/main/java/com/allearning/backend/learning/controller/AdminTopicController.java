package com.allearning.backend.learning.controller;

import com.allearning.backend.learning.dto.TopicCreateRequest;
import com.allearning.backend.learning.dto.TopicResponse;
import com.allearning.backend.learning.dto.TopicUpdateRequest;
import com.allearning.backend.learning.service.TopicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminTopicController {

    private final TopicService topicService;

    public AdminTopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping("/chapters/{chapterId}/topics")
    public ResponseEntity<List<TopicResponse>> getTopicsForChapter(@PathVariable Long chapterId) {
        return ResponseEntity.ok(topicService.getTopicsForChapter(chapterId));
    }

    @PostMapping("/chapters/{chapterId}/topics")
    public ResponseEntity<TopicResponse> createTopic(
            @PathVariable Long chapterId,
            @Valid @RequestBody TopicCreateRequest request
    ) {
        TopicResponse response = topicService.createTopic(chapterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/topics/{id}")
    public ResponseEntity<TopicResponse> updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody TopicUpdateRequest request
    ) {
        return ResponseEntity.ok(topicService.updateTopic(id, request));
    }
}

