package com.institute.institute_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

@ConfigurationProperties(prefix = "institute")
public record InstituteProperties(ZoneId timezone) {
}
