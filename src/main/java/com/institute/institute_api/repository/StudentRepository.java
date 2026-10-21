package com.institute.institute_api.repository;

import com.institute.institute_api.entity.Student;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    boolean existsByNationalId(String nationalId);
    boolean existsByNationalIdAndIdNot(String nationalId, UUID id);
    boolean existsByCourseId(UUID courseId);

    @EntityGraph(attributePaths = "course")
    @NonNull Page<Student> findAll(@NonNull Pageable pageable);
}
