package com.allearning.backend.classes.repository;

import com.allearning.backend.classes.entity.ClassRegistration;
import com.allearning.backend.classes.entity.RegistrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRegistrationRepository extends JpaRepository<ClassRegistration, Long>, JpaSpecificationExecutor<ClassRegistration> {

    @Query("SELECT r FROM ClassRegistration r JOIN FETCH r.classEntity WHERE r.userId = :userId ORDER BY r.registeredAt DESC, r.registrationId DESC")
    List<ClassRegistration> findByUserIdWithClass(@Param("userId") Long userId);

    @Query("SELECT r FROM ClassRegistration r JOIN FETCH r.classEntity WHERE r.userId = :userId AND r.registrationId = :registrationId")
    Optional<ClassRegistration> findByUserIdAndRegistrationIdWithClass(
            @Param("userId") Long userId,
            @Param("registrationId") Long registrationId
    );

    @Query("SELECT r FROM ClassRegistration r WHERE r.userId = :userId AND r.classEntity.classId = :classId AND r.status IN :statuses")
    List<ClassRegistration> findActiveRegistrations(
            @Param("userId") Long userId,
            @Param("classId") Long classId,
            @Param("statuses") Collection<RegistrationStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ClassRegistration r WHERE r.registrationId = :id")
    Optional<ClassRegistration> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(RegistrationStatus status);

    interface ClassRegistrationCountView {
        Long getClassId();
        RegistrationStatus getStatus();
        long getCount();
    }

    @Query("SELECT r.classEntity.classId as classId, r.status as status, COUNT(r) as count " +
           "FROM ClassRegistration r " +
           "WHERE r.status IN ('PENDING', 'CONFIRMED') " +
           "GROUP BY r.classEntity.classId, r.status")
    List<ClassRegistrationCountView> countActiveRegistrationsGrouped();

    @Query("SELECT r FROM ClassRegistration r JOIN FETCH r.classEntity ORDER BY r.registeredAt DESC, r.registrationId DESC")
    List<ClassRegistration> findRecentRegistrations(org.springframework.data.domain.Pageable pageable);
}
