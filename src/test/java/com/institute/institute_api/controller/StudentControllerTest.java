package com.institute.institute_api.controller;

import com.institute.institute_api.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

@WebMvcTest
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
    void createStudent_validRequest_returns201AndBody() {}
    @Test
    void createStudent_validRequest_passesParsedJsonToService() {}
    @Test
    void createStudent_withoutOptionalFields_returns201() {}
    @Test
    void createStudent_invalidEmail_returns400WithFieldError() {}
    @Test
    void createStudent_invalidNationalId_returns400WithFieldError() {}
    @Test
    void createStudent_missingAddress_returns400WithFieldError() {}
    @Test
    void createStudent_invalidNestedAddress_returns400WithNestedFieldError() {}
    @Test
    void createStudent_missingDob_returns400WithFieldError() {}
    @Test
    void createStudent_malformedDob_returns400() {}
    @Test
    void createStudent_malformedCourseId_returns400() {}
    @Test
    void createStudent_duplicateNationalId_returns409() {}
    @Test
    void createStudent_courseIdDoesNotExist_returns404() {}
}
