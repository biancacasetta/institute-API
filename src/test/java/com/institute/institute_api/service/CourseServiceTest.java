package com.institute.institute_api.service;

// For Service tests, there is no Spring and no database. Unit tests of business logic.

import com.institute.institute_api.dto.course.CourseRequestDTO;
import com.institute.institute_api.dto.course.CourseResponseDTO;
import com.institute.institute_api.embeddable.Timetable;
import com.institute.institute_api.entity.Course;
import com.institute.institute_api.repository.CourseRepository;
import com.institute.institute_api.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        // create request DTO
        Timetable timetable = testTimetable("Mon,Wed", "16:00", "17:00");
        List<Timetable> timetables = new ArrayList<>(List.of(timetable));
        CourseRequestDTO req = testCourseRequest("1st Grade", BigDecimal.valueOf(45000.00), timetables);

        // simulate calls to repo methods
        when(courseRepository.existsByName("1st Grade")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(call -> {
            Course course = call.getArgument(0);
            course.setId(COURSE_ID);
            return course;
        });

        // receive response DTO
        CourseResponseDTO response = courseService.createCourse(req);

        // get saved Course
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        // assertions
        assertThat(saved.getName()).isEqualTo("1st Grade");
        assertThat(saved.getFee()).isEqualByComparingTo("45000.00");
        assertThat(saved.getTimetables()).containsExactly(timetable);

        assertThat(response.getId()).isEqualTo(COURSE_ID);
        assertThat(response.getName()).isEqualTo("1st Grade");
        assertThat(response.getFee()).isEqualByComparingTo("45000.00");
        assertThat(response.getTimetables()).containsExactly(timetable);
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

    // helpers

    private static CourseRequestDTO testCourseRequest(String name, BigDecimal fee, List<Timetable> timetables) {
        CourseRequestDTO req = new CourseRequestDTO();
        req.setName(name);
        req.setFee(fee);
        req.setTimetables(timetables);

        return req;
    }

    private static Timetable testTimetable(String days, String startTime, String endTime) {
        return Timetable.builder()
                .days(days)
                .startTime(LocalTime.parse(startTime))
                .endTime(LocalTime.parse(endTime))
                .build();
    }
}
