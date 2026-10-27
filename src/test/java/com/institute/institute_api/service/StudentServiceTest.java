package com.institute.institute_api.service;

import com.institute.institute_api.repository.CourseRepository;
import com.institute.institute_api.repository.PaymentRepository;
import com.institute.institute_api.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

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
    void createStudent_withoutCourse_savesAndReturnsDto() {}
    @Test
    void createStudent_withCourse_linksCourse() {}
    @Test
    void createStudent_duplicateNationalId_throwsAndDoesNotSave() {}
    @Test
    void createStudent_courseDoesNotExist_throwsNotFoundAndDoesNotSave() {}

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
}
