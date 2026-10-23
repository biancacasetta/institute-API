package com.institute.institute_api.security;

import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "institute.auth")
public record AuthProperties(
        @Size(min = 32) String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        boolean cookieSecure,
        String cookieSameSite
) {}
