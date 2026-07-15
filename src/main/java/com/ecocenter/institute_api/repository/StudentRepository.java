package com.ecocenter.institute_api.repository;

import com.ecocenter.institute_api.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Integer> {
}
