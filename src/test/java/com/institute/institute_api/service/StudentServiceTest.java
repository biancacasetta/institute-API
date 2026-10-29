package com.institute.institute_api.service;

import com.institute.institute_api.dto.student.StudentRequestDTO;
import com.institute.institute_api.dto.student.StudentResponseDTO;
import com.institute.institute_api.embeddable.Address;
import com.institute.institute_api.entity.Course;
import com.institute.institute_api.entity.Student;
import com.institute.institute_api.exception.DuplicateNationalIdException;
import com.institute.institute_api.exception.StudentHasPaymentsException;
import com.institute.institute_api.repository.CourseRepository;
import com.institute.institute_api.repository.PaymentRepository;
import com.institute.institute_api.repository.StudentRepository;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    void getAllStudents_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(studentRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(mockStudent()), pageable, 1));

        Page<StudentResponseDTO> result = studentService.getAllStudents(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getFirstName()).isEqualTo("Jane");
    }

    @Test
    void getStudentById_exists_returnsDto() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(mockStudent()));

        StudentResponseDTO result = studentService.getStudentById(STUDENT_ID);

        assertThat(result.getId()).isEqualTo(STUDENT_ID);
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getCourse()).isNull();
    }

    @Test
    void getStudentById_doesNotExist_throwsNotFound() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(STUDENT_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(STUDENT_ID.toString());
    }

    // update

    @Test
    void updateStudent_validRequest_savesAndReturnsUpdatedDto() {
        Student student = mockStudent();
        StudentRequestDTO req = mockStudentRequest(null);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(studentRepository.existsByNationalIdAndIdNot("12345678", STUDENT_ID))
                .thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(call ->
                call.getArgument(0));

        StudentResponseDTO result = studentService.updateStudent(STUDENT_ID, req);

        verify(studentRepository).save(student);
        assertThat(student.getNationalId()).isEqualTo("12345678");
        assertThat(student.getFirstName()).isEqualTo("John");
        assertThat(student.getMiddleName()).isNull();
        assertThat(student.getDob()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(student.getEmail()).isEqualTo("johndoe@example.com");
        assertThat(student.getOwnPhone()).isEqualTo("1100002222");
        assertThat(student.getOtherPhone()).isEqualTo("1100003333");
        assertThat(student.getAddress()).usingRecursiveComparison().isEqualTo(req.getAddress());

        assertThat(result.getFirstName()).isEqualTo("John");
    }

    @Test
    void updateStudent_nullCourseId_clearsCourse() {
        Student student = mockStudent();
        student.setCourse(mockCourse());
        StudentRequestDTO req = mockStudentRequest(null);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(studentRepository.existsByNationalIdAndIdNot("12345678", STUDENT_ID))
                .thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(call ->
                call.getArgument(0));

        StudentResponseDTO result = studentService.updateStudent(STUDENT_ID, req);

        assertThat(student.getCourse()).isNull();
        assertThat(result.getCourse()).isNull();
        verifyNoInteractions(courseRepository);
    }

    @Test
    void updateStudent_newCourseId_changesCourse() {
        Course newCourse = Course.builder()
                .id(UUID.randomUUID())
                .name("2nd Grade")
                .fee(BigDecimal.valueOf(50000))
                .build();
        Student student = mockStudent();
        student.setCourse(mockCourse());
        StudentRequestDTO req = mockStudentRequest(newCourse.getId());

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(studentRepository.existsByNationalIdAndIdNot("12345678", STUDENT_ID))
                .thenReturn(false);
        when(courseRepository.findById(newCourse.getId())).thenReturn(Optional.of(newCourse));
        when(studentRepository.save(any(Student.class))).thenAnswer(call ->
                call.getArgument(0));

        StudentResponseDTO result = studentService.updateStudent(STUDENT_ID, req);

        assertThat(student.getCourse()).isSameAs(newCourse);
        assertThat(result.getCourse().name()).isEqualTo("2nd Grade");
    }

    @Test
    void updateStudent_duplicateNationalId_throwsAndDoesNotUpdate() {
        StudentRequestDTO req = mockStudentRequest(null);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(mockStudent()));
        when(studentRepository.existsByNationalIdAndIdNot("12345678", STUDENT_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> studentService.updateStudent(STUDENT_ID, req))
                .isInstanceOf(DuplicateNationalIdException.class)
                .hasMessageContaining("12345678");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStudent_doesNotExist_throwsNotFound() {
        StudentRequestDTO req = mockStudentRequest(null);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.updateStudent(STUDENT_ID, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(STUDENT_ID.toString());

        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStudent_courseIdDoesNotExist_throwsNotFoundAndDoesNotUpdate() {
        StudentRequestDTO req = mockStudentRequest(COURSE_ID);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(mockStudent()));
        when(studentRepository.existsByNationalIdAndIdNot("12345678", STUDENT_ID))
                .thenReturn(false);
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.updateStudent(STUDENT_ID, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(COURSE_ID.toString());

        verify(studentRepository, never()).save(any());
    }

    // delete

    @Test
    void deleteStudent_withoutPayments_deletesStudent() {
        Student student = mockStudent();
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(paymentRepository.existsByStudentId(STUDENT_ID)).thenReturn(false);

        studentService.deleteStudent(STUDENT_ID);

        verify(studentRepository).delete(student);
    }

    @Test
    void deleteStudent_withPayments_throwsAndDoesNotDelete() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(mockStudent()));
        when(paymentRepository.existsByStudentId(STUDENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> studentService.deleteStudent(STUDENT_ID))
                .isInstanceOf(StudentHasPaymentsException.class)
                .hasMessageContaining("Jane Doe");

        verify(studentRepository, never()).delete(any());
    }

    @Test
    void deleteStudent_doesNotExist_throwsNotFound() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.deleteStudent(STUDENT_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(STUDENT_ID.toString());
    }

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

    private static Student mockStudent() {
        return Student.builder()
                .id(STUDENT_ID)
                .nationalId("87654321")
                .firstName("Jane")
                .middleName("Anna")
                .lastName("Doe")
                .dob(LocalDate.of(1999, 12, 31))
                .email("janeanna@example.com")
                .ownPhone("1122223333")
                .address(mockAddress())
                .build();
    }
}
