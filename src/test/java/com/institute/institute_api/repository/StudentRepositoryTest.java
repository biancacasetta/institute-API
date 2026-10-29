package com.institute.institute_api.repository;

import com.institute.institute_api.TestcontainersConfig;
import com.institute.institute_api.embeddable.Address;
import com.institute.institute_api.entity.Course;
import com.institute.institute_api.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    // existsByNationalId

    @Test
    void existsByNationalId_exists_returnsTrue() {
        testEntityManager.persistAndFlush(mockStudent("12345678", "Doe", null));

        assertThat(studentRepository.existsByNationalId("12345678")).isTrue();
    }

    @Test
    void existsByNationalId_doesNotExist_returnsFalse() {
        testEntityManager.persistAndFlush(mockStudent("12345678", "Doe", null));

        assertThat(studentRepository.existsByNationalId("87654321")).isFalse();
    }

    // existsByNationalIdAndIdNot

    @Test
    void existsByNationalIdAndIdNot_sameStudent_returnsFalse() {
        Student student = testEntityManager.persistAndFlush(
                mockStudent("12345678", "Doe", null));

        assertThat(studentRepository
                .existsByNationalIdAndIdNot("12345678", student.getId())).isFalse();
    }

    @Test
    void existsByNationalIdAndIdNot_differentStudent_returnsTrue() {
        testEntityManager.persistAndFlush(mockStudent("87654321", "Smith", null));
        Student other = testEntityManager.persistAndFlush(
                mockStudent("12345678", "Doe", null));

        assertThat(studentRepository
                .existsByNationalIdAndIdNot("87654321", other.getId())).isTrue();
    }

    // existsByCourseId

    @Test
    void existsByCourseId_enrolled_returnsTrue() {
        Course course = testEntityManager.persistAndFlush(mockCourse("1st Grade"));
        testEntityManager.persistAndFlush(mockStudent("12345678", "Doe", course));

        assertThat(studentRepository.existsByCourseId(course.getId())).isTrue();
    }

    @Test
    void existsByCourseId_emptyCourse_returnsFalse() {
        Course course = testEntityManager.persistAndFlush(mockCourse("1st Grade"));

        assertThat(studentRepository.existsByCourseId(course.getId())).isFalse();
    }

    // unique national ID constraint

    @Test
    void save_duplicateNationalId_violatesUniqueConstraint() {
        testEntityManager.persistAndFlush(mockStudent("12345678", "Doe", null));

        assertThatThrownBy(() -> studentRepository.saveAndFlush(
                mockStudent("12345678", "Smith", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // persistence

    @Test
    void save_persistsFieldsAndAddress() {
        Student saved = testEntityManager.persistAndFlush(
                mockStudent("12345678", "Doe", null));
        testEntityManager.clear();

        Student loaded = studentRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getNationalId()).isEqualTo("12345678");
        assertThat(loaded.getFirstName()).isEqualTo("John");
        assertThat(loaded.getLastName()).isEqualTo("Doe");
        assertThat(loaded.getDob()).isEqualTo(LocalDate.of(1999, 12, 31));
        assertThat(loaded.getEmail()).isEqualTo("john@example.com");
        assertThat(loaded.getAddress().getStreet()).isEqualTo("Main Street");
        assertThat(loaded.getAddress().getNumber()).isEqualTo(1234);
        assertThat(loaded.getAddress().getCity()).isEqualTo("Buenos Aires");
        assertThat(loaded.getAddress().getZip()).isEqualTo(4321);
        assertThat(loaded.getAddress().getCountry()).isEqualTo("Argentina");
        assertThat(loaded.getMiddleName()).isNull();
        assertThat(loaded.getCourse()).isNull();
    }

    @Test
    void save_withCourse_persistsRelation() {
        Course course = testEntityManager.persistAndFlush(mockCourse("1st Grade"));
        Student saved = testEntityManager.persistAndFlush(mockStudent("12345678", "Doe", course));
        testEntityManager.clear();

        Student loaded = studentRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getCourse().getId()).isEqualTo(course.getId());
    }

    // JOIN query to fetch Course together and avoid N+1 problem

    @Test
    void findAll_fetchesCourseInSameQuery() {}

    // helpers

    private static Student mockStudent(String nationalId, String lastName, Course course) {
        return Student.builder()
                .nationalId(nationalId)
                .firstName("John")
                .lastName(lastName)
                .dob(LocalDate.of(1999, 12, 31))
                .email("john@example.com")
                .ownPhone("1122223333")
                .address(mockAddress())
                .course(course)
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

    private static Course mockCourse(String name) {
        return Course.builder()
                .name(name)
                .fee(BigDecimal.valueOf(45000))
                .build();
    }
}
