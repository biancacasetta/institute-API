package com.institute.institute_api.entity;

import com.institute.institute_api.embeddable.Address;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String nationalId;

    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dob;
    private String email;
    private String ownPhone;
    private String otherPhone;

    @Embedded
    private Address address;

    @ManyToOne
    private Course course;
}
