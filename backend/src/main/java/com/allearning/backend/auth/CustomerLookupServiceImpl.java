package com.allearning.backend.auth;

import com.allearning.backend.classes.service.StudentLookupService;
import com.allearning.backend.classes.service.StudentSummaryDto;
import com.allearning.backend.commerce.service.CustomerLookupService;
import com.allearning.backend.commerce.service.CustomerSummaryDto;
import com.allearning.backend.communication.service.UserLookupService;
import com.allearning.backend.learning.service.LearningUserLookupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CustomerLookupService}, {@link StudentLookupService}, and {@link UserLookupService}
 * that lives in the auth package so it can access {@link UserRepository} directly.
 */
@Service
@Transactional(readOnly = true)
public class CustomerLookupServiceImpl implements CustomerLookupService, StudentLookupService, UserLookupService, LearningUserLookupService {

    private final UserRepository userRepository;

    public CustomerLookupServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Map<Long, CustomerSummaryDto> getCustomerSummaries(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        u -> new CustomerSummaryDto(
                                u.getUserId(),
                                u.getFullName(),
                                u.getEmail(),
                                u.getPhone()
                        )
                ));
    }

    @Override
    public Long findUserIdByEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return userRepository.findByEmailIgnoreCase(email)
                .map(User::getUserId)
                .orElse(null);
    }

    @Override
    public Map<Long, StudentSummaryDto> getStudentSummaries(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        u -> new StudentSummaryDto(
                                u.getUserId(),
                                u.getFullName(),
                                u.getEmail(),
                                u.getPhone()
                        )
                ));
    }

    @Override
    public List<Long> findUserIdsByQuery(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.findUserIdsByEmailOrName(query.trim());
    }

    @Override
    public Map<Long, String> getUserDisplayNames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        User::getFullName
                ));
    }

    @Override
    public boolean isEmailVerified(Long userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(User::isEmailVerified)
                .orElse(false);
    }

    @Override
    public boolean isEmailVerifiedByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return userRepository.findByEmailIgnoreCase(email.trim())
                .map(User::isEmailVerified)
                .orElse(false);
    }

    @Override
    public java.util.Optional<StudentProfileInfo> getStudentProfile(Long userId) {
        if (userId == null) return java.util.Optional.empty();
        return userRepository.findById(userId)
                .map(u -> new StudentProfileInfo(
                        u.getUserId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole() == UserRole.STUDENT
                ));
    }

    @Override
    public List<Long> getActiveStudentIds(int maxCount) {
        return userRepository.findActiveStudentIds(org.springframework.data.domain.PageRequest.of(0, maxCount));
    }
}
