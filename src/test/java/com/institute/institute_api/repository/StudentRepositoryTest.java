package com.institute.institute_api.repository;

import com.institute.institute_api.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

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
    void existsByNationalId_exists_returnsTrue() {}
    @Test
    void existsByNationalId_doesNotExist_returnsFalse() {}

    // existsByNationalIdAndIdNot

    @Test
    void existsByNationalIdAndIdNot_sameStudent_returnsFalse() {}
    @Test
    void existsByNationalIdAndIdNot_differentStudent_returnsTrue() {}

    // existsByCourseId

    @Test
    void existsByCourseId_enrolled_returnsTrue() {}
    @Test
    void existsByCourseId_emptyCourse_returnsFalse() {}

    // unique national ID constraint

    @Test
    void save_duplicateNationalId_violatesUniqueConstraint() {}

    // persistence

    @Test
    void save_persistsFieldsAndAddress() {}
    @Test
    void save_withCourse_persistsRelation() {}

    // JOIN query to fetch Course together and avoid N+1 problem

    @Test
    void findAll_fetchesCourseInSameQuery() {}
}
