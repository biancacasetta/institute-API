package com.institute.institute_api.entity;

import com.institute.institute_api.embeddable.Timetable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "students")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Course {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    private String name;
    private BigDecimal fee;

    @ElementCollection
    @CollectionTable(
            name = "course_timetables",
            joinColumns = @JoinColumn(name = "course_id")
    )
    @Builder.Default
    private List<Timetable> timetables = new ArrayList<>();

    @OneToMany(mappedBy = "course")
    @Builder.Default
    private List<Student> students = new ArrayList<>();
}
