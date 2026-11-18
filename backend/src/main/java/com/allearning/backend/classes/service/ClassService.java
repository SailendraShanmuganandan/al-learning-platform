package com.allearning.backend.classes.service;

import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.academic.service.SubjectLookupService.SubjectBasicInfo;
import com.allearning.backend.classes.dto.*;
import com.allearning.backend.classes.entity.ClassEntity;
import com.allearning.backend.classes.entity.ClassStatus;
import com.allearning.backend.classes.entity.ClassType;
import com.allearning.backend.classes.entity.RegistrationStatus;
import com.allearning.backend.classes.repository.ClassRegistrationRepository;
import com.allearning.backend.classes.repository.ClassRegistrationRepository.ClassRegistrationCountView;
import com.allearning.backend.classes.repository.ClassRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class ClassService {

    private static final Logger log = LoggerFactory.getLogger(ClassService.class);

    private final ClassRepository classRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final SubjectLookupService subjectLookupService;

    public ClassService(
            ClassRepository classRepository,
            ClassRegistrationRepository classRegistrationRepository,
            SubjectLookupService subjectLookupService
    ) {
        this.classRepository = classRepository;
        this.classRegistrationRepository = classRegistrationRepository;
        this.subjectLookupService = subjectLookupService;
    }

    // -------------------------------------------------------------------------
    // Public Operations
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<PublicClassResponse> listPublicClasses(Long subjectId, String classTypeStr, Pageable pageable) {
        ClassType classTypeFilter = null;
        if (classTypeStr != null && !classTypeStr.isBlank()) {
            try {
                classTypeFilter = ClassType.valueOf(classTypeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid class_type: " + classTypeStr);
            }
        }

        final ClassType classType = classTypeFilter;

        // If a subjectId filter is specified, check whether it is active
        if (subjectId != null) {
            if (!subjectLookupService.isSubjectActive(subjectId)) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            Specification<ClassEntity> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("status"), ClassStatus.ACTIVE));
                predicates.add(cb.equal(root.get("subjectId"), subjectId));
                if (classType != null) {
                    predicates.add(cb.equal(root.get("classType"), classType));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            Page<ClassEntity> page = classRepository.findAll(spec, pageable);
            return mapToPublicPage(page, pageable);
        }

        // When no subjectId is passed, only show classes under ACTIVE subjects
        List<Long> activeSubjectIds = subjectLookupService.getActiveSubjectIds();
        if (activeSubjectIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Specification<ClassEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ClassStatus.ACTIVE));
            predicates.add(root.get("subjectId").in(activeSubjectIds));
            if (classType != null) {
                predicates.add(cb.equal(root.get("classType"), classType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ClassEntity> page = classRepository.findAll(spec, pageable);
        return mapToPublicPage(page, pageable);
    }

    @Transactional(readOnly = true)
    public PublicClassResponse getPublicClass(Long id) {
        ClassEntity entity = classRepository.findByClassIdAndStatus(id, ClassStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        SubjectBasicInfo subject = subjectLookupService.getSubjectInfo(entity.getSubjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        if (!subject.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found");
        }

        return new PublicClassResponse(
                entity.getClassId(),
                entity.getName(),
                entity.getDescription(),
                new PublicSubjectRef(subject.subjectId(), subject.name()),
                entity.getClassType(),
                entity.getScheduleDescription(),
                entity.getFee()
        );
    }

    private Page<PublicClassResponse> mapToPublicPage(Page<ClassEntity> page, Pageable pageable) {
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        Set<Long> subjectIds = page.getContent().stream()
                .map(ClassEntity::getSubjectId)
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, SubjectBasicInfo> subjectMap = subjectLookupService.getSubjectsInfo(subjectIds);

        List<PublicClassResponse> dtoList = page.getContent().stream()
                .map(c -> {
                    SubjectBasicInfo s = subjectMap.get(c.getSubjectId());
                    PublicSubjectRef subRef = s != null
                            ? new PublicSubjectRef(s.subjectId(), s.name())
                            : new PublicSubjectRef(c.getSubjectId(), "Unknown");
                    return new PublicClassResponse(
                            c.getClassId(),
                            c.getName(),
                            c.getDescription(),
                            subRef,
                            c.getClassType(),
                            c.getScheduleDescription(),
                            c.getFee()
                    );
                })
                .toList();

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // Admin Operations
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AdminClassResponse> listAdminClasses() {
        List<ClassEntity> classes = classRepository.findAllByOrderByCreatedAtDesc();
        if (classes.isEmpty()) {
            return List.of();
        }

        Set<Long> subjectIds = classes.stream()
                .map(ClassEntity::getSubjectId)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, SubjectBasicInfo> subjectMap = subjectLookupService.getSubjectsInfo(subjectIds);

        Map<Long, RegistrationCounts> countsMap = loadRegistrationCounts();

        return classes.stream().map(c -> {
            SubjectBasicInfo s = subjectMap.get(c.getSubjectId());
            String subjectName = s != null ? s.name() : "Subject #" + c.getSubjectId();
            RegistrationCounts rc = countsMap.getOrDefault(c.getClassId(), new RegistrationCounts(0, 0));
            return new AdminClassResponse(
                    c.getClassId(),
                    c.getSubjectId(),
                    subjectName,
                    c.getName(),
                    c.getDescription(),
                    c.getClassType(),
                    c.getScheduleDescription(),
                    c.getFee(),
                    c.getStatus(),
                    rc.pendingCount,
                    rc.confirmedCount,
                    c.getCreatedAt(),
                    c.getUpdatedAt()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public AdminClassResponse getAdminClass(Long id) {
        ClassEntity c = classRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        SubjectBasicInfo s = subjectLookupService.getSubjectInfo(c.getSubjectId()).orElse(null);
        String subjectName = s != null ? s.name() : "Subject #" + c.getSubjectId();

        Map<Long, RegistrationCounts> countsMap = loadRegistrationCounts();
        RegistrationCounts rc = countsMap.getOrDefault(c.getClassId(), new RegistrationCounts(0, 0));

        return new AdminClassResponse(
                c.getClassId(),
                c.getSubjectId(),
                subjectName,
                c.getName(),
                c.getDescription(),
                c.getClassType(),
                c.getScheduleDescription(),
                c.getFee(),
                c.getStatus(),
                rc.pendingCount,
                rc.confirmedCount,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    public AdminClassResponse createClass(CreateClassRequest request) {
        // Validate name and schedule
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class name is required");
        }
        if (request.scheduleDescription() == null || request.scheduleDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule description is required");
        }
        if (request.classType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class type is required");
        }
        validateFee(request.fee());

        // Validate subject
        SubjectBasicInfo subject = subjectLookupService.getSubjectInfo(request.subjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject does not exist"));

        if (!subject.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is not active");
        }

        ClassEntity entity = new ClassEntity(
                request.subjectId(),
                request.name().trim(),
                request.description() != null ? request.description().trim() : null,
                request.classType(),
                request.scheduleDescription().trim(),
                request.fee(),
                ClassStatus.ACTIVE
        );

        ClassEntity saved = classRepository.save(entity);
        log.info("Class created: id={} name='{}' subjectId={}", saved.getClassId(), saved.getName(), saved.getSubjectId());

        return new AdminClassResponse(
                saved.getClassId(),
                saved.getSubjectId(),
                subject.name(),
                saved.getName(),
                saved.getDescription(),
                saved.getClassType(),
                saved.getScheduleDescription(),
                saved.getFee(),
                saved.getStatus(),
                0,
                0,
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    public AdminClassResponse updateClass(Long id, UpdateClassRequest request) {
        ClassEntity entity = classRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class name cannot be blank");
            }
            entity.setName(request.name().trim());
        }
        if (request.description() != null) {
            entity.setDescription(request.description().trim());
        }
        if (request.classType() != null) {
            entity.setClassType(request.classType());
        }
        if (request.scheduleDescription() != null) {
            if (request.scheduleDescription().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule description cannot be blank");
            }
            entity.setScheduleDescription(request.scheduleDescription().trim());
        }
        if (request.fee() != null) {
            validateFee(request.fee());
            entity.setFee(request.fee());
        }

        ClassEntity saved = classRepository.save(entity);
        SubjectBasicInfo s = subjectLookupService.getSubjectInfo(saved.getSubjectId()).orElse(null);
        String subjectName = s != null ? s.name() : "Subject #" + saved.getSubjectId();

        Map<Long, RegistrationCounts> countsMap = loadRegistrationCounts();
        RegistrationCounts rc = countsMap.getOrDefault(saved.getClassId(), new RegistrationCounts(0, 0));

        return new AdminClassResponse(
                saved.getClassId(),
                saved.getSubjectId(),
                subjectName,
                saved.getName(),
                saved.getDescription(),
                saved.getClassType(),
                saved.getScheduleDescription(),
                saved.getFee(),
                saved.getStatus(),
                rc.pendingCount,
                rc.confirmedCount,
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    public AdminClassResponse setClassStatus(Long id, ClassStatus status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        ClassEntity entity = classRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        entity.setStatus(status);
        ClassEntity saved = classRepository.save(entity);

        SubjectBasicInfo s = subjectLookupService.getSubjectInfo(saved.getSubjectId()).orElse(null);
        String subjectName = s != null ? s.name() : "Subject #" + saved.getSubjectId();

        Map<Long, RegistrationCounts> countsMap = loadRegistrationCounts();
        RegistrationCounts rc = countsMap.getOrDefault(saved.getClassId(), new RegistrationCounts(0, 0));

        log.info("Class id={} status changed to {}", id, status);

        return new AdminClassResponse(
                saved.getClassId(),
                saved.getSubjectId(),
                subjectName,
                saved.getName(),
                saved.getDescription(),
                saved.getClassType(),
                saved.getScheduleDescription(),
                saved.getFee(),
                saved.getStatus(),
                rc.pendingCount,
                rc.confirmedCount,
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    private void validateFee(BigDecimal fee) {
        if (fee == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fee is required");
        }
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fee must be greater than or equal to 0");
        }
        if (fee.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fee can have at most 2 decimal places");
        }
    }

    private Map<Long, RegistrationCounts> loadRegistrationCounts() {
        List<ClassRegistrationCountView> list = classRegistrationRepository.countActiveRegistrationsGrouped();
        Map<Long, RegistrationCounts> map = new HashMap<>();
        for (ClassRegistrationCountView view : list) {
            RegistrationCounts counts = map.computeIfAbsent(view.getClassId(), k -> new RegistrationCounts(0, 0));
            if (view.getStatus() == RegistrationStatus.PENDING) {
                counts.pendingCount += view.getCount();
            } else if (view.getStatus() == RegistrationStatus.CONFIRMED) {
                counts.confirmedCount += view.getCount();
            }
        }
        return map;
    }

    private static class RegistrationCounts {
        long pendingCount;
        long confirmedCount;

        RegistrationCounts(long pendingCount, long confirmedCount) {
            this.pendingCount = pendingCount;
            this.confirmedCount = confirmedCount;
        }
    }
}
