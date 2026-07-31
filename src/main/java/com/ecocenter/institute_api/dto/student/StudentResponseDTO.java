package com.ecocenter.institute_api.dto.student;

import com.ecocenter.institute_api.entity.Student;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentResponseDTO {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;

    public static StudentResponseDTO from(Student student) {
        return StudentResponseDTO.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .build();
    }
}
