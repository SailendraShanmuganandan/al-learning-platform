package com.allearning.backend.classes.repository;

import com.allearning.backend.classes.entity.ClassEntity;
import com.allearning.backend.classes.entity.ClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long>, JpaSpecificationExecutor<ClassEntity> {

    List<ClassEntity> findAllByOrderByCreatedAtDesc();

    Optional<ClassEntity> findByClassIdAndStatus(Long classId, ClassStatus status);

    long countByStatus(ClassStatus status);
}
