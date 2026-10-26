package com.institute.institute_api.service;

import com.institute.institute_api.config.InstituteProperties;
import com.institute.institute_api.dto.payment.PaymentRequestDTO;
import com.institute.institute_api.dto.payment.PaymentResponseDTO;
import com.institute.institute_api.entity.Payment;
import com.institute.institute_api.entity.Student;
import com.institute.institute_api.exception.BackToTheFutureException;
import com.institute.institute_api.repository.PaymentRepository;
import com.institute.institute_api.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final InstituteProperties props;

    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO req) {
        Student student = findStudent(req.getStudentId());

        Payment payment = Payment.builder()
                .date(req.getDate())
                .amount(req.getAmount())
                .student(student)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponseDTO.from(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getAllPaymentsByStudent(UUID studentId, LocalDate from, LocalDate to) {
        int year = LocalDate.now(props.timezone()).getYear();
        LocalDate start = from != null ? from : LocalDate.of(year, 1, 1);
        LocalDate end = to != null ? to : LocalDate.of(year, 12, 31);

        if (!studentRepository.existsById(studentId)) {
            throw new EntityNotFoundException("Student ID not found: " + studentId);
        }

        if (start.isAfter(end)) {
            throw new BackToTheFutureException(start, end);
        }

        return paymentRepository.findByStudentIdAndDateBetweenOrderByDateDesc(
                studentId, start, end).stream()
                .map(PaymentResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(UUID id) {
        return PaymentResponseDTO.from(findPayment(id));
    }

    @Transactional
    public PaymentResponseDTO updatePayment(UUID id, PaymentRequestDTO req) {
        Payment payment = findPayment(id);
        Student student = findStudent(req.getStudentId());

        payment.setDate(req.getDate());
        payment.setAmount(req.getAmount());
        payment.setStudent(student);

        return PaymentResponseDTO.from(payment);
    }

    @Transactional
    public void deletePayment(UUID id) {
        Payment payment = findPayment(id);
        paymentRepository.delete(payment);
    }

    private Payment findPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment ID not found: " + id));
    }

    private Student findStudent(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student ID not found: " + id));
    }

}
