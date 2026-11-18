package com.allearning.backend.academic.service;

import com.allearning.backend.academic.entity.Subject;
import com.allearning.backend.academic.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SubjectLookupServiceImpl implements SubjectLookupService {

    private final SubjectRepository subjectRepository;

    public SubjectLookupServiceImpl(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Override
    public Optional<SubjectBasicInfo> getSubjectInfo(Long subjectId) {
        if (subjectId == null) {
            return Optional.empty();
        }
        return subjectRepository.findById(subjectId)
                .map(s -> new SubjectBasicInfo(s.getSubjectId(), s.getName(), s.getStatus()));
    }

    @Override
    public boolean isSubjectActive(Long subjectId) {
        if (subjectId == null) {
            return false;
        }
        return subjectRepository.findById(subjectId)
                .map(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .orElse(false);
    }

    @Override
    public List<Long> getActiveSubjectIds() {
        return subjectRepository.findByStatusOrderByNameAsc("ACTIVE").stream()
                .map(Subject::getSubjectId)
                .toList();
    }

    @Override
    public Map<Long, SubjectBasicInfo> getSubjectsInfo(Collection<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(
                        Subject::getSubjectId,
                        s -> new SubjectBasicInfo(s.getSubjectId(), s.getName(), s.getStatus())
                ));
    }
}
