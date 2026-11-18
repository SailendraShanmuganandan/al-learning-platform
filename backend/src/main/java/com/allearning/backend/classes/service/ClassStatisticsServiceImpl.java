package com.allearning.backend.classes.service;

import com.allearning.backend.classes.dto.ClassStatisticsDto;
import com.allearning.backend.classes.dto.RecentRegistrationDto;
import com.allearning.backend.classes.entity.ClassRegistration;
import com.allearning.backend.classes.entity.ClassStatus;
import com.allearning.backend.classes.entity.RegistrationStatus;
import com.allearning.backend.classes.repository.ClassRegistrationRepository;
import com.allearning.backend.classes.repository.ClassRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ClassStatisticsServiceImpl implements ClassStatisticsService {

    private final ClassRepository classRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final StudentLookupService studentLookupService;

    public ClassStatisticsServiceImpl(
            ClassRepository classRepository,
            ClassRegistrationRepository classRegistrationRepository,
            StudentLookupService studentLookupService
    ) {
        this.classRepository = classRepository;
        this.classRegistrationRepository = classRegistrationRepository;
        this.studentLookupService = studentLookupService;
    }

    @Override
    public ClassStatisticsDto getClassStatistics() {
        long activeClasses = classRepository.countByStatus(ClassStatus.ACTIVE);
        long pendingRegistrations = classRegistrationRepository.countByStatus(RegistrationStatus.PENDING);
        long confirmedRegistrations = classRegistrationRepository.countByStatus(RegistrationStatus.CONFIRMED);

        List<ClassRegistration> recent = classRegistrationRepository.findRecentRegistrations(PageRequest.of(0, 5));
        List<RecentRegistrationDto> recentDtos;

        if (recent.isEmpty()) {
            recentDtos = Collections.emptyList();
        } else {
            Set<Long> userIds = recent.stream().map(ClassRegistration::getUserId).collect(Collectors.toSet());
            Map<Long, StudentSummaryDto> studentMap = studentLookupService.getStudentSummaries(userIds);

            recentDtos = recent.stream().map(r -> {
                StudentSummaryDto s = studentMap.get(r.getUserId());
                String studentName = (s != null && s.fullName() != null) ? s.fullName() : "Student #" + r.getUserId();
                String className = r.getClassEntity() != null ? r.getClassEntity().getName() : "Class #" + r.getRegistrationId();
                return new RecentRegistrationDto(
                        r.getRegistrationId(),
                        studentName,
                        className,
                        r.getStatus().name(),
                        r.getRegisteredAt()
                );
            }).toList();
        }

        return new ClassStatisticsDto(activeClasses, pendingRegistrations, confirmedRegistrations, recentDtos);
    }
}
