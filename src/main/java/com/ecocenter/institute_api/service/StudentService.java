package com.ecocenter.institute_api.service;

import com.ecocenter.institute_api.dto.student.StudentRequestDTO;
import com.ecocenter.institute_api.dto.student.StudentResponseDTO;
import com.ecocenter.institute_api.entity.Course;
import com.ecocenter.institute_api.entity.Student;
import com.ecocenter.institute_api.repository.CourseRepository;
import com.ecocenter.institute_api.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO req) {
        Course course = null;
        if (req.getCourseId() != null) {
            course = courseRepository.findById(req.getCourseId())
                    .orElseThrow(() -> new EntityNotFoundException("Course ID not found:" + req.getCourseId()));
        }

        Student student = Student.builder()
                .id(req.getId())
                .firstName(req.getFirstName())
                .middleName(req.getMiddleName())
                .lastName(req.getLastName())
                .dob(req.getDob())
                .email(req.getEmail())
                .ownPhone(req.getOwnPhone())
                .otherPhone(req.getOtherPhone())
                .address(req.getAddress())
                .course(course)
                .build();

        Student savedStudent = studentRepository.save(student);
        return StudentResponseDTO.from(savedStudent);
    }

}
