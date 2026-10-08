package com.institute.institute_api.service;

import com.institute.institute_api.dto.student.StudentRequestDTO;
import com.institute.institute_api.dto.student.StudentResponseDTO;
import com.institute.institute_api.entity.Course;
import com.institute.institute_api.entity.Student;
import com.institute.institute_api.repository.CourseRepository;
import com.institute.institute_api.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Transactional
    public List<StudentResponseDTO> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(StudentResponseDTO::from)
                .toList();
    }

    @Transactional
    public StudentResponseDTO getStudentById(Integer id) {
        Student student = studentRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Student ID not found:" + id));

        return StudentResponseDTO.from(student);
    }

    @Transactional
    public StudentResponseDTO updateStudent(StudentRequestDTO req) {
        Student student = studentRepository.findById(req.getId())
                .orElseThrow(() -> new EntityNotFoundException("Student ID not found:" + req.getId()));

        Course course = null;
        if (req.getCourseId() != null) {
            course = courseRepository.findById(req.getCourseId())
                    .orElseThrow(() -> new EntityNotFoundException("Course ID not found:" + req.getCourseId()));
        }

        student.setId(req.getId());
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
    public void deleteStudent(Integer id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student ID not found:" + id));
        
        studentRepository.deleteById(id);
    }


}
