
package com.phuc.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import com.phuc.studentmanagement.exception.EnrollmentAlreadyExistsException;
import com.phuc.studentmanagement.exception.ResourceNotFoundException;
import com.phuc.studentmanagement.model.Course;
import com.phuc.studentmanagement.model.Enrollment;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.repository.CourseRepository;
import com.phuc.studentmanagement.repository.EnrollmentRepository;
import com.phuc.studentmanagement.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void getAllEnrollments_shouldReturnAllEnrollments() {
        Enrollment enrollment1 = new Enrollment(1, 1, 2);
        Enrollment enrollment2 = new Enrollment(2, 2, 3);

        List<Enrollment> expectedEnrollments = List.of(enrollment1, enrollment2);

        when(enrollmentRepository.findAll()).thenReturn(expectedEnrollments);

        List<Enrollment> result = enrollmentService.getAllEnrollments();

        assertEquals(expectedEnrollments, result);
        verify(enrollmentRepository).findAll();
    }

    @Test
    void getEnrollmentById_shouldReturnEnrollment_whenEnrollmentExists() {
        Enrollment enrollment = new Enrollment(1, 1, 2);

        when(enrollmentRepository.findById(1))
                .thenReturn(Optional.of(enrollment));

        Optional<Enrollment> result = enrollmentService.getEnrollmentById(1);

        assertTrue(result.isPresent());
        assertSame(enrollment, result.get());

        verify(enrollmentRepository).findById(1);
    }

    @Test
    void getEnrollmentById_shouldReturnEmpty_whenEnrollmentDoesNotExist() {
        when(enrollmentRepository.findById(999))
                .thenReturn(Optional.empty());

        Optional<Enrollment> result = enrollmentService.getEnrollmentById(999);

        assertTrue(result.isEmpty());
        verify(enrollmentRepository).findById(999);
    }

    @Test
    void getEnrollmentsByStudentId_shouldReturnEnrollments_whenStudentExists() {
        Student student = new Student(1, "24120001", "Phuc", 20);

        List<Enrollment> expectedEnrollments = List.of(
                new Enrollment(1, 1, 2),
                new Enrollment(2, 1, 3));

        when(studentRepository.findById(1))
                .thenReturn(Optional.of(student));

        when(enrollmentRepository.findByStudentId(1))
                .thenReturn(expectedEnrollments);

        List<Enrollment> result = enrollmentService.getEnrollmentsByStudentId(1);

        assertEquals(expectedEnrollments, result);

        verify(studentRepository).findById(1);
        verify(enrollmentRepository).findByStudentId(1);
    }

    @Test
    void getEnrollmentsByStudentId_shouldReturnEmptyList_whenStudentHasNoEnrollments() {
        Student student = new Student(1, "24120001", "Phuc", 20);

        when(studentRepository.findById(1))
                .thenReturn(Optional.of(student));

        when(enrollmentRepository.findByStudentId(1))
                .thenReturn(List.of());

        List<Enrollment> result = enrollmentService.getEnrollmentsByStudentId(1);

        assertTrue(result.isEmpty());

        verify(studentRepository).findById(1);
        verify(enrollmentRepository).findByStudentId(1);
    }

    @Test
    void getEnrollmentsByStudentId_shouldThrowResourceNotFoundException_whenStudentDoesNotExist() {
        when(studentRepository.findById(999))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> enrollmentService.getEnrollmentsByStudentId(999));

        assertEquals("Student with id 999 not found", exception.getMessage());

        verify(studentRepository).findById(999);
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void addEnrollment_shouldReturnCreatedEnrollment_whenRequestIsValid() {
        Student student = new Student(1, "24120001", "Phuc", 20);
        Course course = new Course(2, "CS102", "Data Structures", 4);

        Enrollment enrollment = new Enrollment(1, 2);
        Enrollment createdEnrollment = new Enrollment(13, 1, 2);

        when(studentRepository.findById(1))
                .thenReturn(Optional.of(student));

        when(courseRepository.findById(2))
                .thenReturn(Optional.of(course));

        when(enrollmentRepository.insert(enrollment))
                .thenReturn(createdEnrollment);

        Enrollment result = enrollmentService.addEnrollment(enrollment);

        assertSame(createdEnrollment, result);

        verify(studentRepository).findById(1);
        verify(courseRepository).findById(2);
        verify(enrollmentRepository).insert(enrollment);
    }

    @Test
    void addEnrollment_shouldThrowResourceNotFoundException_whenStudentDoesNotExist() {
        Enrollment enrollment = new Enrollment(999, 2);

        when(studentRepository.findById(999))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> enrollmentService.addEnrollment(enrollment));

        assertEquals("Student with id 999 not found", exception.getMessage());

        verify(studentRepository).findById(999);
        verifyNoInteractions(courseRepository, enrollmentRepository);
    }

    @Test
    void addEnrollment_shouldThrowResourceNotFoundException_whenCourseDoesNotExist() {
        Student student = new Student(1, "24120001", "Phuc", 20);
        Enrollment enrollment = new Enrollment(1, 999);

        when(studentRepository.findById(1))
                .thenReturn(Optional.of(student));

        when(courseRepository.findById(999))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> enrollmentService.addEnrollment(enrollment));

        assertEquals("Course with id 999 not found", exception.getMessage());

        verify(studentRepository).findById(1);
        verify(courseRepository).findById(999);
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void addEnrollment_shouldThrowEnrollmentAlreadyExistsException_whenEnrollmentIsDuplicate() {
        Student student = new Student(1, "24120001", "Phuc", 20);
        Course course = new Course(2, "CS102", "Data Structures", 4);
        Enrollment enrollment = new Enrollment(1, 2);

        DuplicateKeyException expectedCause = new DuplicateKeyException("Duplicate enrollment");

        when(studentRepository.findById(1))
                .thenReturn(Optional.of(student));

        when(courseRepository.findById(2))
                .thenReturn(Optional.of(course));

        when(enrollmentRepository.insert(enrollment))
                .thenThrow(expectedCause);

        EnrollmentAlreadyExistsException exception = assertThrows(
                EnrollmentAlreadyExistsException.class,
                () -> enrollmentService.addEnrollment(enrollment));

        assertEquals(
                "Student with id 1 is already enrolled in course with id 2",
                exception.getMessage());

        assertSame(expectedCause, exception.getCause());

        verify(enrollmentRepository).insert(enrollment);
    }

    @Test
    void addEnrollment_shouldThrowResourceNotFoundException_whenStudentIsDeletedBeforeInsert() {
        Student student = new Student(1, "24120001", "Phuc", 20);
        Course course = new Course(2, "CS102", "Data Structures", 4);
        Enrollment enrollment = new Enrollment(1, 2);

        SQLException sqlException = new SQLException("Foreign key violation", "23503");

        DataIntegrityViolationException databaseException = new DataIntegrityViolationException(
                "Cannot insert enrollment",
                sqlException);

        when(studentRepository.findById(1))
                .thenReturn(Optional.of(student))
                .thenReturn(Optional.empty());

        when(courseRepository.findById(2))
                .thenReturn(Optional.of(course));

        when(enrollmentRepository.insert(enrollment))
                .thenThrow(databaseException);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> enrollmentService.addEnrollment(enrollment));

        assertEquals("Student with id 1 not found", exception.getMessage());

        verify(studentRepository, times(2)).findById(1);
        verify(courseRepository).findById(2);
        verify(enrollmentRepository).insert(enrollment);
    }

    @Test
    void addEnrollment_shouldThrowResourceNotFoundException_whenCourseIsDeletedBeforeInsert() {
        Student student = new Student(1, "24120001", "Phuc", 20);
        Course course = new Course(2, "CS102", "Data Structures", 4);
        Enrollment enrollment = new Enrollment(1, 2);

        SQLException sqlException = new SQLException("Foreign key violation", "23503");

        DataIntegrityViolationException databaseException = new DataIntegrityViolationException(
                "Cannot insert enrollment",
                sqlException);

        when(studentRepository.findById(1))
                .thenReturn(Optional.of(student));

        when(courseRepository.findById(2))
                .thenReturn(Optional.of(course))
                .thenReturn(Optional.empty());

        when(enrollmentRepository.insert(enrollment))
                .thenThrow(databaseException);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> enrollmentService.addEnrollment(enrollment));

        assertEquals("Course with id 2 not found", exception.getMessage());

        verify(studentRepository, times(2)).findById(1);
        verify(courseRepository, times(2)).findById(2);
        verify(enrollmentRepository).insert(enrollment);
    }

    @Test
    void addEnrollment_shouldRethrowDataIntegrityViolationException_whenForeignKeyErrorCannotBeResolved() {
        Student student = new Student(1, "24120001", "Phuc", 20);
        Course course = new Course(2, "CS102", "Data Structures", 4);
        Enrollment enrollment = new Enrollment(1, 2);

        SQLException sqlException = new SQLException("Foreign key violation", "23503");

        DataIntegrityViolationException expectedException = new DataIntegrityViolationException(
                "Cannot insert enrollment",
                sqlException);

        when(studentRepository.findById(1))
                .thenReturn(Optional.of(student));

        when(courseRepository.findById(2))
                .thenReturn(Optional.of(course));

        when(enrollmentRepository.insert(enrollment))
                .thenThrow(expectedException);

        DataIntegrityViolationException actualException = assertThrows(
                DataIntegrityViolationException.class,
                () -> enrollmentService.addEnrollment(enrollment));

        assertSame(expectedException, actualException);

        verify(studentRepository, times(2)).findById(1);
        verify(courseRepository, times(2)).findById(2);
        verify(enrollmentRepository).insert(enrollment);
    }

    @Test
    void addEnrollment_shouldRethrowDataIntegrityViolationException_whenErrorIsNotForeignKey() {
        Student student = new Student(1, "24120001", "Phuc", 20);
        Course course = new Course(2, "CS102", "Data Structures", 4);
        Enrollment enrollment = new Enrollment(1, 2);

        SQLException sqlException = new SQLException("Check constraint violation", "23514");

        DataIntegrityViolationException expectedException = new DataIntegrityViolationException(
                "Database constraint violation",
                sqlException);

        when(studentRepository.findById(1))
                .thenReturn(Optional.of(student));

        when(courseRepository.findById(2))
                .thenReturn(Optional.of(course));

        when(enrollmentRepository.insert(enrollment))
                .thenThrow(expectedException);

        DataIntegrityViolationException actualException = assertThrows(
                DataIntegrityViolationException.class,
                () -> enrollmentService.addEnrollment(enrollment));

        assertSame(expectedException, actualException);

        verify(studentRepository).findById(1);
        verify(courseRepository).findById(2);
        verify(enrollmentRepository).insert(enrollment);
    }

    @Test
    void deleteEnrollment_shouldReturnTrue_whenEnrollmentExists() {
        when(enrollmentRepository.deleteById(1)).thenReturn(true);

        boolean result = enrollmentService.deleteEnrollment(1);

        assertTrue(result);
        verify(enrollmentRepository).deleteById(1);
    }

    @Test
    void deleteEnrollment_shouldReturnFalse_whenEnrollmentDoesNotExist() {
        when(enrollmentRepository.deleteById(999)).thenReturn(false);

        boolean result = enrollmentService.deleteEnrollment(999);

        assertFalse(result);
        verify(enrollmentRepository).deleteById(999);
    }
}