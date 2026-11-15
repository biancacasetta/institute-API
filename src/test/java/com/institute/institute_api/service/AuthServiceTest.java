package com.institute.institute_api.service;

import com.institute.institute_api.dto.auth.AuthRequestDTO;
import com.institute.institute_api.entity.RefreshToken;
import com.institute.institute_api.entity.User;
import com.institute.institute_api.repository.RefreshTokenRepository;
import com.institute.institute_api.security.AuthProperties;
import com.institute.institute_api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "secret";
    private static final String RAW_TOKEN = "raw-refresh-token";
    private static final String INVALID_REFRESH_MESSAGE = "Invalid or expired refresh token.";

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserService userService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;

    private final AuthProperties props = new AuthProperties(
            "a".repeat(32),
            Duration.ofMinutes(15),
            Duration.ofDays(7),
            false,
            "Strict");

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager, userService, refreshTokenRepository, jwtService, props);
    }

    // login

    @Test
    void login_validCredentials_returnsAccessTokenAndStoresHashedRefreshToken() {
        User user = mockUser(true);

        when(userService.findByUsername(USERNAME)).thenReturn(user);
        when(jwtService.createAccessToken(user)).thenReturn("access-token");

        Instant before = Instant.now();
        AuthService.AuthResult result = authService.login(mockLoginRequest());
        Instant after = Instant.now();

        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        Authentication auth = authCaptor.getValue();
        assertThat(auth.getPrincipal()).isEqualTo(USERNAME);
        assertThat(auth.getCredentials()).isEqualTo(PASSWORD);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.response().expiresIn()).isEqualTo(900); // 15 min
        assertThat(result.refreshToken()).matches("[A-Za-z0-9_-]{43}");

        assertThat(saved.getTokenHash())
                .isEqualTo(hash(result.refreshToken()))
                .isNotEqualTo(result.refreshToken());
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiration()).isBetween(
                before.plus(props.refreshTokenTtl()), after.plus(props.refreshTokenTtl()));
    }

    @Test
    void login_invalidCredentials_throwsGenericMessageAndIssuesNoTokens() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(mockLoginRequest()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verifyNoInteractions(userService, refreshTokenRepository, jwtService);
    }

    @Test
    void login_disabledUser_isMaskedAsGenericBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(mockLoginRequest()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verifyNoInteractions(refreshTokenRepository, jwtService);
    }

    //refresh

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void refresh_missingOrBlankToken_throwsWithoutRepositoryInteractions(String rawToken) {
        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(INVALID_REFRESH_MESSAGE);

        verifyNoInteractions(refreshTokenRepository, jwtService);
    }

    @Test
    void refresh_unknownToken_throws() {
        when(refreshTokenRepository.findByTokenHash(hash(RAW_TOKEN)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(RAW_TOKEN))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(INVALID_REFRESH_MESSAGE);

        verify(refreshTokenRepository, never()).revokeAllByUser(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_validToken_revokesOldAndIssuesNewPair() {
        User user = mockUser(true);
        RefreshToken stored = mockStoredToken(user,
                Instant.now().plus(Duration.ofDays(1)), false);

        when(refreshTokenRepository.findByTokenHash(hash(RAW_TOKEN)))
                .thenReturn(Optional.of(stored));
        when(jwtService.createAccessToken(user)).thenReturn("new-access-token");

        AuthService.AuthResult result = authService.refresh(RAW_TOKEN);

        assertThat(stored.isRevoked()).isTrue();
        assertThat(result.response().accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isNotEqualTo(RAW_TOKEN);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken fresh = captor.getValue();
        assertThat(fresh.getTokenHash()).isEqualTo(hash(result.refreshToken()));
        assertThat(fresh.isRevoked()).isFalse();
        assertThat(fresh.getUser()).isSameAs(user);

        verify(refreshTokenRepository, never()).revokeAllByUser(any());
    }

    @Test
    void refresh_revokedToken_revokedAllUserTokensAndThrows() {
        User user = mockUser(true);
        RefreshToken stored = mockStoredToken(user, Instant.now().plus(Duration.ofDays(1)), true);

        when(refreshTokenRepository.findByTokenHash(hash(RAW_TOKEN))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(RAW_TOKEN))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(INVALID_REFRESH_MESSAGE);

        verify(refreshTokenRepository).revokeAllByUser(user);
        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void refresh_expiredToken_throwsWithoutIssuingTokens() {
        User user = mockUser(true);
        RefreshToken stored = mockStoredToken(user, Instant.now().minus(Duration.ofMinutes(1)), false);

        when(refreshTokenRepository.findByTokenHash(hash(RAW_TOKEN))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(RAW_TOKEN))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(INVALID_REFRESH_MESSAGE);

        verify(refreshTokenRepository, never()).revokeAllByUser(any());
        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void refresh_disabledUser_throwsWithoutIssuingTokens() {
        User user = mockUser(false);
        RefreshToken stored = mockStoredToken(user, Instant.now().plus(Duration.ofDays(1)), false);

        when(refreshTokenRepository.findByTokenHash(hash(RAW_TOKEN))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(RAW_TOKEN))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(INVALID_REFRESH_MESSAGE);

        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    // logout

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void logout_missingOrBlankToken_doesNothing(String rawToken) {
        authService.logout(rawToken);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void logout_unknownToken_doesNotThrow() {
        when(refreshTokenRepository.findByTokenHash(hash(RAW_TOKEN))).thenReturn(Optional.empty());
        assertThatCode(() -> authService.logout(RAW_TOKEN)).doesNotThrowAnyException();
    }

    @Test
    void logout_validToken_revokedOnlyThatToken() {
        RefreshToken stored = mockStoredToken(
                mockUser(true), Instant.now().plus(Duration.ofDays(1)), false);

        when(refreshTokenRepository.findByTokenHash(hash(RAW_TOKEN))).thenReturn(Optional.of(stored));

        authService.logout(RAW_TOKEN);
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository, never()).revokeAllByUser(any());
    }

    // deleteExpiredTokens

    @Test
    void deleteExpiredTokens_usesCurrentTimeAsCutoff() {
        when(refreshTokenRepository.deleteExpired(any(Instant.class))).thenReturn(3);

        Instant before = Instant.now();
        authService.deleteExpiredTokens();
        Instant after = Instant.now();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteExpired(captor.capture());
        Instant expiration = captor.getValue();

        assertThat(expiration).isBetween(before, after);
    }

    // helpers

    private static User mockUser(boolean active) {
        return User.builder()
                .id(UUID.randomUUID())
                .username(USERNAME)
                .passwordHash("hashed-password")
                .role(User.Role.ADMIN)
                .isActive(active)
                .build();
    }

    private static AuthRequestDTO mockLoginRequest() {
        AuthRequestDTO req = new AuthRequestDTO();
        req.setUsername(USERNAME);
        req.setPassword(PASSWORD);
        return req;
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RefreshToken mockStoredToken(
            User user,
            Instant expiration,
            boolean revoked) {
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash(hash(RAW_TOKEN))
                .user(user)
                .expiration(expiration)
                .revoked(revoked)
                .build();
    }
}
