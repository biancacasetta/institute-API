package com.institute.institute_api.security;

import com.institute.institute_api.entity.User;
import com.institute.institute_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminSeedTest {
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "secret";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationArguments args;
    @InjectMocks
    private AdminSeed adminSeed;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminSeed, "username", USERNAME);
        ReflectionTestUtils.setField(adminSeed, "password", PASSWORD);
    }

    @Test
    void run_adminDoesNotExist_savesActiveAdminWithEncodedPassword() {
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

        adminSeed.run(args);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo(USERNAME);
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void run_adminDoesNotExist_neverStoresRawPassword() {
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

        adminSeed.run(args);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo(PASSWORD);
    }

    @Test
    void run_adminAlreadyExists_doesNotSaveOrEncode() {
        when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

        adminSeed.run(args);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
