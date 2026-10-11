package com.phuc.studentmanagement.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.phuc.studentmanagement.exception.EnrollmentAlreadyExistsException;
import com.phuc.studentmanagement.exception.ResourceNotFoundException;
import com.phuc.studentmanagement.model.Enrollment;
import com.phuc.studentmanagement.repository.CourseRepository;
import com.phuc.studentmanagement.repository.EnrollmentRepository;
import com.phuc.studentmanagement.repository.StudentRepository;

@Service
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    public List<Enrollment> getEnrollmentsByStudentId(int studentId) {
        if (studentRepository.findById(studentId).isEmpty()) {
            throw new ResourceNotFoundException("Student", studentId);
        }

        return enrollmentRepository.findByStudentId(studentId);
    }

    public Optional<Enrollment> getEnrollmentById(int id) {
        return enrollmentRepository.findById(id);
    }

    public Enrollment addEnrollment(Enrollment enrollment) {
        int studentId = enrollment.getStudentId();
        int courseId = enrollment.getCourseId();

        if (studentRepository.findById(studentId).isEmpty()) {
            throw new ResourceNotFoundException("Student", studentId);
        }

        if (courseRepository.findById(courseId).isEmpty()) {
            throw new ResourceNotFoundException("Course", courseId);
        }

        try {
            return enrollmentRepository.insert(enrollment);
        } catch (DuplicateKeyException exception) {
            throw new EnrollmentAlreadyExistsException(studentId, courseId, exception);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause() instanceof SQLException sqlException &&
                    "23503".equals(sqlException.getSQLState())) {
                if (studentRepository.findById(studentId).isEmpty()) {
                    throw new ResourceNotFoundException("Student", studentId);
                }

                if (courseRepository.findById(courseId).isEmpty()) {
                    throw new ResourceNotFoundException("Course", courseId);
                }
            }

            throw exception;
        }
    }

    public boolean deleteEnrollment(int id) {
        return enrollmentRepository.deleteById(id);
    }
}