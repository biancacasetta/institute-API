package com.institute.institute_api.controller;

import com.institute.institute_api.dto.auth.UserResponseDTO;
import com.institute.institute_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(@AuthenticationPrincipal Jwt jwt) {
        UserResponseDTO me = userService.getUserById(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok(me);
    }
}
