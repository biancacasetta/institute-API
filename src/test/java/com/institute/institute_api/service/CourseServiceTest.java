package com.institute.institute_api.service;

// For Service tests, there is no Spring and no database. Unit tests of business logic.

import com.institute.institute_api.repository.CourseRepository;
import com.institute.institute_api.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class CourseServiceTest {
    private static final UUID COURSE_ID = UUID.randomUUID();

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private StudentRepository studentRepository;
    @InjectMocks
    private CourseService courseService;

    // test naming convention: method_condition_expectation()

    // create

    @Test
    void createCourse_validRequest_savesAndReturnsDto() {

    }

    @Test
    void createCourse_duplicateCourseName_throwsAndDoesNotSave() {

    }

    // get

    @Test
    void getAllCourses_returnsMappedDtoList() {

    }

    @Test
    void getCourseById_exists_returnsDto() {

    }

    @Test
    void getCourseById_doesNotExist_throwsNotFound() {

    }

    // update

    @Test
    void updateCourse_validRequest_savesAndReturnsUpdatedDto() {

    }

    @Test
    void updateCourse_duplicateCourseName_throwsAndDoesNotUpdate() {

    }

    @Test
    void updateCourse_doesNotExist_throwsNotFound() {

    }

    // delete

    @Test
    void deleteCourse_empty_deletesCourse() {

    }

    @Test
    void deleteCourse_notEmpty_throwsAndDoesNotDelete() {

    }

    @Test
    void deleteCourse_doesNotExist_throwsNotFound() {

    }
}
