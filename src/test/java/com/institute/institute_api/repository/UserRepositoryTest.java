package com.institute.institute_api.repository;

import com.institute.institute_api.TestcontainersConfig;
import com.institute.institute_api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    // findByUsername

    @Test
    void findByUsername_exists_returnsUser() {
        User saved = testEntityManager.persistAndFlush(mockUser("admin"));
        testEntityManager.clear();

        Optional<User> found = userRepository.findByUsername("admin");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByUsername_doesNotExist_returnsEmpty() {
        testEntityManager.persistAndFlush(mockUser("admin"));

        assertThat(userRepository.findByUsername("other")).isEmpty();
    }

    // existsByUsername

    @Test
    void existsByUsername_exists_returnsTrue() {
        testEntityManager.persistAndFlush(mockUser("admin"));

        assertThat(userRepository.existsByUsername("admin")).isTrue();
    }

    @Test
    void existsByUsername_doesNotExist_returnsFalse() {
        testEntityManager.persistAndFlush(mockUser("admin"));

        assertThat(userRepository.existsByUsername("other")).isFalse();
    }

    // unique and not-null constraints

    @Test
    void save_duplicateUsername_violatesUniqueConstraint() {
        testEntityManager.persistAndFlush(mockUser("admin"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(mockUser("admin")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_withoutRole_violatesNotNullConstraint() {
        User user = mockUser("admin");
        user.setRole(null);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_withoutPasswordHash_violatesNotNullConstraint() {
        User user = mockUser("admin");
        user.setPasswordHash(null);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // persistence

    @Test
    void save_persistsFieldsAndDefaultsToActive() {
        User saved = testEntityManager.persistAndFlush(mockUser("admin"));
        testEntityManager.clear();

        User loaded = userRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getUsername()).isEqualTo("admin");
        assertThat(loaded.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(loaded.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(loaded.isActive()).isTrue();
    }

    @Test
    void save_storesRoleAsStringNotOrdinal() {
        User saved = testEntityManager.persistAndFlush(mockUser("admin"));

        String column = (String) testEntityManager.getEntityManager()
                .createNativeQuery("SELECT role FROM users WHERE id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();

        assertThat(column).isEqualTo("ADMIN");
    }

    // helpers

    private static User mockUser(String username) {
        return User.builder()
                .username(username)
                .passwordHash("hashed-password")
                .role(User.Role.ADMIN)
                .build();
    }
}
