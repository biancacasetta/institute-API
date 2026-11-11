package com.institute.institute_api.controller;

import com.institute.institute_api.dto.payment.PaymentRequestDTO;
import com.institute.institute_api.dto.payment.PaymentResponseDTO;
import com.institute.institute_api.dto.student.StudentRequestDTO;
import com.institute.institute_api.exception.BackToTheFutureException;
import com.institute.institute_api.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {
    private static final String BASE_URL = "/api/payments";
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    private static final String BODY_CONTENT = """
            {
              "studentId": "%s",
              "date": "2026-10-05",
              "amount": 45000.00
            }
            """.formatted(STUDENT_ID);

    @Autowired
    private MockMvcTester mvcTester;
    @MockitoBean
    private PaymentService paymentService;

    // POST

    @Test
    void createPayment_validRequest_returns201AndBody() {
        when(paymentService.createPayment(any(PaymentRequestDTO.class))).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(PAYMENT_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.amount").asNumber().isEqualTo(45000.00);
        assertThat(result).bodyJson().extractingPath("$.date").isEqualTo("2026-10-05");
        assertThat(result).bodyJson().extractingPath("$.studentId").isEqualTo(STUDENT_ID.toString());
    }

    @Test
    void createPayment_validRequest_passesParsedJsonToService() {
        when(paymentService.createPayment(any(PaymentRequestDTO.class))).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);

        ArgumentCaptor<PaymentRequestDTO> captor = ArgumentCaptor.forClass(PaymentRequestDTO.class);
        verify(paymentService).createPayment(captor.capture());
        PaymentRequestDTO req = captor.getValue();

        assertThat(req.getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(req.getDate()).isEqualTo(LocalDate.parse("2026-10-05"));
        assertThat(req.getAmount()).isEqualByComparingTo("45000.00");
    }

    @Test
    void createPayment_missingStudentId_returns400WithFieldError() {
        String json = """
            { "date": "2026-10-05", "amount": 45000.00 }
            """;

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.studentId").isNotNull();
        verifyNoInteractions(paymentService);
    }

    @Test
    void createPayment_missingDate_returns400WithFieldError() {
        String json = """
                { "studentId": "%s", "amount": 45000.00 }
                """.formatted(STUDENT_ID);

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.date").isNotNull();
        verifyNoInteractions(paymentService);
    }

    @Test
    void createPayment_missingAmount_returns400WithFieldError() {
        String json = """
                { "studentId": "%s", "date": "2026-10-05" }
                """.formatted(STUDENT_ID);

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.amount").isNotNull();
        verifyNoInteractions(paymentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1000.00", "12345678901.00", "10000.123456"})
    void createPayment_invalidAmount_returns400WithFieldError(String amount) {
        String json = BODY_CONTENT.replace("45000.00", amount);

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(paymentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10-03-2005", "2005-03-35", "2005-15-10", "not-a-date"})
    void createPayment_malformedDate_returns400(String date) {
        String json = BODY_CONTENT.replace("2026-10-05", date);

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(paymentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"298f8a6b-3e-1c4-4bba-9488-8f4b233e63e7", // wrong segment length
            "15ee-168da--20c6-4c1f0-9a2b--2806bf323e706", // too many hyphens
            "123e4567-e89b-12d3-a456", // too short
            "not-a-real-uuid-string-000000000000", // not hexadecimal
            "123e4567e89b12d3a456426614174000"}) // no hyphens})
    void createPayment_malformedStudentId_returns400(String studentId) {
        String json = BODY_CONTENT.replace(STUDENT_ID.toString(), studentId);

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(paymentService);
    }

    @Test
    void createPayment_studentDoesNotExist_returns404() {
        when(paymentService.createPayment(any(PaymentRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("Student ID not found: " + STUDENT_ID));

        MvcTestResult result = mvcTester.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("Student ID not found: " + STUDENT_ID);
    }

    // GET

    @Test
    void getAllPaymentsByStudent_returns200AndDtoList() {
        when(paymentService.getAllPaymentsByStudent(eq(STUDENT_ID), any(), any()))
                .thenReturn(List.of(mockResponse()));

        MvcTestResult result = mvcTester.get().uri(BASE_URL)
                .queryParam("studentId", STUDENT_ID.toString()).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$").asArray().hasSize(1);
        assertThat(result).bodyJson().extractingPath("$[0].id").isEqualTo(PAYMENT_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.[0].studentId").isEqualTo(STUDENT_ID.toString());
    }

    @Test
    void getAllPaymentsByStudent_withoutDates_passesNullToService() {
        when(paymentService.getAllPaymentsByStudent(eq(STUDENT_ID), any(), any()))
                .thenReturn(List.of());

        MvcTestResult result = mvcTester.get().uri(BASE_URL)
                .queryParam("studentId", STUDENT_ID.toString()).exchange();

        assertThat(result).hasStatusOk();
        verify(paymentService).getAllPaymentsByStudent(STUDENT_ID, null, null);
    }

    @Test
    void getAllPaymentsByStudent_withDates_passesParsedDatesToService() {
        when(paymentService.getAllPaymentsByStudent(eq(STUDENT_ID), any(), any()))
                .thenReturn(List.of());

        MvcTestResult result = mvcTester.get().uri(BASE_URL)
                .queryParam("studentId", STUDENT_ID.toString())
                .queryParam("from", "2026-10-01")
                .queryParam("to", "2026-10-31")
                .exchange();

        assertThat(result).hasStatusOk();
        verify(paymentService).getAllPaymentsByStudent(
                STUDENT_ID,
                LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-31"));
    }

    @Test
    void getAllPaymentsByStudent_missingStudentId_returns400() {
        MvcTestResult result = mvcTester.get().uri(BASE_URL).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(paymentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"298f8a6b-3e-1c4-4bba-9488-8f4b233e63e7", // wrong segment length
            "15ee-168da--20c6-4c1f0-9a2b--2806bf323e706", // too many hyphens
            "123e4567-e89b-12d3-a456", // too short
            "not-a-real-uuid-string-000000000000", // not hexadecimal
            "123e4567e89b12d3a456426614174000"}) // no hyphens
    void getAllPaymentsByStudent_malformedStudentId_returns400(String studentId) {
        MvcTestResult result = mvcTester.get().uri(BASE_URL)
                .queryParam("studentId", studentId)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(paymentService);

    }

    @ParameterizedTest
    @ValueSource(strings = {"10-03-2005", "2005-03-35", "2005-15-10", "not-a-date"})
    void getAllPaymentsByStudent_malformedFormDate_returns400(String fromDate) {
        MvcTestResult result = mvcTester.get().uri(BASE_URL)
                .queryParam("studentId", STUDENT_ID.toString())
                .queryParam("from", fromDate)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(paymentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10-03-2005", "2005-03-35", "2005-15-10", "not-a-date"})
    void getAllPaymentsByStudent_malformedToDate_returns400(String toDate) {
        MvcTestResult result = mvcTester.get().uri(BASE_URL)
                .queryParam("studentId", STUDENT_ID.toString())
                .queryParam("from", toDate)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(paymentService);
    }

    @Test
    void getAllPaymentsByStudent_studentDoesNotExist_returns404() {
        when(paymentService.getAllPaymentsByStudent(eq(STUDENT_ID), any(), any()))
                .thenThrow(new EntityNotFoundException("Student ID not found: " + STUDENT_ID));

        MvcTestResult result = mvcTester.get().uri(BASE_URL)
                .queryParam("studentId", STUDENT_ID.toString())
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.detail")
                        .isEqualTo("Student ID not found: " + STUDENT_ID);
    }

    @Test
    void getAllPaymentsByStudent_fromAfterTo_returns400() {
        LocalDate from = LocalDate.of(2026, 10, 31);
        LocalDate to = LocalDate.of(2026, 10, 1);

        when(paymentService.getAllPaymentsByStudent(STUDENT_ID, from, to))
                .thenThrow(new BackToTheFutureException(from, to));

        MvcTestResult result = mvcTester.get().uri(BASE_URL)
                .queryParam("studentId", STUDENT_ID.toString())
                .queryParam("from", "2026-10-31")
                .queryParam("to", "2026-10-01")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("'from' date (2026-10-31) must not be after 'to' date (2026-10-01).");
    }

    @Test
    void getPaymentById_exists_returns200() {
        when(paymentService.getPaymentById(PAYMENT_ID)).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.get()
                .uri(BASE_URL + "/{id}", PAYMENT_ID).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(PAYMENT_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.amount").asNumber().isEqualTo(45000.00);
    }

    @Test
    void getPaymentById_doesNotExist_returns404() {
        when(paymentService.getPaymentById(PAYMENT_ID))
                .thenThrow(new EntityNotFoundException("Payment ID not found: " + PAYMENT_ID));

        MvcTestResult result = mvcTester.get()
                .uri(BASE_URL + "/{id}", PAYMENT_ID).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("Payment ID not found: " + PAYMENT_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"298f8a6b-3e-1c4-4bba-9488-8f4b233e63e7", // wrong segment length
            "15ee-168da--20c6-4c1f0-9a2b--2806bf323e706", // too many hyphens
            "123e4567-e89b-12d3-a456", // too short
            "not-a-real-uuid-string-000000000000", // not hexadecimal
            "123e4567e89b12d3a456426614174000"}) // no hyphens
    void getPaymentById_malformedUuid_returns400(String uuid) {
        MvcTestResult result = mvcTester.get()
                .uri(BASE_URL + "/{id}", uuid).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(paymentService);
    }

    // PUT

    @Test
    void updatePayment_validRequest_returns200AndUsesPathId() {
        when(paymentService.updatePayment(
                eq(PAYMENT_ID), any(PaymentRequestDTO.class))).thenReturn(mockResponse());

        MvcTestResult result = mvcTester.put().uri(BASE_URL + "/{id}", PAYMENT_ID)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(PAYMENT_ID.toString());
        verify(paymentService).updatePayment(eq(PAYMENT_ID), any(PaymentRequestDTO.class));
    }

    @Test
    void updatePayment_invalidBody_returns400() {
        String json = BODY_CONTENT.replace("45000.00", "-5000.00");

        MvcTestResult result = mvcTester.put().uri(BASE_URL + "/{id}", PAYMENT_ID)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.amount").isNotNull();
        verifyNoInteractions(paymentService);
    }

    @Test
    void updatePayment_doesNotExist_returns404() {
        when(paymentService.updatePayment(
                eq(PAYMENT_ID), any(PaymentRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("Payment ID not found: " + PAYMENT_ID));

        MvcTestResult result = mvcTester.put().uri(BASE_URL + "/{id}", PAYMENT_ID)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void updatePayment_studentDoesNotExist_returns404() {
        when(paymentService.updatePayment(
                eq(PAYMENT_ID), any(PaymentRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("Student ID not found: " + STUDENT_ID));

        MvcTestResult result = mvcTester.put().uri(BASE_URL + "/{id}", PAYMENT_ID)
                .contentType(MediaType.APPLICATION_JSON).content(BODY_CONTENT).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("Student ID not found: " + STUDENT_ID);
    }

    // DELETE

    @Test
    void deletePayment_exists_returns204() {
        MvcTestResult result = mvcTester.delete()
                .uri(BASE_URL + "/{id}", PAYMENT_ID).exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(paymentService).deletePayment(PAYMENT_ID);
    }

    @Test
    void deletePayment_doesNotExist_returns404() {
        doThrow(new EntityNotFoundException("Payment ID not found: " + PAYMENT_ID))
                .when(paymentService).deletePayment(PAYMENT_ID);

        MvcTestResult result = mvcTester.delete()
                .uri(BASE_URL + "/{id}", PAYMENT_ID).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    // helpers

    private static PaymentResponseDTO mockResponse() {
        return PaymentResponseDTO.builder()
                .id(PAYMENT_ID)
                .date(LocalDate.of(2026,10, 5))
                .amount(BigDecimal.valueOf(45000.00))
                .studentId(STUDENT_ID)
                .build();
    }

}
