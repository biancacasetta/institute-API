package com.institute.institute_api.repository;

// for Repository tests, the custom derived queries are checked
// to see if they work with a real instance of the database (a test instance, though)

import com.institute.institute_api.TestcontainersConfig;
import com.institute.institute_api.embeddable.Timetable;
import com.institute.institute_api.entity.Course;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    // existsByName

    @Test
    void existsByName_exists_returnsTrue() {
        testEntityManager.persistAndFlush(mockCourse("1st Grade"));

        assertThat(courseRepository.existsByName("1st Grade")).isTrue();
    }

    @Test
    void existsByName_doesNotExist_returnsFalse() {
        testEntityManager.persistAndFlush(mockCourse("1st Grade"));

        assertThat(courseRepository.existsByName("2nd Grade")).isFalse();
    }

    // existsByNameAndIdNot

    @Test
    void existsByNameAndIdNot_availableName_returnsFalse() {
        Course course = testEntityManager.persistAndFlush(mockCourse("1st Grade"));

        assertThat(courseRepository.existsByNameAndIdNot("1st Grade", course.getId())).isFalse();
    }

    @Test
    void existsByNameAndIdNot_duplicateName_returnsTrue() {
        testEntityManager.persistAndFlush(mockCourse("1st Grade"));
        Course other = testEntityManager.persistAndFlush(mockCourse("2nd Grade"));

        assertThat(courseRepository.existsByNameAndIdNot("1st Grade", other.getId())).isTrue();
    }

    // unique constraint

    @Test
    void save_duplicateName_violatesUniqueConstraint() {
        testEntityManager.persistAndFlush(mockCourse("1st Grade"));

        assertThatThrownBy(() -> courseRepository.saveAndFlush(mockCourse("1st Grade")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // persistence

    @Test
    void save_persistsFeeAndTimetables() {
        Course saved = testEntityManager.persistAndFlush(mockCourse("1st Grade"));
        testEntityManager.clear();

        Course loaded = courseRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getName()).isEqualTo("1st Grade");
        assertThat(loaded.getFee()).isEqualByComparingTo("45000.00");
        assertThat(loaded.getTimetables()).hasSize(2);
        assertThat(loaded.getTimetables().getFirst().getDays()).isEqualTo("Mon,Wed");
        assertThat(loaded.getTimetables().getFirst().getStartTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(loaded.getTimetables().getLast().getDays()).isEqualTo("Fri");
    }

    @Test
    void delete_removesTimetableRows() {
        Course saved = testEntityManager.persistAndFlush(mockCourse("1st Grade"));

        courseRepository.delete(saved);
        courseRepository.flush();

        Number remaining = (Number) testEntityManager.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM course_timetables")
                .getSingleResult();

        assertThat(remaining.longValue()).isZero();

    }

    // helpers

    private static Timetable mockTimetable(String days, String start, String end) {
        return Timetable.builder()
                .days(days)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }

    private static Course mockCourse(String name) {
        return Course.builder()
                .name(name)
                .fee(new BigDecimal("45000.00"))
                .timetables(new ArrayList<>(List.of(
                        mockTimetable("Mon,Wed", "16:00", "17:00"),
                        mockTimetable("Fri", "10:00", "11:30"))))
                .build();
    }
}
