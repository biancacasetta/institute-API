package com.institute.institute_api.controller;

import com.institute.institute_api.security.AuthProperties;
import com.institute.institute_api.dto.auth.AuthResponseDTO;
import com.institute.institute_api.dto.auth.AuthRequestDTO;
import com.institute.institute_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String REFRESH_COOKIE = "refresh_token";
    private final AuthService authService;
    private final AuthProperties props;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO req) {
        return withRefreshCookie(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return withRefreshCookie(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseEntity<AuthResponseDTO> withRefreshCookie(AuthService.AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken(), props.refreshTokenTtl()).toString())
                .body(result.response());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite(props.cookieSameSite())
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
