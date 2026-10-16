package com.institute.institute_api.service;

import com.institute.institute_api.dto.course.CourseRequestDTO;
import com.institute.institute_api.dto.course.CourseResponseDTO;
import com.institute.institute_api.entity.Course;
import com.institute.institute_api.exception.CourseNotEmptyException;
import com.institute.institute_api.exception.DuplicateCourseNameException;
import com.institute.institute_api.repository.CourseRepository;
import com.institute.institute_api.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public CourseResponseDTO createCourse(CourseRequestDTO req) {
        if (courseRepository.existsByName(req.getName())) {
            throw new DuplicateCourseNameException(req.getName());
        }

        Course course = Course.builder()
                .name(req.getName())
                .fee(req.getFee())
                .timetables(new ArrayList<>(req.getTimetables()))
                .build();

        Course savedCourse = courseRepository.save(course);
        return CourseResponseDTO.from(savedCourse);
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(CourseResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponseDTO getCourseById(UUID id) {
        Course course = findCourse(id);
        return CourseResponseDTO.from(course);
    }

    @Transactional
    public CourseResponseDTO updateCourse(UUID id, CourseRequestDTO req) {
        Course course = findCourse(id);
        if (courseRepository.existsByNameAndIdNot(req.getName(), id)) {
            throw new DuplicateCourseNameException(req.getName());
        }

        course.setName(req.getName());
        course.setFee(req.getFee());
        course.getTimetables().clear();
        course.getTimetables().addAll(req.getTimetables());

        return CourseResponseDTO.from(course);
    }

    @Transactional
    public void deleteCourse(UUID id) {
        Course course = findCourse(id);
        if (studentRepository.existsByCourseId(id)) {
            throw new CourseNotEmptyException(course.getName());
        }

        courseRepository.delete(course);
    }

    private Course findCourse(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course ID not found:" + id));
    }
}
