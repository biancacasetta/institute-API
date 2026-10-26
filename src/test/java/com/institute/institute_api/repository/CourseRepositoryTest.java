package com.institute.institute_api.repository;

// for Repository tests, the custom derived queries are checked
// to see if they work with a real instance of the database (a test instance, though)

import com.institute.institute_api.TestcontainersConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EntityManager entityManager;

    // existsByName

    @Test
    void existsByName_exactMatchExists_returnsTrue() {

    }

    @Test
    void existsByName_caseInsensitiveMatchExists_returnsTrue() {

    }

    @Test
    void existsByName_doesNotExist_returnsFalse() {

    }

    // existsByNameAndIdNot

    @Test
    void existsByNameAndIdNot_availableName_returnsFalse() {

    }

    @Test
    void existsByNameAndIdNot_duplicateName_returnsTrue() {

    }

    // unique constraint

    @Test
    void save_duplicateName_violatesUniqueConstraint() {

    }

    // persistence

    @Test
    void save_persistsFeeAndTimetables() {

    }

    @Test
    void delete_removesTimetableRows() {

    }
}
