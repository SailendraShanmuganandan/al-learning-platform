package com.allearning.backend.auth.service;

import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.auth.dto.UserStatisticsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@Transactional(readOnly = true)
public class UserStatisticsServiceImpl implements UserStatisticsService {

    private final UserRepository userRepository;

    public UserStatisticsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserStatisticsDto getUserStatistics(ZoneId zone) {
        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now(zone).minusDays(30);

        long totalStudents = userRepository.countByRole(UserRole.STUDENT);
        long newStudents = userRepository.countByRoleAndCreatedAtAfter(UserRole.STUDENT, thirtyDaysAgo);
        long unverifiedStudents = userRepository.countByRoleAndEmailVerifiedFalse(UserRole.STUDENT);

        return new UserStatisticsDto(totalStudents, newStudents, unverifiedStudents);
    }
}
