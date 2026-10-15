package com.allearning.backend.academic.repository;

import com.allearning.backend.academic.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Subject}.
 * Case-insensitive name uniqueness is enforced at DB level; the service
 * uses {@code existsByNameIgnoreCase} to surface a 409 before hitting the DB constraint.
 */
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /** Returns only ACTIVE subjects, ordered by name. */
    List<Subject> findByStatusOrderByNameAsc(String status);

    /** All subjects (admin view), ordered by name. */
    List<Subject> findAllByOrderByNameAsc();

    /** Case-insensitive existence check for duplicate-name guard. */
    @Query("SELECT COUNT(s) > 0 FROM Subject s WHERE lower(s.name) = lower(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    /** Case-insensitive existence check excluding the given subject id (for updates). */
    @Query("SELECT COUNT(s) > 0 FROM Subject s WHERE lower(s.name) = lower(:name) AND s.subjectId <> :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);
}
