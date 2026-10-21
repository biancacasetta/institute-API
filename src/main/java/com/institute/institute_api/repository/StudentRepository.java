package com.institute.institute_api.repository;

import com.institute.institute_api.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    boolean existsByNationalId(String nationalId);
    boolean existsByNationalIdAndIdNot(String nationalId, UUID id);
    boolean existsByCourseId(UUID courseId);

    @EntityGraph(attributePaths = "course")
    Page<Student> findAll(Pageable pageable);
}
