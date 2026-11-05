package com.institute.institute_api.service;

import com.institute.institute_api.config.InstituteProperties;
import com.institute.institute_api.dto.course.CourseRequestDTO;
import com.institute.institute_api.dto.course.CourseResponseDTO;
import com.institute.institute_api.dto.payment.PaymentRequestDTO;
import com.institute.institute_api.dto.payment.PaymentResponseDTO;
import com.institute.institute_api.dto.student.StudentRequestDTO;
import com.institute.institute_api.embeddable.Timetable;
import com.institute.institute_api.entity.Course;
import com.institute.institute_api.entity.Payment;
import com.institute.institute_api.entity.Student;
import com.institute.institute_api.repository.PaymentRepository;
import com.institute.institute_api.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final ZoneId ZONE = ZoneId.of("UTC");

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private InstituteProperties props;
    @InjectMocks
    private PaymentService paymentService;

    // createPayment

    @Test
    void createPayment_validRequest_savesAndReturnsDto() {
        Student student = mockStudent();
        PaymentRequestDTO req = mockPaymentRequest(STUDENT_ID, "2026-10-05", "45000.00");

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(call -> {
            Payment payment = call.getArgument(0);
            payment.setId(PAYMENT_ID);
            return payment;
        });

        PaymentResponseDTO result = paymentService.createPayment(req);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();


        assertThat(saved.getDate()).isEqualTo(LocalDate.parse("2026-10-05"));
        assertThat(saved.getAmount()).isEqualByComparingTo("45000.00");
        assertThat(saved.getStudent()).isSameAs(student);

        assertThat(result.getId()).isEqualTo(PAYMENT_ID);
        assertThat(result.getDate()).isEqualTo(LocalDate.parse("2026-10-05"));
        assertThat(result.getAmount()).isEqualByComparingTo("45000.00");
        assertThat(result.getStudentId()).isEqualTo(STUDENT_ID);
    }

    @Test
    void createPayment_studentDoesNotExist_throwsAndDoesNotSave() {
        PaymentRequestDTO req = mockPaymentRequest(STUDENT_ID, "2026-10-05", "45000.00");

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(STUDENT_ID.toString());

        verify(paymentRepository, never()).save(any());
    }

    // helpers

    private static Student mockStudent() {
        return Student.builder()
                .id(STUDENT_ID)
                .nationalId("12345678")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    private static PaymentRequestDTO mockPaymentRequest(UUID studentId, String date, String amount) {
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setStudentId(studentId);
        req.setDate(LocalDate.parse(date));
        req.setAmount(new BigDecimal(amount));
        return req;
    }

}
