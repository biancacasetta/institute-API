package com.institute.institute_api.dto.payment;

import com.institute.institute_api.entity.Payment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class PaymentResponseDTO {
    private UUID id;
    private LocalDate date;
    private BigDecimal amount;
    private UUID studentId;

    public static PaymentResponseDTO from(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .date(payment.getDate())
                .amount(payment.getAmount())
                .studentId(payment.getStudent().getId())
                .build();
    }
}
