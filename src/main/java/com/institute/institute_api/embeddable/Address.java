package com.institute.institute_api.embeddable;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    private String street;
    private Integer number;
    private String city;
    private Integer zip;
    private String country;
}
