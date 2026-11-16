package com.institute.institute_api.controller;

import com.institute.institute_api.dto.auth.UserResponseDTO;
import com.institute.institute_api.entity.User;
import com.institute.institute_api.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(UserController.class)
public class UserControllerTest {
    private static final String URL = "/api/users/me";
    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvcTester mvcTester;
    @MockitoBean
    private UserService userService;

    // GET /me

    @Test
    void me_authenticated_returns200AndUserBody() {
        when(userService.getUserById(USER_ID)).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.get().uri(URL).with(authenticatedAs(USER_ID)).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(USER_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.username").isEqualTo("admin");
        assertThat(result).bodyJson().extractingPath("$.role").isEqualTo("ADMIN");
    }

    @Test
    void me_authenticated_doesNotExposePasswordData() {
        when(userService.getUserById(USER_ID)).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.get().uri(URL).with(authenticatedAs(USER_ID)).exchange();

        assertThat(result).bodyText().doesNotContainIgnoringCase("password");
        assertThat(result).bodyText().doesNotContainIgnoringCase("hash");
    }

    @Test
    void me_authenticated_usesJwtSubjectAsUserId() {
        when(userService.getUserById(USER_ID)).thenReturn(mockResponse());

        mvcTester.get().uri(URL).with(authenticatedAs(USER_ID)).exchange();

        verify(userService).getUserById(USER_ID);
    }

    @Test
    void me_userNoLongerExists_returns404() {
        when(userService.getUserById(USER_ID))
                .thenThrow(new EntityNotFoundException("User ID not found: " + USER_ID));

        MvcTestResult result = mvcTester.get().uri(URL).with(authenticatedAs(USER_ID)).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("User ID not found: " + USER_ID);
    }

    // helpers

    private static UserResponseDTO mockResponse() {
        return new UserResponseDTO(USER_ID, "admin", User.Role.ADMIN);
    }

    private static RequestPostProcessor authenticatedAs(UUID userId) {
        return jwt().jwt(j -> j.subject(userId.toString()));
    }

}
