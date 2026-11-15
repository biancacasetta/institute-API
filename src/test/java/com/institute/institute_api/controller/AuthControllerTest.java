package com.institute.institute_api.controller;

import com.institute.institute_api.dto.auth.AuthRequestDTO;
import com.institute.institute_api.dto.auth.AuthResponseDTO;
import com.institute.institute_api.security.AuthProperties;
import com.institute.institute_api.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
@EnableConfigurationProperties(AuthProperties.class)
@TestPropertySource(properties = {
        "institute.auth.cookie-secure=true",
        "institute.auth.cookie-same-site=Lax"
})
public class AuthControllerTest {
    private static final String BASE_URL = "/api/auth";
    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String LOGIN_BODY = """
            { "username": "admin", "password": "secret" }
            """;

    @Autowired
    private MockMvcTester mvcTester;
    @MockitoBean
    private AuthService authService;

    // POST /login

    @Test
    void login_validRequest_returnsAccessTokenInBodyAndRefreshTokenInCookie() {
        when(authService.login(any(AuthRequestDTO.class))).thenReturn(mockResult("raw-refresh"));

        MvcTestResult result = mvcTester.post().uri(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.accessToken").isEqualTo("access-token");
        assertThat(result).bodyJson().extractingPath("$.expiresIn").asNumber().isEqualTo(900);
        assertThat(result).bodyText().doesNotContain("raw-refresh");

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains(
                "refresh_token=raw-refresh",
                "Path=/api/auth",
                "Max-Age=604800", // 7 days in secs
                "HttpOnly",
                "Secure",
                "SameSite=Lax");
    }

    @Test
    void login_validRequest_passesCredentialsToService() {
        when(authService.login(any(AuthRequestDTO.class))).thenReturn(mockResult("raw-refresh"));

        MvcTestResult result = mvcTester.post().uri(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY).exchange();

        ArgumentCaptor<AuthRequestDTO> captor = ArgumentCaptor.forClass(AuthRequestDTO.class);
        verify(authService).login(captor.capture());
        AuthRequestDTO req = captor.getValue();

        assertThat(req.getUsername()).isEqualTo("admin");
        assertThat(req.getPassword()).isEqualTo("secret");
    }

    @Test
    void login_blankFields_returns400WithFieldErrors() {
        String json = """
                {"username": "", "password": "  " }
                """;

        MvcTestResult result = mvcTester.post().uri(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.username").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.password").isNotNull();
        verifyNoInteractions(authService);
    }

    @Test
    void login_invalidCredentials_returns401ProblemDetailWithoutCookie() {
        when(authService.login(any(AuthRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        MvcTestResult result = mvcTester.post().uri(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY).exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Invalid username or password");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    // POST /refresh

    @Test
    void refresh_withCookie_passesCookieValueToServiceAndRotatesCookie() {
        when(authService.refresh("old-refresh")).thenReturn(mockResult("new-refresh"));

        MvcTestResult result = mvcTester.post().uri(BASE_URL + "/refresh")
                .cookie(new Cookie(REFRESH_COOKIE, "old-refresh")).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.accessToken").isEqualTo("access-token");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("refresh_token=new-refresh;");
        verify(authService).refresh("old-refresh");
    }

    @Test
    void refresh_withoutCookie_passesNullToServiceAndReturns401() {
        when(authService.refresh(null))
                .thenThrow(new BadCredentialsException("Invalid or expired refresh token."));

        MvcTestResult result = mvcTester.post().uri(BASE_URL + "/refresh").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("Invalid or expired refresh token.");
        verify(authService).refresh(null);
    }

    @Test
    void refresh_invalidToken_returns401() {
        when(authService.refresh("bad-refresh"))
                .thenThrow(new BadCredentialsException("Invalid or expired refresh token."));

        MvcTestResult result = mvcTester.post().uri(BASE_URL + "/refresh")
                .cookie(new Cookie(REFRESH_COOKIE, "bad-refresh")).exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    // POST logout

    @Test
    void logout_withCookie_returns204AndClearsCookie() {
        MvcTestResult result = mvcTester.post().uri(BASE_URL + "/logout")
                .cookie(new Cookie(REFRESH_COOKIE, "raw-refresh")).exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(authService).logout("raw-refresh");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains(
                "refresh_token=;",
                "Path=/api/auth",
                "Max-Age=0",
                "HttpOnly");
    }

    @Test
    void logout_withoutCookie_stillReturns204AndClearsCookie() {
        MvcTestResult result = mvcTester.post().uri(BASE_URL + "/logout").exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(authService).logout(null);
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains(
                "refresh_token=;",
                "Path=/api/auth",
                "Max-Age=0",
                "HttpOnly");
    }

    // helpers

    private static AuthService.AuthResult mockResult(String refreshToken) {
        return new AuthService.AuthResult(
                new AuthResponseDTO("access-token", 900), refreshToken);
    }

}
