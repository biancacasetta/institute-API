package com.ecocenter.institute_api.embeddable;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Timetable {
    private String days;
    private LocalTime startTime;
    private LocalTime endTime;
}
