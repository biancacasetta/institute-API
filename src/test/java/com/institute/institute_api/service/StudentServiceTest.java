package com.institute.institute_api.service;

import com.institute.institute_api.dto.course.CourseRequestDTO;
import com.institute.institute_api.dto.student.StudentRequestDTO;
import com.institute.institute_api.dto.student.StudentResponseDTO;
import com.institute.institute_api.embeddable.Address;
import com.institute.institute_api.embeddable.Timetable;
import com.institute.institute_api.entity.Course;
import com.institute.institute_api.entity.Student;
import com.institute.institute_api.exception.DuplicateCourseNameException;
import com.institute.institute_api.exception.DuplicateNationalIdException;
import com.institute.institute_api.repository.CourseRepository;
import com.institute.institute_api.repository.PaymentRepository;
import com.institute.institute_api.repository.StudentRepository;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @InjectMocks
    private StudentService studentService;

    // create

    @Test
    void createStudent_withoutCourse_savesAndReturnsDto() {
        StudentRequestDTO req = mockStudentRequest(null);

        when(studentRepository.existsByNationalId("12345678")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(call -> {
            Student student = call.getArgument(0);
            student.setId(STUDENT_ID);
            return student;
        });

        StudentResponseDTO result = studentService.createStudent(req);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        Student saved = captor.getValue();

        assertThat(saved.getNationalId()).isEqualTo("12345678");
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getDob()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(saved.getEmail()).isEqualTo("johndoe@example.com");
        assertThat(saved.getAddress()).usingRecursiveComparison().isEqualTo(req.getAddress());
        assertThat(saved.getCourse()).isNull();

        assertThat(result.getId()).isEqualTo(STUDENT_ID);
        assertThat(result.getCourse()).isNull();
        verifyNoInteractions(courseRepository);
    }

    @Test
    void createStudent_withCourse_linksCourse() {
        Course course = mockCourse();
        StudentRequestDTO req = mockStudentRequest(course.getId());

        when(studentRepository.existsByNationalId("12345678")).thenReturn(false);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(studentRepository.save(any(Student.class))).thenAnswer(call ->
            call.getArgument(0));

        StudentResponseDTO result = studentService.createStudent(req);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        Student student = captor.getValue();

        assertThat(student.getCourse()).isSameAs(course);
        assertThat(result.getCourse().name()).isEqualTo("1st Grade");
    }

    @Test
    void createStudent_duplicateNationalId_throwsAndDoesNotSave() {
        StudentRequestDTO req = mockStudentRequest(null);

        when(studentRepository.existsByNationalId("12345678")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(req))
                .isInstanceOf(DuplicateNationalIdException.class)
                .hasMessageContaining("12345678");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void createStudent_courseDoesNotExist_throwsNotFoundAndDoesNotSave() {
        StudentRequestDTO req = mockStudentRequest(COURSE_ID);

        when(studentRepository.existsByNationalId("12345678")).thenReturn(false);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.createStudent(req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(COURSE_ID.toString());

        verify(studentRepository, never()).save(any());
    }

    // read

    @Test
    void getAllStudents_returnsMappedPage() {}
    @Test
    void getStudentById_exists_returnsDto() {}
    @Test
    void getStudentById_doesNotExist_throwsNotFound() {}

    // update

    @Test
    void updateStudent_validRequest_savesAndReturnsUpdatedDto() {}
    @Test
    void updateStudent_nullCourseId_clearsCourse() {}
    @Test
    void updateStudent_newCourseId_changesCourse() {}
    @Test
    void updateStudent_duplicateNationalId_throwsAndDoesNotUpdate() {}
    @Test
    void updateStudent_doesNotExist_throwsNotFound() {}
    @Test
    void updateStudent_courseIdDoesNotExist_throwsNotFoundAndDoesNotUpdate() {}

    // delete

    @Test
    void deleteStudent_withoutPayments_deletesStudent() {}
    @Test
    void deleteStudent_withPayments_throwsAndDoesNotDelete() {}
    @Test
    void deleteStudent_doesNotExist_throwsNotFound() {}

    // helpers

    private static StudentRequestDTO mockStudentRequest(UUID courseId) {
        StudentRequestDTO req = new StudentRequestDTO();
        req.setNationalId("12345678");
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setDob(LocalDate.of(2000, 1, 1));
        req.setEmail("johndoe@example.com");
        req.setOwnPhone("1100002222");
        req.setOtherPhone("1100003333");
        req.setAddress(mockAddress());
        req.setCourseId(courseId);
        return req;
    }

    private static Address mockAddress() {
        return Address.builder()
                .street("Main Street")
                .number(1234)
                .city("Buenos Aires")
                .zip(4321)
                .country("Argentina")
                .build();
    }

    private static Course mockCourse() {
        return Course.builder()
                .id(COURSE_ID)
                .name("1st Grade")
                .fee(BigDecimal.valueOf(45000))
                .build();
    }
}
