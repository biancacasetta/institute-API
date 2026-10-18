package com.institute.institute_api.repository;

import com.institute.institute_api.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    @EntityGraph(attributePaths = "student")
    List<Payment> findByStudentIdAndDateBetweenOrderByDateDesc(
            UUID studentId, LocalDate from, LocalDate to
    );
}
