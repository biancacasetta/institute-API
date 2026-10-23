package com.institute.institute_api.dto.auth;

import com.institute.institute_api.entity.User;

import java.util.UUID;

public record UserResponseDTO(UUID id, String username, User.Role role) {
    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(user.getId(), user.getUsername(), user.getRole());
    }
}
