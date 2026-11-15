package com.institute.institute_api.security;

import com.institute.institute_api.entity.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.institute.institute_api.security.JwtService.ROLE_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        assertThat(jwt.getClaimAsString(ROLE_CLAIM)).isEqualTo(role.name());
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

    // decode

    @Test
    void decode_tokenSignedWithDifferentSecret_isRejected() {
        String foreignToken = serviceWith(OTHER_SECRET).createAccessToken(mockUser(User.Role.ADMIN));

        assertThatThrownBy(() -> jwtService.jwtDecoder().decode(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void decode_validSignedTokenWithinLifetime_isAccepted() {
        Instant now = Instant.now();
        String token = tokenWithTimes(
                now.minus(Duration.ofMinutes(1)),
                now.plus(Duration.ofMinutes(5)));

        assertThat(jwtService.jwtDecoder().decode(token).getClaimAsString(JwtService.ROLE_CLAIM))
                .isEqualTo("ADMIN");
    }

    @Test
    void decode_expiredToken_isRejected() {
        Instant now = Instant.now();
        String token = tokenWithTimes(
                now.minus(Duration.ofMinutes(10)),
                now.minus(Duration.ofMinutes(5)));

        assertThatThrownBy(() -> jwtService.jwtDecoder().decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("expired");
    }

    @ParameterizedTest
    @ValueSource(strings = {"garbage", "a.b.c"})
    void decode_malformedToken_isRejected(String token) {
        assertThatThrownBy(() -> jwtService.jwtDecoder().decode(token))
                .isInstanceOf(JwtException.class);
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

    private static String tokenWithTimes(Instant issuedAt, Instant expiresAt) {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("institute-api")
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(ROLE_CLAIM, User.Role.ADMIN.name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}
