package com.allearning.backend.classes.controller;

import com.allearning.backend.classes.dto.PublicClassResponse;
import com.allearning.backend.classes.service.ClassService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes")
public class PublicClassController {

    private final ClassService classService;

    public PublicClassController(ClassService classService) {
        this.classService = classService;
    }

    /**
     * GET /api/classes
     * Returns ACTIVE classes whose subject is ACTIVE, paginated, optional filters subjectId and class_type.
     */
    @GetMapping
    public ResponseEntity<Page<PublicClassResponse>> listClasses(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false, name = "class_type") String classType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<PublicClassResponse> result = classService.listPublicClasses(subjectId, classType, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/classes/{id}
     * Returns 404 if missing, INACTIVE, or under an INACTIVE subject.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PublicClassResponse> getClassById(@PathVariable Long id) {
        PublicClassResponse response = classService.getPublicClass(id);
        return ResponseEntity.ok(response);
    }
}
