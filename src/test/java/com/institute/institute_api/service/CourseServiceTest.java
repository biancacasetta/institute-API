package com.institute.institute_api.service;

// For Service tests, there is no Spring and no database. Unit tests of business logic.

import com.institute.institute_api.dto.course.CourseRequestDTO;
import com.institute.institute_api.dto.course.CourseResponseDTO;
import com.institute.institute_api.embeddable.Timetable;
import com.institute.institute_api.entity.Course;
import com.institute.institute_api.exception.CourseNotEmptyException;
import com.institute.institute_api.exception.DuplicateCourseNameException;
import com.institute.institute_api.repository.CourseRepository;
import com.institute.institute_api.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        Timetable timetable = mockTimetable("Mon,Wed");
        CourseRequestDTO req = mockCourseRequest("1st Grade", BigDecimal.valueOf(45000.00), timetable);

        // simulate calls to repo methods
        when(courseRepository.existsByName("1st Grade")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(call -> {
            Course course = call.getArgument(0);
            course.setId(COURSE_ID);
            return course;
        });

        // receive response DTO
        CourseResponseDTO result = courseService.createCourse(req);

        // get saved Course
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        // assertions
        assertThat(saved.getName()).isEqualTo("1st Grade");
        assertThat(saved.getFee()).isEqualByComparingTo("45000.00");
        assertThat(saved.getTimetables()).containsExactly(timetable);

        assertThat(result.getId()).isEqualTo(COURSE_ID);
        assertThat(result.getName()).isEqualTo("1st Grade");
        assertThat(result.getFee()).isEqualByComparingTo("45000.00");
        assertThat(result.getTimetables()).containsExactly(timetable);
    }

    @Test
    void createCourse_duplicateCourseName_throwsAndDoesNotSave() {
        Timetable timetable = mockTimetable("Tue,Thu");
        CourseRequestDTO req = mockCourseRequest("1st Grade", BigDecimal.valueOf(45000.00), timetable);

        when(courseRepository.existsByName("1st Grade")).thenReturn(true);

        assertThatThrownBy(() -> courseService.createCourse(req))
                .isInstanceOf(DuplicateCourseNameException.class)
                .hasMessageContaining("1st Grade");

        verify(courseRepository, never()).save(any());
    }

    // get

    @Test
    void getAllCourses_returnsMappedDtoList() {
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse()));

        List<CourseResponseDTO> result = courseService.getAllCourses();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("1st Grade");
    }

    @Test
    void getCourseById_exists_returnsDto() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(mockCourse()));

        CourseResponseDTO result = courseService.getCourseById(COURSE_ID);

        assertThat(result.getId()).isEqualTo(COURSE_ID);
        assertThat(result.getFee()).isEqualByComparingTo("45000.00");
    }

    @Test
    void getCourseById_doesNotExist_throwsNotFound() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        CourseResponseDTO result = courseService.getCourseById(COURSE_ID);

        assertThatThrownBy(() -> courseService.getCourseById(COURSE_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(COURSE_ID.toString());
    }

    // update

    @Test
    void updateCourse_validRequest_savesAndReturnsUpdatedDto() {
        Course course = mockCourse();
        Timetable newTimetable = mockTimetable("Wed,Fri");
        CourseRequestDTO req = mockCourseRequest("2nd Grade", BigDecimal.valueOf(50000.00), newTimetable);

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(courseRepository.existsByNameAndIdNot("2nd Grade", COURSE_ID)).thenReturn(false);

        CourseResponseDTO result = courseService.updateCourse(COURSE_ID, req);

        assertThat(course.getName()).isEqualTo("2nd Grade");
        assertThat(course.getFee()).isEqualByComparingTo("50000.00");
        assertThat(course.getTimetables()).containsExactly(newTimetable);

        assertThat(result.getName()).isEqualTo("2nd Grade");
    }

    @Test
    void updateCourse_duplicateCourseName_throwsAndDoesNotUpdate() {
        Timetable newTimetable = mockTimetable("Wed,Fri");
        CourseRequestDTO req = mockCourseRequest("2nd Grade", BigDecimal.valueOf(50000.00), newTimetable);

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(mockCourse()));
        when(courseRepository.existsByNameAndIdNot("2nd Grade", COURSE_ID)).thenReturn(true);

        assertThatThrownBy(() -> courseService.updateCourse(COURSE_ID, req))
                .isInstanceOf(DuplicateCourseNameException.class)
                .hasMessageContaining("2nd Grade");
    }

    @Test
    void updateCourse_doesNotExist_throwsNotFound() {
        Timetable newTimetable = mockTimetable("Wed,Fri");
        CourseRequestDTO req = mockCourseRequest("2nd Grade", BigDecimal.valueOf(50000.00), newTimetable);

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourse(COURSE_ID, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(COURSE_ID.toString());
    }

    // delete

    @Test
    void deleteCourse_empty_deletesCourse() {
        Course course = mockCourse();
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(studentRepository.existsByCourseId(COURSE_ID)).thenReturn(false);

        courseService.deleteCourse(COURSE_ID);

        verify(courseRepository).delete(course);
    }

    @Test
    void deleteCourse_notEmpty_throwsAndDoesNotDelete() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(mockCourse()));
        when(studentRepository.existsByCourseId(COURSE_ID)).thenReturn(true);

        assertThatThrownBy(() -> courseService.deleteCourse(COURSE_ID))
                .isInstanceOf(CourseNotEmptyException.class)
                .hasMessageContaining("1st Grade");

        verify(courseRepository, never()).delete(any());
    }

    @Test
    void deleteCourse_doesNotExist_throwsNotFound() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.deleteCourse(COURSE_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(COURSE_ID.toString());
    }

    // helpers

    private static CourseRequestDTO mockCourseRequest(String name, BigDecimal fee, Timetable... timetable) {
        CourseRequestDTO req = new CourseRequestDTO();
        req.setName(name);
        req.setFee(fee);
        req.setTimetables(new ArrayList<>(List.of(timetable)));

        return req;
    }

    private static Timetable mockTimetable(String days) {
        return Timetable.builder()
                .days(days)
                .startTime(LocalTime.parse("16:00"))
                .endTime(LocalTime.parse("17:00"))
                .build();
    }

    private static Course mockCourse() {
        return Course.builder()
                .id(COURSE_ID)
                .name("1st Grade")
                .fee(BigDecimal.valueOf(45000.00))
                .timetables(new ArrayList<>(List.of(mockTimetable(
                        "Mon,Fri"
                ))))
                .build();
    }
}
