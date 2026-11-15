package com.institute.institute_api.security;

import com.institute.institute_api.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtServiceTest {
    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";
    private static final String OTHER_SECRET = "another-secret-that-is-at-least-32-characters-long";
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private final JwtService jwtService = serviceWith(SECRET);

    // createAccessToken

    @Test
    void createAccessToken_validUser_containsSubjectAndIssuer() {
        User user = mockUser(User.Role.ADMIN);
        Jwt jwt = jwtService.jwtDecoder().decode(jwtService.createAccessToken(user));

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("institute-api");
    }

    @ParameterizedTest
    @EnumSource(User.Role.class)
    void createAccessToken_anyRole_putsRoleNameInRoleClaim(User.Role role) {
        Jwt jwt = jwtService.jwtDecoder().decode(jwtService.createAccessToken(mockUser(role)));

        assertThat(jwt.getClaimAsString(JwtService.ROLE_CLAIM)).isEqualTo(role.name());
    }

    @Test
    void createAccessToken_validUser_expiresAfterConfiguredTtl() {
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String token = jwtService.createAccessToken(mockUser(User.Role.ADMIN));
        Instant after = Instant.now();

        Jwt jwt = jwtService.jwtDecoder().decode(token);

        assertThat(jwt.getIssuedAt()).isBetween(before, after);
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(ACCESS_TTL);
    }

    // helpers

    private static JwtService serviceWith(String secret) {
        return new JwtService(new AuthProperties(
                secret, ACCESS_TTL, Duration.ofDays(7), false, "Strict"));
    }

    private static User mockUser(User.Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .username("Someone")
                .passwordHash("hashed-password")
                .role(role)
                .build();
    }
}
