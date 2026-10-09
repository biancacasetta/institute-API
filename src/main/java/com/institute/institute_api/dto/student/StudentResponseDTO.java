package com.institute.institute_api.dto.student;

import com.institute.institute_api.entity.Student;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class StudentResponseDTO {
    private UUID id;
    private String nationalId;
    private String firstName;
    private String lastName;
    private String email;

    public static StudentResponseDTO from(Student student) {
        return StudentResponseDTO.builder()
                .id(student.getId())
                .nationalId(student.getNationalId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .build();
    }
}
