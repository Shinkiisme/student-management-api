
package com.phuc.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
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

import com.phuc.studentmanagement.exception.ResourceInUseException;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void getAllStudents_shouldReturnAllStudents_whenAgeIsNull() {
        Student student1 = new Student(1, "Phuc", 20);
        Student student2 = new Student(2, "Huy", 19);

        List<Student> expectedStudents = List.of(student1, student2);

        when(studentRepository.findAll()).thenReturn(expectedStudents);

        List<Student> actualStudents = studentService.getAllStudents(null);

        assertEquals(expectedStudents, actualStudents);

        verify(studentRepository).findAll();
    }

    @Test
    void getAllStudents_shouldFilterByAge_whenAgeIsProvided() {
        Student student1 = new Student(1, "Phuc", 20);
        Student student2 = new Student(3, "Nam", 20);

        List<Student> expectedStudents = List.of(student1, student2);

        when(studentRepository.findByAge(20)).thenReturn(expectedStudents);

        List<Student> actualStudents = studentService.getAllStudents(20);

        assertEquals(expectedStudents, actualStudents);

        verify(studentRepository).findByAge(20);
    }

    @Test
    void getStudentById_shouldReturnStudent_whenStudentExists() {
        Student student = new Student(1, "Phuc", 20);

        when(studentRepository.findById(1))
                .thenReturn(Optional.of(student));

        Optional<Student> result = studentService.getStudentById(1);

        assertTrue(result.isPresent());
        assertSame(student, result.get());

        verify(studentRepository).findById(1);
    }

    @Test
    void getStudentById_shouldReturnEmpty_whenStudentDoesNotExist() {
        when(studentRepository.findById(999))
                .thenReturn(Optional.empty());

        Optional<Student> result = studentService.getStudentById(999);

        assertTrue(result.isEmpty());

        verify(studentRepository).findById(999);
    }

    @Test
    void addStudent_shouldReturnCreatedStudent() {
        Student student = new Student("Phuc", 20);
        Student createdStudent = new Student(1, "Phuc", 20);

        when(studentRepository.insert(student))
                .thenReturn(createdStudent);

        Student result = studentService.addStudent(student);

        assertSame(createdStudent, result);

        verify(studentRepository).insert(student);
    }

    @Test
    void updateStudent_shouldReturnUpdatedStudent_whenStudentExists() {
        Student student = new Student("Huy Nguyen", 22);
        Student updatedStudent = new Student(2, "Huy Nguyen", 22);

        when(studentRepository.update(2, student))
                .thenReturn(Optional.of(updatedStudent));

        Optional<Student> result = studentService.updateStudent(2, student);

        assertTrue(result.isPresent());
        assertSame(updatedStudent, result.get());

        verify(studentRepository).update(2, student);
    }

    @Test
    void updateStudent_shouldReturnEmpty_whenStudentDoesNotExist() {
        Student student = new Student("Nam", 22);

        when(studentRepository.update(999, student))
                .thenReturn(Optional.empty());

        Optional<Student> result = studentService.updateStudent(999, student);

        assertTrue(result.isEmpty());

        verify(studentRepository).update(999, student);
    }

    @Test
    void deleteStudent_shouldReturnTrue_whenStudentExists() {
        when(studentRepository.deleteById(1)).thenReturn(true);

        boolean result = studentService.deleteStudent(1);

        assertTrue(result);

        verify(studentRepository).deleteById(1);
    }

    @Test
    void deleteStudent_shouldReturnFalse_whenStudentDoesNotExist() {
        when(studentRepository.deleteById(999)).thenReturn(false);

        boolean result = studentService.deleteStudent(999);

        assertFalse(result);

        verify(studentRepository).deleteById(999);
    }

    @Test
    void deleteStudent_shouldThrowResourceInUseException_whenStudentIsReferenced() {
        SQLException sqlException = new SQLException("Foreign key violation", "23503");

        DataIntegrityViolationException expectedCause = new DataIntegrityViolationException(
                "Cannot delete student",
                sqlException);

        when(studentRepository.deleteById(1))
                .thenThrow(expectedCause);

        ResourceInUseException exception = assertThrows(
                ResourceInUseException.class,
                () -> studentService.deleteStudent(1));

        assertEquals(
                "Student with id 1 cannot be deleted because it is in use",
                exception.getMessage());

        assertSame(expectedCause, exception.getCause());

        verify(studentRepository).deleteById(1);
    }

    @Test
    void deleteStudent_shouldRethrowDataIntegrityViolationException_whenErrorIsNotForeignKey() {
        SQLException sqlException = new SQLException("Check constraint violation", "23514");

        DataIntegrityViolationException expectedException = new DataIntegrityViolationException(
                "Database constraint violation",
                sqlException);

        when(studentRepository.deleteById(1))
                .thenThrow(expectedException);

        DataIntegrityViolationException actualException = assertThrows(
                DataIntegrityViolationException.class,
                () -> studentService.deleteStudent(1));

        assertSame(expectedException, actualException);

        verify(studentRepository).deleteById(1);
    }
}