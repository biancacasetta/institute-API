package com.institute.institute_api.repository;

import com.institute.institute_api.TestcontainersConfig;
import com.institute.institute_api.entity.RefreshToken;
import com.institute.institute_api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class RefreshTokenRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z"); // seconds included to avoid precision loss

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    // findByTokenHash

    @Test
    void findByTokenHash_exists_returnsToken() {
        User user = persistUser("admin");
        RefreshToken saved = testEntityManager.persistAndFlush(
                mockToken(user, "hash-1", NOW.plus(Duration.ofDays(1)), false));
        testEntityManager.clear();

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(hash("hash-1"));

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(found.get().isRevoked()).isFalse();
    }

    @Test
    void findByTokenHash_doesNotExist_returnsEmpty() {
        testEntityManager.persistAndFlush(
                mockToken(persistUser("admin"), "hash-1",
                        NOW.plus(Duration.ofDays(1)), false));

        assertThat(refreshTokenRepository.findByTokenHash(hash("hash-2"))).isEmpty();
    }

    // unique constraint

    @Test
    void save_duplicateTokenHash_violatesUniqueConstraint() {
        User user = persistUser("admin");
        testEntityManager.persistAndFlush(
                mockToken(user, "hash-1", NOW.plus(Duration.ofDays(2)), false));

        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(
                mockToken(user, "hash-1", NOW.plus(Duration.ofDays(2)), false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // revokeAllByUser

    @Test
    void revokeAllByUser_revokesOnlyThatUsersActiveTokens() {
        User admin = persistUser("admin");
        User other = persistUser("other");
        RefreshToken adminActive1 = persistToken(admin, "admin-1", NOW.plus(Duration.ofDays(1)), false);
        RefreshToken adminActive2 = persistToken(admin, "admin-2", NOW.plus(Duration.ofDays(2)), false);
        RefreshToken otherActive = persistToken(other, "other-1", NOW.plus(Duration.ofDays(1)), false);

        refreshTokenRepository.revokeAllByUser(admin);
        testEntityManager.clear();

        assertThat(reload(adminActive1).isRevoked()).isTrue();
        assertThat(reload(adminActive2).isRevoked()).isTrue();
        assertThat(reload(otherActive).isRevoked()).isFalse();
    }

    @Test
    void revokeAllByUser_alreadyRevokedToken_staysRevoked() {
        User admin = persistUser("admin");
        RefreshToken alreadyRevoked = persistToken(admin, "admin-1", NOW.plus(Duration.ofDays(1)), false);
        RefreshToken active = persistToken(admin, "admin-2", NOW.plus(Duration.ofDays(1)), false);

        refreshTokenRepository.revokeAllByUser(admin);
        testEntityManager.clear();

        assertThat(reload(alreadyRevoked).isRevoked()).isTrue();
        assertThat(reload(active).isRevoked()).isTrue();
    }

    @Test
    void revokeAllByUser_userWithoutTokens_doesNothing() {
        User withTokens = persistUser("admin");
        User withoutTokens = persistUser("other");
        RefreshToken token = persistToken(withTokens, "admin-1", NOW.plus(Duration.ofDays(1)), false);

        refreshTokenRepository.revokeAllByUser(withoutTokens);
        testEntityManager.clear();

        assertThat(reload(token).isRevoked()).isFalse();
    }

    // helpers

    private User persistUser(String username) {
        return testEntityManager.persistAndFlush(User.builder()
                .username(username)
                .passwordHash("hashed-password")
                .role(User.Role.ADMIN)
                .build());
    }

    private RefreshToken mockToken(User user, String label, Instant expiration, boolean revoked) {
        return RefreshToken.builder()
                .tokenHash(hash(label))
                .user(user)
                .expiration(expiration)
                .revoked(revoked)
                .build();
    }

    private static String hash(String label) {
        return String.format("%64s", label).replace(' ', '0');
    }

    private RefreshToken persistToken(User user, String label, Instant expiration, boolean revoked) {
        return testEntityManager.persistAndFlush(mockToken(
                user,
                label,
                expiration,
                revoked));
    }

    private RefreshToken reload(RefreshToken token) {
        return testEntityManager.find(RefreshToken.class, token.getId());
    }
}
