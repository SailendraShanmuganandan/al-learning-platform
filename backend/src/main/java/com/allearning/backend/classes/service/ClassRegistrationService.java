package com.allearning.backend.classes.service;

import com.allearning.backend.academic.service.SubjectLookupService;
import com.allearning.backend.academic.service.SubjectLookupService.SubjectBasicInfo;
import com.allearning.backend.classes.dto.AdminRegistrationResponse;
import com.allearning.backend.classes.dto.StudentRegistrationResponse;
import com.allearning.backend.classes.entity.ClassEntity;
import com.allearning.backend.classes.entity.ClassRegistration;
import com.allearning.backend.classes.entity.ClassStatus;
import com.allearning.backend.classes.entity.RegistrationStatus;
import com.allearning.backend.classes.exception.ClassRegistrationConflictException;
import com.allearning.backend.classes.repository.ClassRegistrationRepository;
import com.allearning.backend.classes.repository.ClassRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ClassRegistrationService.class);

    private final ClassRegistrationRepository classRegistrationRepository;
    private final ClassRepository classRepository;
    private final SubjectLookupService subjectLookupService;
    private final StudentLookupService studentLookupService;

    public ClassRegistrationService(
            ClassRegistrationRepository classRegistrationRepository,
            ClassRepository classRepository,
            SubjectLookupService subjectLookupService,
            StudentLookupService studentLookupService
    ) {
        this.classRegistrationRepository = classRegistrationRepository;
        this.classRepository = classRepository;
        this.subjectLookupService = subjectLookupService;
        this.studentLookupService = studentLookupService;
    }

    // -------------------------------------------------------------------------
    // Student Operations
    // -------------------------------------------------------------------------

    public StudentRegistrationResponse register(Long userId, Long classId) {
        // 1. Verify class is ACTIVE under an ACTIVE subject
        ClassEntity classEntity = classRepository.findByClassIdAndStatus(classId, ClassStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        SubjectBasicInfo subject = subjectLookupService.getSubjectInfo(classEntity.getSubjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        if (!subject.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found");
        }

        // 2. Check if student already has a PENDING or CONFIRMED registration for this class
        List<ClassRegistration> active = classRegistrationRepository.findActiveRegistrations(
                userId,
                classId,
                List.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED)
        );
        if (!active.isEmpty()) {
            String currentStatus = active.get(0).getStatus().name();
            throw new ClassRegistrationConflictException(
                    "ALREADY_REGISTERED",
                    "You already have an active registration for this class with status: " + currentStatus,
                    Map.of("currentStatus", currentStatus)
            );
        }

        // 3. Persist new PENDING registration, catching duplicate key violation in case of simultaneous requests
        ClassRegistration registration = new ClassRegistration(userId, classEntity, RegistrationStatus.PENDING);
        try {
            ClassRegistration saved = classRegistrationRepository.saveAndFlush(registration);
            log.info("Student userId={} registered for classId={} registrationId={}", userId, classId, saved.getRegistrationId());
            return mapToStudentResponse(saved, subject.name());
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent registration attempt for userId={} classId={}", userId, classId);
            List<ClassRegistration> recheck = classRegistrationRepository.findActiveRegistrations(
                    userId,
                    classId,
                    List.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED)
            );
            String currentStatus = !recheck.isEmpty() ? recheck.get(0).getStatus().name() : "PENDING";
            throw new ClassRegistrationConflictException(
                    "ALREADY_REGISTERED",
                    "You already have an active registration for this class with status: " + currentStatus,
                    Map.of("currentStatus", currentStatus)
            );
        }
    }

    @Transactional(readOnly = true)
    public List<StudentRegistrationResponse> getStudentRegistrations(Long userId) {
        List<ClassRegistration> list = classRegistrationRepository.findByUserIdWithClass(userId);
        if (list.isEmpty()) {
            return List.of();
        }

        Set<Long> subjectIds = list.stream()
                .map(r -> r.getClassEntity().getSubjectId())
                .collect(Collectors.toSet());
        Map<Long, SubjectBasicInfo> subjectMap = subjectLookupService.getSubjectsInfo(subjectIds);

        return list.stream().map(r -> {
            SubjectBasicInfo s = subjectMap.get(r.getClassEntity().getSubjectId());
            String subjectName = s != null ? s.name() : "Subject #" + r.getClassEntity().getSubjectId();
            return mapToStudentResponse(r, subjectName);
        }).toList();
    }

    @Transactional(readOnly = true)
    public StudentRegistrationResponse getStudentRegistration(Long userId, Long registrationId) {
        ClassRegistration reg = classRegistrationRepository.findByUserIdAndRegistrationIdWithClass(userId, registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        SubjectBasicInfo s = subjectLookupService.getSubjectInfo(reg.getClassEntity().getSubjectId()).orElse(null);
        String subjectName = s != null ? s.name() : "Subject #" + reg.getClassEntity().getSubjectId();

        return mapToStudentResponse(reg, subjectName);
    }

    public StudentRegistrationResponse cancelStudentRegistration(Long userId, Long registrationId) {
        ClassRegistration reg = classRegistrationRepository.findByIdForUpdate(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        // Enforce ownership: another user's registration -> 404
        if (!reg.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found");
        }

        if (reg.getStatus() == RegistrationStatus.CONFIRMED) {
            throw new ClassRegistrationConflictException(
                    "CANCEL_NOT_ALLOWED",
                    "Confirmed registrations can only be cancelled by the teacher. Please contact us."
            );
        }

        if (reg.getStatus() == RegistrationStatus.CANCELLED || reg.getStatus() == RegistrationStatus.REJECTED) {
            throw new ClassRegistrationConflictException(
                    "INVALID_TRANSITION",
                    "Registration is already " + reg.getStatus().name()
            );
        }

        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new ClassRegistrationConflictException(
                    "INVALID_TRANSITION",
                    "Cannot cancel registration in status " + reg.getStatus().name()
            );
        }

        reg.setStatus(RegistrationStatus.CANCELLED);
        ClassRegistration saved = classRegistrationRepository.save(reg);
        log.info("Student userId={} cancelled registrationId={}", userId, registrationId);

        SubjectBasicInfo s = subjectLookupService.getSubjectInfo(saved.getClassEntity().getSubjectId()).orElse(null);
        String subjectName = s != null ? s.name() : "Subject #" + saved.getClassEntity().getSubjectId();

        return mapToStudentResponse(saved, subjectName);
    }

    // -------------------------------------------------------------------------
    // Admin Operations
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<AdminRegistrationResponse> listAdminRegistrations(
            Long classId,
            String statusStr,
            String q,
            Pageable pageable
    ) {
        RegistrationStatus statusFilter = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                statusFilter = RegistrationStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + statusStr);
            }
        }

        List<Long> matchedUserIds = null;
        if (q != null && !q.isBlank()) {
            matchedUserIds = studentLookupService.findUserIdsByQuery(q.trim());
            if (matchedUserIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
        }

        final RegistrationStatus status = statusFilter;
        final List<Long> userIds = matchedUserIds;

        Specification<ClassRegistration> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (classId != null) {
                predicates.add(cb.equal(root.get("classEntity").get("classId"), classId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (userIds != null) {
                predicates.add(root.get("userId").in(userIds));
            }
            query.orderBy(cb.desc(root.get("registeredAt")), cb.desc(root.get("registrationId")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ClassRegistration> page = classRegistrationRepository.findAll(spec, pageable);
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        Set<Long> pageUserIds = page.getContent().stream()
                .map(ClassRegistration::getUserId)
                .collect(Collectors.toSet());
        Map<Long, StudentSummaryDto> studentMap = studentLookupService.getStudentSummaries(pageUserIds);

        List<AdminRegistrationResponse> list = page.getContent().stream().map(r -> {
            StudentSummaryDto student = studentMap.get(r.getUserId());
            return new AdminRegistrationResponse(
                    r.getRegistrationId(),
                    student,
                    r.getClassEntity().getClassId(),
                    r.getClassEntity().getName(),
                    r.getStatus(),
                    r.getRegisteredAt(),
                    r.getUpdatedAt()
            );
        }).toList();

        return new PageImpl<>(list, pageable, page.getTotalElements());
    }

    public AdminRegistrationResponse updateRegistrationStatus(Long id, RegistrationStatus targetStatus) {
        if (targetStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target status is required");
        }

        ClassRegistration reg = classRegistrationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        RegistrationStatus current = reg.getStatus();

        // Allowed transitions:
        // PENDING -> CONFIRMED, PENDING -> REJECTED, PENDING -> CANCELLED, CONFIRMED -> CANCELLED
        boolean valid = false;
        if (current == RegistrationStatus.PENDING) {
            valid = (targetStatus == RegistrationStatus.CONFIRMED ||
                     targetStatus == RegistrationStatus.REJECTED ||
                     targetStatus == RegistrationStatus.CANCELLED);
        } else if (current == RegistrationStatus.CONFIRMED) {
            valid = (targetStatus == RegistrationStatus.CANCELLED);
        }

        if (!valid) {
            throw new ClassRegistrationConflictException(
                    "INVALID_TRANSITION",
                    "Cannot transition registration from " + current + " to " + targetStatus
            );
        }

        reg.setStatus(targetStatus);
        ClassRegistration saved = classRegistrationRepository.save(reg);
        log.info("Admin updated registrationId={} status from {} to {}", id, current, targetStatus);

        Map<Long, StudentSummaryDto> studentMap = studentLookupService.getStudentSummaries(Set.of(saved.getUserId()));
        StudentSummaryDto student = studentMap.get(saved.getUserId());

        return new AdminRegistrationResponse(
                saved.getRegistrationId(),
                student,
                saved.getClassEntity().getClassId(),
                saved.getClassEntity().getName(),
                saved.getStatus(),
                saved.getRegisteredAt(),
                saved.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public long getPendingRegistrationsCount() {
        return classRegistrationRepository.countByStatus(RegistrationStatus.PENDING);
    }

    private StudentRegistrationResponse mapToStudentResponse(ClassRegistration r, String subjectName) {
        return new StudentRegistrationResponse(
                r.getRegistrationId(),
                r.getClassEntity().getClassId(),
                r.getClassEntity().getName(),
                subjectName,
                r.getClassEntity().getScheduleDescription(),
                r.getClassEntity().getFee(),
                r.getStatus(),
                r.getRegisteredAt(),
                r.getUpdatedAt()
        );
    }
}
