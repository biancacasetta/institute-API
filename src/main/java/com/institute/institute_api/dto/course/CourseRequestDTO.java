package com.institute.institute_api.dto.course;

import com.institute.institute_api.embeddable.Timetable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CourseRequestDTO {
    @NotBlank
    private String name;
    @NotNull
    @Positive
    private BigDecimal fee;
    @NotNull
    @Valid
    private List<Timetable> timetables = new ArrayList<>();
}
