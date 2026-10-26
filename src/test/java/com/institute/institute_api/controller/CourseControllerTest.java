package com.institute.institute_api.controller;

import com.institute.institute_api.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CourseController.class)
public class CourseControllerTest {
    private static final String BASE_URL = "/api/courses";
    private static final UUID COURSE_ID = UUID.randomUUID();

    @Autowired
    private MockMvcTester mvcTester;
    @MockitoBean
    private CourseService courseService;

    // POST

    @Test
    void createCourse_validRequest_returns201AndBody() {

    }

    @Test
    void createCourse_validRequest_passesParsedJsonToService() {

    }

    @Test
    void createCourse_negativeFee_returns400WithFieldError() {

    }

    @Test
    void createCourse_nullTimetables_returns400WithFieldError() {

    }

    @Test
    void createCourse_invalidNestedTimetable_returns400WithNestedFieldError() {

    }

    @Test
    void createCourse_malformedTime_returns400() {

    }

    @Test
    void createCourse_duplicateName_returns409() {

    }
}
