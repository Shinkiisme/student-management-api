
package com.phuc.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        when(studentRepository.findAll())
                .thenReturn(List.of(student1, student2));

        List<Student> result = studentService.getAllStudents(null);

        assertEquals(2, result.size());
        assertSame(student1, result.get(0));
        assertSame(student2, result.get(1));

        verify(studentRepository, times(1)).findAll();
        verify(studentRepository, never()).findByAge(anyInt());
    }

    @Test
    void getAllStudents_shouldReturnFilteredStudents_whenAgeIsProvided() {
        Student student1 = new Student(1, "Phuc", 20);
        Student student2 = new Student(3, "Nam", 20);

        when(studentRepository.findByAge(20))
                .thenReturn(List.of(student1, student2));

        List<Student> result = studentService.getAllStudents(20);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(student -> student.getAge() == 20));

        verify(studentRepository, times(1)).findByAge(20);
        verify(studentRepository, never()).findAll();
    }

    @Test
    void getStudentById_shouldReturnStudent_whenStudentExists() {
        Student student = new Student(2, "Phuc", 20);

        when(studentRepository.findById(2))
                .thenReturn(Optional.of(student));

        Optional<Student> result = studentService.getStudentById(2);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getId());
        assertEquals("Phuc", result.get().getName());
        assertEquals(20, result.get().getAge());

        verify(studentRepository, times(1)).findById(2);
    }

    @Test
    void getStudentById_shouldReturnEmpty_whenStudentDoesNotExist() {
        when(studentRepository.findById(999))
                .thenReturn(Optional.empty());

        Optional<Student> result = studentService.getStudentById(999);

        assertTrue(result.isEmpty());

        verify(studentRepository, times(1)).findById(999);
    }

    @Test
    void addStudent_shouldReturnCreatedStudent() {
        Student student = new Student("Nam", 22);
        Student createdStudent = new Student(4, "Nam", 22);

        when(studentRepository.insert(student))
                .thenReturn(createdStudent);

        Student result = studentService.addStudent(student);

        assertNotNull(result);
        assertEquals(4, result.getId());
        assertEquals("Nam", result.getName());
        assertEquals(22, result.getAge());

        verify(studentRepository, times(1)).insert(student);
    }

    @Test
    void updateStudent_shouldReturnUpdatedStudent_whenStudentExists() {
        Student student = new Student("Phuc Nguyen", 21);
        Student updatedStudent = new Student(2, "Phuc Nguyen", 21);

        when(studentRepository.update(2, student))
                .thenReturn(Optional.of(updatedStudent));

        Optional<Student> result = studentService.updateStudent(2, student);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getId());
        assertEquals("Phuc Nguyen", result.get().getName());
        assertEquals(21, result.get().getAge());

        verify(studentRepository, times(1)).update(2, student);
    }

    @Test
    void updateStudent_shouldReturnEmpty_whenStudentDoesNotExist() {
        Student student = new Student("Nam", 22);

        when(studentRepository.update(999, student))
                .thenReturn(Optional.empty());

        Optional<Student> result = studentService.updateStudent(999, student);

        assertTrue(result.isEmpty());

        verify(studentRepository, times(1)).update(999, student);
    }

    @Test
    void deleteStudent_shouldReturnTrue_whenStudentExists() {
        when(studentRepository.deleteById(2))
                .thenReturn(true);

        boolean result = studentService.deleteStudent(2);

        assertTrue(result);

        verify(studentRepository, times(1)).deleteById(2);
    }

    @Test
    void deleteStudent_shouldReturnFalse_whenStudentDoesNotExist() {
        when(studentRepository.deleteById(999))
                .thenReturn(false);

        boolean result = studentService.deleteStudent(999);

        assertFalse(result);

        verify(studentRepository, times(1)).deleteById(999);
    }
}
