package com.institute.institute_api.controller;

import com.institute.institute_api.dto.payment.PaymentRequestDTO;
import com.institute.institute_api.dto.payment.PaymentResponseDTO;
import com.institute.institute_api.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(@Valid @RequestBody PaymentRequestDTO req) {
        PaymentResponseDTO payment = paymentService.createPayment(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> getAllPaymentsByStudent(
            @RequestParam UUID studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<PaymentResponseDTO> payments = paymentService.getAllPaymentsByStudent(studentId, from, to);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(@PathVariable UUID id) {
        PaymentResponseDTO payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> updatePayment(
            @PathVariable UUID id, @Valid @RequestBody PaymentRequestDTO req) {
        PaymentResponseDTO updatedPayment = paymentService.updatePayment(id, req);
        return ResponseEntity.ok(updatedPayment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable UUID id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}
