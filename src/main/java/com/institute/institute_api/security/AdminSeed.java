package com.institute.institute_api.security;

import com.institute.institute_api.entity.User;
import com.institute.institute_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeed implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${institute.admin.username}")
    private String username;
    @Value("${institute.admin.password}")
    private String password;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(User.Role.ADMIN)
                .build());
        log.info("Admin user '{}' created", username);
    }
}
