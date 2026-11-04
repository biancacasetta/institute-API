package com.institute.institute_api.controller;

import com.institute.institute_api.dto.course.CourseRequestDTO;
import com.institute.institute_api.dto.course.CourseSummaryDTO;
import com.institute.institute_api.dto.student.StudentRequestDTO;
import com.institute.institute_api.dto.student.StudentResponseDTO;
import com.institute.institute_api.embeddable.Address;
import com.institute.institute_api.exception.DuplicateCourseNameException;
import com.institute.institute_api.exception.DuplicateNationalIdException;
import com.institute.institute_api.service.StudentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class StudentControllerTest {
    private static final String BASE_URL = "/api/students";
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

    private static final String BODY_CONTENT = """
            {
              "nationalId": "12345678",
              "firstName": "John",
              "middleName": "Paul",
              "lastName": "Doe",
              "dob": "2005-03-10",
              "email": "johnpaul@example.com",
              "ownPhone": "1122334455",
              "otherPhone": "1122334466",
              "address": {
                "street": "Main Street",
                "number": 1234,
                "city": "Buenos Aires",
                "zip": 4321,
                "country": "Argentina"
              },
              "courseId": "%s"
            }
            """.formatted(COURSE_ID);

    @Autowired
    private MockMvcTester mvcTester;
    @MockitoBean
    private StudentService studentService;

    // POST

    @Test
    void createStudent_validRequest_returns201AndBody() {
        when(studentService.createStudent(any(StudentRequestDTO.class))).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(STUDENT_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.nationalId").isEqualTo("12345678");
        assertThat(result).bodyJson().extractingPath("$.firstName").isEqualTo("John");
        assertThat(result).bodyJson().extractingPath("$.dob").isEqualTo("2005-03-10");
        assertThat(result).bodyJson().extractingPath("$.address.city").isEqualTo("Buenos Aires");
        assertThat(result).bodyJson().extractingPath("$.course.id").isEqualTo(COURSE_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.course.name").isEqualTo("1st Grade");
    }

    @Test
    void createStudent_validRequest_passesParsedJsonToService() {
        when(studentService.createStudent(any(StudentRequestDTO.class))).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        ArgumentCaptor<StudentRequestDTO> captor = ArgumentCaptor.forClass(StudentRequestDTO.class);
        verify(studentService).createStudent(captor.capture());
        StudentRequestDTO req = captor.getValue();

        assertThat(req.getNationalId()).isEqualTo("12345678");
        assertThat(req.getFirstName()).isEqualTo("John");
        assertThat(req.getMiddleName()).isEqualTo("Paul");
        assertThat(req.getDob()).isEqualTo(LocalDate.parse("2005-03-10"));
        assertThat(req.getAddress().getNumber()).isEqualTo(1234);
        assertThat(req.getCourseId()).isEqualTo(COURSE_ID);
    }

    @Test
    void createStudent_withoutOptionalFields_returns201() {
        String json = """
            {
              "nationalId": "12345678",
              "firstName": "John",
              "lastName": "Doe",
              "dob": "2005-03-10",
              "email": "johnpaul@example.com",
              "address": {
                "street": "Main Street",
                "number": 1234,
                "city": "Buenos Aires",
                "zip": 4321,
                "country": "Argentina"
              }
            }
            """;

        when(studentService.createStudent(any(StudentRequestDTO.class))).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        ArgumentCaptor<StudentRequestDTO> captor = ArgumentCaptor.forClass(StudentRequestDTO.class);
        verify(studentService).createStudent(captor.capture());
        StudentRequestDTO req = captor.getValue();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(req.getMiddleName()).isNull();
        assertThat(req.getOwnPhone()).isNull();
        assertThat(req.getCourseId()).isNull();
    }

    @Test
    void createStudent_invalidEmail_returns400WithFieldError() {
        String json = BODY_CONTENT.replace("johnpaul@example.com", "invalid-email.format");

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.email").isNotNull();
        verifyNoInteractions(studentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1234", "123456789", "123abc"})
    void createStudent_invalidNationalId_returns400WithFieldError(String nationalId) {
        String json = BODY_CONTENT.replace("12345678", nationalId);

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.nationalId").isNotNull();
        verifyNoInteractions(studentService);
    }

    @Test
    void createStudent_missingAddress_returns400WithFieldError() {
        String json = """
            {
              "nationalId": "12345678",
              "firstName": "John",
              "lastName": "Doe",
              "dob": "2005-03-10",
              "email": "johnpaul@example.com"
            }
            """;

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.address").isNotNull();
        verifyNoInteractions(studentService);
    }

    @Test
    void createStudent_invalidNestedAddress_returns400WithNestedFieldError() {
        String json = BODY_CONTENT.replace("\"number\": 1234", "\"number\": -123");

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors['address.number']").isNotNull();
        verifyNoInteractions(studentService);
    }

    @Test
    void createStudent_missingDob_returns400WithFieldError() {
        String json = """
            {
              "nationalId": "12345678",
              "firstName": "John",
              "lastName": "Doe",
              "email": "johnpaul@example.com",
              "address": {
                "street": "Main Street",
                "number": 1234,
                "city": "Buenos Aires",
                "zip": 4321,
                "country": "Argentina"
              }
            }
            """;

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.dob").isNotNull();
        verifyNoInteractions(studentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10-03-2005", "2005-03-35", "2005-15-10", "not-a-date"})
    void createStudent_malformedDob_returns400(String dob) {
        String json = BODY_CONTENT.replace("2005-03-10", dob);

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(studentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"298f8a6b-3e-1c4-4bba-9488-8f4b233e63e7", // wrong segment length
    "15ee-168da--20c6-4c1f0-9a2b--2806bf323e706", // too many hyphens
    "123e4567-e89b-12d3-a456", // too short
    "not-a-real-uuid-string-000000000000", // not hexadecimal
    "123e4567e89b12d3a456426614174000"}) // no hyphens
    void createStudent_malformedCourseId_returns400(String courseId) {
        String json = BODY_CONTENT.replace(COURSE_ID.toString(), courseId);

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(studentService);
    }

    @Test
    void createStudent_duplicateNationalId_returns409() {
        when(studentService.createStudent(any(StudentRequestDTO.class)))
                .thenThrow(new DuplicateNationalIdException("12345678"));

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("A student with national ID 12345678 already exists.");
    }

    @Test
    void createStudent_courseIdDoesNotExist_returns404() {
        when(studentService.createStudent(any(StudentRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("Course ID not found: " + COURSE_ID));

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("Course ID not found: " + COURSE_ID);
    }

    // helpers

    private static StudentResponseDTO mockResponse() {
        return StudentResponseDTO.builder()
                .id(STUDENT_ID)
                .nationalId("12345678")
                .firstName("John")
                .middleName("Paul")
                .lastName("Doe")
                .email("johnpaul@example.com")
                .dob(LocalDate.of(2005, 3, 10))
                .ownPhone("1122334455")
                .otherPhone("1122334466")
                .address(mockAddress())
                .course(new CourseSummaryDTO(COURSE_ID, "1st Grade"))
                .build();
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
}
