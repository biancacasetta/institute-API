package com.institute.institute_api.repository;

import com.institute.institute_api.TestcontainersConfig;
import com.institute.institute_api.embeddable.Address;
import com.institute.institute_api.entity.Payment;
import com.institute.institute_api.entity.Student;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class PaymentRepositoryTest {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    // findByStudentIdAndDateBetweenOrderByDateDesc

    @Test
    void findByStudentIdAndDateBetween_returnsOnlyPaymentsInRangeNewestFirst() {
        Student student = testEntityManager.persistAndFlush(mockStudent("12345678"));
        testEntityManager.persistAndFlush(mockPayment(student, "2026-08-01", "10000.00"));
        testEntityManager.persistAndFlush(mockPayment(student, "2026-09-01", "15000.00"));
        testEntityManager.persistAndFlush(mockPayment(student, "2026-10-01", "20000.00"));
        testEntityManager.persistAndFlush(mockPayment(student, "2026-11-01", "12000.00"));

        List<Payment> result = paymentRepository.findByStudentIdAndDateBetweenOrderByDateDesc(
                student.getId(), LocalDate.parse("2026-09-30"), LocalDate.parse("2026-11-30")
        );

        assertThat(result).extracting(Payment::getDate)
                .containsExactly(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-10-01"));
    }

    @Test
    void findByStudentIdAndDateBetween_boundsAreInclusive() {
        Student student = testEntityManager.persistAndFlush(mockStudent("12345678"));
        testEntityManager.persistAndFlush(mockPayment(student, "2026-08-01", "10000.00"));
        testEntityManager.persistAndFlush(mockPayment(student, "2026-09-01", "15000.00"));

        List<Payment> result = paymentRepository.findByStudentIdAndDateBetweenOrderByDateDesc(
                student.getId(), LocalDate.parse("2026-08-01"), LocalDate.parse("2026-09-01")
        );

        assertThat(result).hasSize(2);
    }

    @Test
    void findByStudentIdAndDateBetween_excludesOtherStudentsPayments() {
        Student student = testEntityManager.persistAndFlush(mockStudent("12345678"));
        Student otherStudent = testEntityManager.persistAndFlush(mockStudent("87654321"));
        testEntityManager.persistAndFlush(mockPayment(student, "2026-10-01", "10000.00"));
        testEntityManager.persistAndFlush(mockPayment(otherStudent, "2026-10-01", "20000.00"));

        List<Payment> result = paymentRepository.findByStudentIdAndDateBetweenOrderByDateDesc(
                student.getId(), LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAmount()).isEqualByComparingTo("10000.00");
    }

    @Test
    void findByStudentIdAndDateBetween_noPaymentsInRange_returnsEmptyList() {
        Student student = testEntityManager.persistAndFlush(mockStudent("12345678"));
        testEntityManager.persistAndFlush(mockPayment(student, "2026-11-15", "10000.00"));

        List<Payment> result = paymentRepository.findByStudentIdAndDateBetweenOrderByDateDesc(
                student.getId(), LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));

        assertThat(result).isEmpty();
    }

    @Test
    void findByStudentIdAndDateBetween_fetchesStudentInSameQuery() {
        Student student = testEntityManager.persistAndFlush(mockStudent("12345678"));
        testEntityManager.persistAndFlush(mockPayment(student, "2026-10-15", "10000.00"));
        testEntityManager.clear();

        List<Payment> result = paymentRepository.findByStudentIdAndDateBetweenOrderByDateDesc(
                student.getId(), LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31")
        );

        assertThat(Hibernate.isInitialized(result.getFirst().getStudent())).isTrue();
    }

    // existsByStudentId

    @Test
    void existsByStudentId_withPayments_returnsTrue() {
        Student student = testEntityManager.persistAndFlush(mockStudent("12345678"));
        testEntityManager.persistAndFlush(mockPayment(student, "2026-10-05", "10000.00"));

        assertThat(paymentRepository.existsByStudentId(student.getId())).isTrue();
    }

    @Test
    void existsByStudentId_withoutPayments_returnsFalse() {
        Student student = testEntityManager.persistAndFlush(mockStudent("12345678"));
        Student other = testEntityManager.persistAndFlush(mockStudent("87654321"));
        testEntityManager.persistAndFlush(mockPayment(other, "2026-10-05", "10000.00"));

        assertThat(paymentRepository.existsByStudentId(student.getId())).isFalse();
    }

    // helpers

    private static Student mockStudent(String nationalId) {
        return Student.builder()
                .nationalId(nationalId)
                .firstName("John")
                .lastName("Doe")
                .dob(LocalDate.of(2000, 12, 31))
                .email("john@example.com")
                .ownPhone("1122223333")
                .address(Address.builder()
                        .street("Main Street")
                        .number(1234)
                        .city("Buenos Aires")
                        .zip(4321)
                        .country("Argentina")
                        .build())
                .build();
    }

    private static Payment mockPayment(Student student, String date, String amount) {
        return Payment.builder()
                .date(LocalDate.parse(date))
                .amount(new BigDecimal(amount))
                .student(student)
                .build();
    }
}
