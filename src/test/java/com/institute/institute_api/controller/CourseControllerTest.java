package com.institute.institute_api.controller;

import com.institute.institute_api.dto.course.CourseRequestDTO;
import com.institute.institute_api.dto.course.CourseResponseDTO;
import com.institute.institute_api.embeddable.Timetable;
import com.institute.institute_api.exception.DuplicateCourseNameException;
import com.institute.institute_api.service.CourseService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CourseController.class)
public class CourseControllerTest {
    private static final String BASE_URL = "/api/courses";
    private static final UUID COURSE_ID = UUID.randomUUID();

    private static final String BODY_CONTENT = """
            {
              "name": "1er Grado",
              "fee": 45000.00,
              "timetables": [
                {
                  "days": "Mon,Wed",
                  "startTime": "16:00",
                  "endTime": "17:00"
                }
              ]
            }
            """;

    @Autowired
    private MockMvcTester mvcTester;
    @MockitoBean
    private CourseService courseService;

    // POST

    @Test
    void createCourse_validRequest_returns201AndBody() {
        when(courseService.createCourse(any(CourseRequestDTO.class))).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(COURSE_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("1st Grade");
        assertThat(result).bodyJson().extractingPath("$.fee").asNumber().isEqualTo(45000.00);
        assertThat(result).bodyJson().extractingPath("$.timetables").asArray().hasSize(1);
        assertThat(result).bodyJson().extractingPath("$.timetables[0].days").isEqualTo("Mon,Wed");
        assertThat(result).bodyJson().extractingPath("$.timetables[0].startTime").asString().startsWith("16:00");
    }

    @Test
    void createCourse_validRequest_passesParsedJsonToService() {
        when(courseService.createCourse(any(CourseRequestDTO.class))).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        ArgumentCaptor<CourseRequestDTO> captor = ArgumentCaptor.forClass(CourseRequestDTO.class);
        verify(courseService).createCourse(captor.capture());
        CourseRequestDTO req = captor.getValue();

        assertThat(req.getName()).isEqualTo("1st Grade");
        assertThat(req.getFee()).isEqualByComparingTo("45000.00");
        assertThat(req.getTimetables()).hasSize(1);
        assertThat(req.getTimetables().getFirst().getStartTime()).isEqualTo(LocalTime.parse("16:00"));
    }

    @Test
    void createCourse_negativeFee_returns400WithFieldError() {
        String json = """
                {"name": "1st Grade", "fee": -10000.00, "timetables": [] }
                """;

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.fee").isNotNull();
        verifyNoInteractions(courseService);
    }

    @Test
    void createCourse_nullTimetables_returns400WithFieldError() {
        String json = """
                { "name": "1st Grade", "fee": 45000.00, "timetables": null }
                """;

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.timetables").isNotNull();
    }

    @Test
    void createCourse_invalidNestedTimetable_returns400WithNestedFieldError() {
        String json = """
                {
                  "name": "1st Grade",
                  "fee": 45000.00,
                  "timetables": [ { "days": "", "startTime": "16:00", "endTime": "17:00" } ]
                }
                """;

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.['timetables[0].days']").isNotNull();
    }

    @Test
    void createCourse_malformedTime_returns400() {
        String json = """
                {
                  "name": "1st Grade",
                  "fee": 45000.00,
                  "timetables": [ { "days": "Mon", "startTime": "banana", "endTime": "17:00" } ]
                }
                """;

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(courseService);
    }

    @Test
    void createCourse_duplicateName_returns409() {
        when(courseService.createCourse(any(CourseRequestDTO.class)))
                .thenThrow(new DuplicateCourseNameException("1st Grade"));

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.detail")
                        .isEqualTo("A course named \"1st Grade\" already exists.");
    }

    // helpers

    private static CourseResponseDTO mockResponse() {
        return CourseResponseDTO.builder()
                .id(COURSE_ID)
                .name("1st Grade")
                .fee(BigDecimal.valueOf(45000.00))
                .timetables(List.of(Timetable.builder()
                        .days("Mon,Wed")
                        .startTime(LocalTime.parse("16:00"))
                        .endTime(LocalTime.parse("17:00"))
                        .build()))
                .build();
    }
}
