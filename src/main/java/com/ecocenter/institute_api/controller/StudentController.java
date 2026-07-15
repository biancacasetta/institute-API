package com.ecocenter.institute_api.controller;

import com.ecocenter.institute_api.dto.student.StudentRequestDTO;
import com.ecocenter.institute_api.dto.student.StudentResponseDTO;
import com.ecocenter.institute_api.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<StudentResponseDTO> createStudent(@Valid @RequestBody StudentRequestDTO req) {
        StudentResponseDTO created = studentService.createStudent(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
