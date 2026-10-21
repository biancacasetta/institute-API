package com.institute.institute_api.dto.student;

import com.institute.institute_api.dto.course.CourseSummaryDTO;
import com.institute.institute_api.embeddable.Address;
import com.institute.institute_api.entity.Student;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class StudentResponseDTO {
    private UUID id;
    private String nationalId;
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dob;
    private String email;
    private String ownPhone;
    private String otherPhone;
    private Address address;
    private CourseSummaryDTO course;

    public static StudentResponseDTO from(Student student) {
        return StudentResponseDTO.builder()
                .id(student.getId())
                .nationalId(student.getNationalId())
                .firstName(student.getFirstName())
                .middleName(student.getMiddleName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .dob(student.getDob())
                .ownPhone(student.getOwnPhone())
                .otherPhone(student.getOtherPhone())
                .address(student.getAddress())
                .course(student.getCourse() == null ? null : CourseSummaryDTO.from(student.getCourse()))
                .build();
    }
}
