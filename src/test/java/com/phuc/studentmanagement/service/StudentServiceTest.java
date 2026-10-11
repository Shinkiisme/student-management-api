
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
import org.springframework.dao.DuplicateKeyException;

import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.exception.ResourceInUseException;
import com.phuc.studentmanagement.exception.StudentCodeAlreadyExistsException;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;
import com.phuc.studentmanagement.pagination.SortDirection;
import com.phuc.studentmanagement.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void getAllStudents_shouldReturnAllStudents_whenAgeIsNull() {
        Student student1 = new Student(1, "24120001", "Phuc", 20);
        Student student2 = new Student(2, "24120002", "Huy", 19);

        List<Student> expectedStudents = List.of(student1, student2);

        when(studentRepository.findAll()).thenReturn(expectedStudents);

        List<Student> result = studentService.getAllStudents(null);

        assertEquals(expectedStudents, result);
        verify(studentRepository).findAll();
    }

    @Test
    void getAllStudents_shouldFilterByAge_whenAgeIsProvided() {
        Student student1 = new Student(1, "24120001", "Phuc", 20);
        Student student2 = new Student(3, "24120003", "Nam", 20);

        List<Student> expectedStudents = List.of(student1, student2);

        when(studentRepository.findByAge(20)).thenReturn(expectedStudents);

        List<Student> result = studentService.getAllStudents(20);

        assertEquals(expectedStudents, result);
        verify(studentRepository).findByAge(20);
    }

    @Test
    void getStudentById_shouldReturnStudent_whenStudentExists() {
        Student student = new Student(1, "24120001", "Phuc", 20);

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
    void getStudentsPage_shouldReturnPage_whenAgeIsNull() {
        Student student1 = new Student(1, "24120001", "Phuc", 20);
        Student student2 = new Student(2, "24120002", "Huy", 19);

        PageRequest request = new PageRequest(
                0, 2, "id", SortDirection.ASC);

        PageResult<Student> expectedPage = new PageResult<>(
                List.of(student1, student2),
                0,
                2,
                5);

        when(studentRepository.findPage(request, null))
                .thenReturn(expectedPage);

        PageResult<Student> result = studentService.getStudentsPage(request, null);

        assertSame(expectedPage, result);
        assertEquals(2, result.content().size());
        assertEquals(0, result.page());
        assertEquals(2, result.size());
        assertEquals(5, result.totalElements());
        assertEquals(3, result.totalPages());

        verify(studentRepository).findPage(request, null);
    }

    @Test
    void getStudentsPage_shouldReturnFilteredPage_whenAgeIsProvided() {
        Student student1 = new Student(1, "24120001", "Phuc", 20);
        Student student2 = new Student(3, "24120003", "Nam", 20);

        PageRequest request = new PageRequest(
                0, 2, "name", SortDirection.ASC);

        PageResult<Student> expectedPage = new PageResult<>(
                List.of(student2, student1),
                0,
                2,
                2);

        when(studentRepository.findPage(request, 20))
                .thenReturn(expectedPage);

        PageResult<Student> result = studentService.getStudentsPage(request, 20);

        assertSame(expectedPage, result);
        assertEquals(2, result.content().size());
        assertTrue(result.content().stream()
                .allMatch(student -> student.getAge() == 20));
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());

        verify(studentRepository).findPage(request, 20);
    }

    @Test
    void getStudentsPage_shouldReturnEmptyPage_whenNoStudentsExist() {
        PageRequest request = new PageRequest(
                0, 10, "id", SortDirection.ASC);

        PageResult<Student> expectedPage = new PageResult<>(
                List.of(),
                0,
                10,
                0);

        when(studentRepository.findPage(request, null))
                .thenReturn(expectedPage);

        PageResult<Student> result = studentService.getStudentsPage(request, null);

        assertSame(expectedPage, result);
        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());

        verify(studentRepository).findPage(request, null);
    }

    @Test
    void getStudentsPage_shouldPropagateInvalidPaginationException_whenSortFieldIsInvalid() {
        PageRequest request = new PageRequest(
                0, 10, "invalid", SortDirection.ASC);

        InvalidPaginationException expectedException = new InvalidPaginationException(
                "Invalid sort field: invalid");

        when(studentRepository.findPage(request, null))
                .thenThrow(expectedException);

        InvalidPaginationException actualException = assertThrows(
                InvalidPaginationException.class,
                () -> studentService.getStudentsPage(request, null));

        assertSame(expectedException, actualException);
        assertEquals(
                "Invalid sort field: invalid",
                actualException.getMessage());

        verify(studentRepository).findPage(request, null);
    }

    @Test
    void addStudent_shouldReturnCreatedStudent_whenCodeIsUnique() {
        Student student = new Student("24120001", "Phuc", 20);
        Student createdStudent = new Student(1, "24120001", "Phuc", 20);

        when(studentRepository.insert(student)).thenReturn(createdStudent);

        Student result = studentService.addStudent(student);

        assertSame(createdStudent, result);
        verify(studentRepository).insert(student);
    }

    @Test
    void addStudent_shouldThrowStudentCodeAlreadyExistsException_whenCodeIsDuplicate() {
        Student student = new Student("24120001", "Phuc", 20);

        DuplicateKeyException expectedCause = new DuplicateKeyException("Duplicate student code");

        when(studentRepository.insert(student)).thenThrow(expectedCause);

        StudentCodeAlreadyExistsException exception = assertThrows(
                StudentCodeAlreadyExistsException.class,
                () -> studentService.addStudent(student));

        assertEquals(
                "Student code 24120001 already exists",
                exception.getMessage());

        assertSame(expectedCause, exception.getCause());
        verify(studentRepository).insert(student);
    }

    @Test
    void updateStudent_shouldReturnUpdatedStudent_whenStudentExists() {
        Student student = new Student("24120002", "Huy Nguyen", 22);
        Student updatedStudent = new Student(2, "24120002", "Huy Nguyen", 22);

        when(studentRepository.update(2, student))
                .thenReturn(Optional.of(updatedStudent));

        Optional<Student> result = studentService.updateStudent(2, student);

        assertTrue(result.isPresent());
        assertSame(updatedStudent, result.get());
        verify(studentRepository).update(2, student);
    }

    @Test
    void updateStudent_shouldReturnEmpty_whenStudentDoesNotExist() {
        Student student = new Student("24120003", "Nam", 22);

        when(studentRepository.update(999, student))
                .thenReturn(Optional.empty());

        Optional<Student> result = studentService.updateStudent(999, student);

        assertTrue(result.isEmpty());
        verify(studentRepository).update(999, student);
    }

    @Test
    void updateStudent_shouldThrowStudentCodeAlreadyExistsException_whenCodeIsDuplicate() {
        Student student = new Student("24120001", "Huy", 20);

        DuplicateKeyException expectedCause = new DuplicateKeyException("Duplicate student code");

        when(studentRepository.update(2, student)).thenThrow(expectedCause);

        StudentCodeAlreadyExistsException exception = assertThrows(
                StudentCodeAlreadyExistsException.class,
                () -> studentService.updateStudent(2, student));

        assertEquals(
                "Student code 24120001 already exists",
                exception.getMessage());

        assertSame(expectedCause, exception.getCause());
        verify(studentRepository).update(2, student);
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

        when(studentRepository.deleteById(1)).thenThrow(expectedCause);

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

        when(studentRepository.deleteById(1)).thenThrow(expectedException);

        DataIntegrityViolationException actualException = assertThrows(
                DataIntegrityViolationException.class,
                () -> studentService.deleteStudent(1));

        assertSame(expectedException, actualException);
        verify(studentRepository).deleteById(1);
    }
}