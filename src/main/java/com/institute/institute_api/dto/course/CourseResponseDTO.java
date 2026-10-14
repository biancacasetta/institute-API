package com.institute.institute_api.dto.course;

import com.institute.institute_api.dto.student.StudentResponseDTO;
import com.institute.institute_api.embeddable.Timetable;
import com.institute.institute_api.entity.Course;
import com.institute.institute_api.entity.Student;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CourseResponseDTO {
    private UUID id;
    private String name;
    private BigDecimal fee;
    private List<Timetable> timetables;

    public static CourseResponseDTO from(Course course) {
        return CourseResponseDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .fee(course.getFee())
                .timetables(List.copyOf(course.getTimetables()))
                .build();
    }
}
