package com.institute.institute_api.service;

import com.institute.institute_api.dto.student.StudentRequestDTO;
import com.institute.institute_api.dto.student.StudentResponseDTO;
import com.institute.institute_api.entity.Course;
import com.institute.institute_api.entity.Student;
import com.institute.institute_api.exception.DuplicateNationalIdException;
import com.institute.institute_api.exception.StudentHasPaymentsException;
import com.institute.institute_api.repository.CourseRepository;
import com.institute.institute_api.repository.PaymentRepository;
import com.institute.institute_api.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO req) {
        if (studentRepository.existsByNationalId(req.getNationalId())) {
            throw new DuplicateNationalIdException(req.getNationalId());
        }

        Course course = findCourse(req.getCourseId());
        Student student = Student.builder()
                .nationalId(req.getNationalId())
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

    @Transactional(readOnly = true)
    public Page<StudentResponseDTO> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable)
                .map(StudentResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public StudentResponseDTO getStudentById(UUID id) {
        Student student = findStudent(id);
        return StudentResponseDTO.from(student);
    }

    @Transactional
    public StudentResponseDTO updateStudent(UUID id, StudentRequestDTO req) {
        Student student = findStudent(id);
        if (studentRepository.existsByNationalIdAndIdNot(req.getNationalId(), id)) {
            throw new DuplicateNationalIdException(req.getNationalId());
        }
        Course course = findCourse(req.getCourseId());

        student.setNationalId(req.getNationalId());
        student.setFirstName(req.getFirstName());
        student.setMiddleName(req.getMiddleName());
        student.setLastName(req.getLastName());
        student.setDob(req.getDob());
        student.setEmail(req.getEmail());
        student.setOwnPhone(req.getOwnPhone());
        student.setOtherPhone(req.getOtherPhone());
        student.setAddress(req.getAddress());
        student.setCourse(course);

        Student updatedStudent = studentRepository.save(student);
        return StudentResponseDTO.from(updatedStudent);
    }

    @Transactional
    public void deleteStudent(UUID id) {
        Student student = findStudent(id);
        if (paymentRepository.existsByStudentId(id)) {
            throw new StudentHasPaymentsException(student.getFirstName() + " " + student.getLastName());
        }
        studentRepository.delete(student);
    }

    private Student findStudent(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student ID not found: " + id));
    }

    private Course findCourse(UUID id) {
        if (id == null) {
            return null;
        }
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course ID not found: " + id));
    }
}
