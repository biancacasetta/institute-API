package com.institute.institute_api.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    @NotBlank
    private String street;
    @NotNull
    @Positive
    private Integer number;
    @NotBlank
    private String city;
    @NotNull
    @Positive
    private Integer zip;
    @NotBlank
    private String country;
}
