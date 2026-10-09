package com.institute.institute_api.repository;

import com.institute.institute_api.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    boolean existsByNationalId(String nationalId);
    boolean otherExistsByNationalId(String nationalId, UUID id);
}
