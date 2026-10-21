package com.institute.institute_api.embeddable;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Timetable {
    @NotBlank
    private String days;
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
}
