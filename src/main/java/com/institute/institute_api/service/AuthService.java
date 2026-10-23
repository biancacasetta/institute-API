package com.institute.institute_api.service;

import com.institute.institute_api.security.AuthProperties;
import com.institute.institute_api.dto.auth.AuthResponseDTO;
import com.institute.institute_api.dto.auth.AuthRequestDTO;
import com.institute.institute_api.dto.auth.UserResponseDTO;
import com.institute.institute_api.entity.RefreshToken;
import com.institute.institute_api.entity.User;
import com.institute.institute_api.repository.RefreshTokenRepository;
import com.institute.institute_api.repository.UserRepository;
import com.institute.institute_api.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthProperties props;

    public record AuthResult(AuthResponseDTO response, String refreshToken) {}

    @Transactional
    public AuthResult login(AuthRequestDTO req) {
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(req.getUsername(), req.getPassword()));
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userService.findByUsername(req.getUsername());
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidRefreshToken();
        }

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(AuthService::invalidRefreshToken);
        User user = stored.getUser();

        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllByUser(user);
            throw invalidRefreshToken();
        }

        if (stored.getExpiration().isBefore(Instant.now()) || !user.isActive()) {
            throw invalidRefreshToken();
        }

        stored.setRevoked(true);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    @Scheduled
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpired(Instant.now());
        log.info("Deleted {} expired refresh tokens", deleted);
    }

    private AuthResult issueTokens(User user) {
        String rawRefresh = generateRefreshToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(hash(rawRefresh))
                .user(user)
                .expiration(Instant.now().plus(props.refreshTokenTtl()))
                .build());

        String accessToken = jwtService.createAccessToken(user);
        return new AuthResult(
                new AuthResponseDTO(accessToken, props.accessTokenTtl().toSeconds()), rawRefresh);
    }

    private static BadCredentialsException invalidRefreshToken() {
        return new BadCredentialsException("Invalid or expired refresh token.");
    }

    private static String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
}
