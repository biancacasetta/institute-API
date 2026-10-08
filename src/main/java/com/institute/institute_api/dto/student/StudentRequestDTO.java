package com.institute.institute_api.dto.student;

import com.institute.institute_api.embeddable.Address;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class StudentRequestDTO {

    @NotNull
    private Integer id;

    @NotBlank
    private String firstName;

    @Nullable
    private String middleName;

    @NotBlank
    private String lastName;

    @NotNull
    private LocalDateTime dob;

    @Email
    @NotBlank
    private String email;

    private String ownPhone;
    private String otherPhone;

    @NotNull
    private Address address;

    @Nullable
    private UUID courseId;
}
