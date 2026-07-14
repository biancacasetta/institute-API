package com.ecocenter.institute_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    @Id
    private Integer id;

    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDateTime dob;
    private String email;
    private String ownPhone;
    private String otherPhone;

    @Embedded
    private Address address;

    @ManyToOne
    private Course course;
}
