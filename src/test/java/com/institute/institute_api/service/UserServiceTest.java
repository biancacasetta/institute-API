package com.institute.institute_api.service;

import com.institute.institute_api.entity.User;
import com.institute.institute_api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USERNAME = "admin";

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService userService;

    // loadUserByUsername

    @Test
    void loadUserByUsername_exists_returnsUserDetails() {
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(mockUser(User.Role.ADMIN, true)));

        UserDetails result = userService.loadUserByUsername(USERNAME);

        assertThat(result.getUsername()).isEqualTo(USERNAME);
        assertThat(result.getPassword()).isEqualTo("hashed-password");
        assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(result.isEnabled()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(User.Role.class)
    void loadUserByUsername_anyRole_prefixesAuthorityWithRole(User.Role role) {
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(mockUser(role, true)));

        UserDetails result = userService.loadUserByUsername(USERNAME);

        assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + role.name());
    }

    @Test
    void loadUserByUsername_disabledUser_returnsDisabledUserDetails() {
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(mockUser(User.Role.ADMIN, false)));

        UserDetails result = userService.loadUserByUsername(USERNAME);

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_doesNotExist_throwsUsernameNotFound() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername(USERNAME))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    // findByUsername

    @Test
    void findByUsername_exists_returnsUserEntity() {
        User user = mockUser(User.Role.ADMIN, true);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        assertThat(userService.findByUsername(USERNAME)).isSameAs(user);
    }

    @Test
    void findByUsername_doesNotExist_throwsNotFound() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername(USERNAME))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(USERNAME);
    }

    // helpers

    private static User mockUser(User.Role role, boolean active) {
        return User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .passwordHash("hashed-password")
                .role(role)
                .isActive(active)
                .build();
    }
}
