package com.institute.institute_api.dto.course;

import com.institute.institute_api.entity.Course;

import java.util.UUID;

public record CourseSummaryDTO(UUID id, String name) {
    public static CourseSummaryDTO from(Course course) {
        return new CourseSummaryDTO(course.getId(), course.getName());
    }
}