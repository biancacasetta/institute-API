package com.ecocenter.institute_api.repository;

import com.ecocenter.institute_api.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
}
