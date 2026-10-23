package com.institute.institute_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InstituteApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(InstituteApiApplication.class, args);
	}

}
