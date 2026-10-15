package com.allearning.backend.academic.service;

import com.allearning.backend.academic.dto.SubjectRequest;
import com.allearning.backend.academic.dto.SubjectResponse;
import com.allearning.backend.academic.entity.Subject;
import com.allearning.backend.academic.repository.SubjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Application service for subject management.
 * <p>
 * Rules:
 * <ul>
 *   <li>Public consumers only see ACTIVE subjects.</li>
 *   <li>Admins see all subjects regardless of status.</li>
 *   <li>Duplicate name (case-insensitive) is rejected with 409 Conflict.</li>
 *   <li>No hard delete — deactivation is the removal mechanism.</li>
 * </ul>
 */
@Service
@Transactional
public class SubjectService {

    private static final Logger log = LoggerFactory.getLogger(SubjectService.class);

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    // -------------------------------------------------------------------------
    // Public operations (students / anonymous)
    // -------------------------------------------------------------------------

    /**
     * Returns all ACTIVE subjects ordered by name.
     */
    @Transactional(readOnly = true)
    public List<SubjectResponse> listActive() {
        return subjectRepository.findByStatusOrderByNameAsc("ACTIVE").stream()
                .map(SubjectResponse::from)
                .toList();
    }

    /**
     * Returns a single ACTIVE subject.
     * Returns 404 if the subject is INACTIVE or does not exist.
     */
    @Transactional(readOnly = true)
    public SubjectResponse getActiveById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        if (!"ACTIVE".equals(subject.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found");
        }
        return SubjectResponse.from(subject);
    }

    // -------------------------------------------------------------------------
    // Admin operations
    // -------------------------------------------------------------------------

    /**
     * Returns all subjects (including INACTIVE) ordered by name.
     */
    @Transactional(readOnly = true)
    public List<SubjectResponse> listAll() {
        return subjectRepository.findAllByOrderByNameAsc().stream()
                .map(SubjectResponse::from)
                .toList();
    }

    /**
     * Creates a new ACTIVE subject.
     * Throws 409 if a subject with the same name (case-insensitive) already exists.
     */
    public SubjectResponse create(SubjectRequest request) {
        if (subjectRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A subject with that name already exists");
        }
        Subject subject = new Subject(request.name().trim(), request.description());
        Subject saved = subjectRepository.save(subject);
        log.info("Subject created: id={} name='{}'", saved.getSubjectId(), saved.getName());
        return SubjectResponse.from(saved);
    }

    /**
     * Updates name and/or description of an existing subject.
     * Throws 404 if the subject does not exist.
     * Throws 409 if the new name conflicts with another subject.
     */
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));

        if (subjectRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A subject with that name already exists");
        }

        subject.setName(request.name().trim());
        subject.setDescription(request.description());
        return SubjectResponse.from(subjectRepository.save(subject));
    }

    /**
     * Sets the status of a subject to ACTIVE or INACTIVE.
     * Throws 404 if the subject does not exist.
     */
    public SubjectResponse setStatus(Long id, String status) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        subject.setStatus(status);
        return SubjectResponse.from(subjectRepository.save(subject));
    }
}
