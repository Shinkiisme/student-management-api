
package com.phuc.studentmanagement.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import com.phuc.studentmanagement.config.PostgresTestConfiguration;
import com.phuc.studentmanagement.model.Course;
import com.phuc.studentmanagement.model.Enrollment;
import com.phuc.studentmanagement.model.Student;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
class StudentRepositoryIntegrationTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void insert_shouldPersistAndRetrieveStudent() {
        Student student = new Student("24990001", "Integration Test", 20);

        Student createdStudent = studentRepository.insert(student);

        Optional<Student> result = studentRepository.findById(createdStudent.getId());

        assertTrue(createdStudent.getId() > 0);
        assertTrue(result.isPresent());
        assertEquals("24990001", result.get().getStudentCode());
        assertEquals("Integration Test", result.get().getName());
        assertEquals(20, result.get().getAge());
    }

    @Test
    void findAll_shouldReturnAllInsertedStudents() {
        Student student1 = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Student student2 = studentRepository.insert(
                new Student("24990002", "Tran Thi B", 21));

        List<Student> result = studentRepository.findAll();

        assertTrue(result.stream()
                .anyMatch(student -> student.getId() == student1.getId()));

        assertTrue(result.stream()
                .anyMatch(student -> student.getId() == student2.getId()));
    }

    @Test
    void findById_shouldReturnStudent_whenStudentExists() {
        Student createdStudent = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Optional<Student> result = studentRepository.findById(createdStudent.getId());

        assertTrue(result.isPresent());
        assertEquals(createdStudent.getId(), result.get().getId());
        assertEquals("24990001", result.get().getStudentCode());
        assertEquals("Nguyen Van A", result.get().getName());
        assertEquals(20, result.get().getAge());
    }

    @Test
    void findById_shouldReturnEmpty_whenStudentDoesNotExist() {
        Optional<Student> result = studentRepository.findById(-1);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByAge_shouldReturnStudentsWithMatchingAge() {
        Student student1 = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Student student2 = studentRepository.insert(
                new Student("24990002", "Tran Thi B", 20));

        Student student3 = studentRepository.insert(
                new Student("24990003", "Le Van C", 21));

        List<Student> result = studentRepository.findByAge(20);

        assertTrue(result.stream().allMatch(student -> student.getAge() == 20));

        assertTrue(result.stream().anyMatch(student -> student.getId() == student1.getId()));

        assertTrue(result.stream().anyMatch(student -> student.getId() == student2.getId()));

        assertFalse(result.stream().anyMatch(student -> student.getId() == student3.getId()));
    }

    @Test
    void update_shouldPersistChanges_whenStudentExists() {
        Student createdStudent = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Student updatedStudent = new Student(
                "24990001",
                "Nguyen Van B",
                21);

        Optional<Student> updateResult = studentRepository.update(
                createdStudent.getId(),
                updatedStudent);

        assertTrue(updateResult.isPresent());
        assertEquals("Nguyen Van B", updateResult.get().getName());
        assertEquals(21, updateResult.get().getAge());

        Optional<Student> result = studentRepository.findById(createdStudent.getId());

        assertTrue(result.isPresent());
        assertEquals("24990001", result.get().getStudentCode());
        assertEquals("Nguyen Van B", result.get().getName());
        assertEquals(21, result.get().getAge());
    }

    @Test
    void update_shouldReturnEmpty_whenStudentDoesNotExist() {
        Student student = new Student("24990001", "Nguyen Van A", 20);

        Optional<Student> result = studentRepository.update(-1, student);

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteById_shouldDeleteStudent_whenStudentExists() {
        Student createdStudent = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        boolean deleted = studentRepository.deleteById(
                createdStudent.getId());

        Optional<Student> result = studentRepository.findById(
                createdStudent.getId());

        assertTrue(deleted);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteById_shouldReturnFalse_whenStudentDoesNotExist() {
        boolean result = studentRepository.deleteById(-1);

        assertFalse(result);
    }

    @Test
    void insert_shouldThrowDuplicateKeyException_whenStudentCodeAlreadyExists() {
        studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Student duplicateStudent = new Student(
                "24990001",
                "Tran Thi B",
                21);

        assertThrows(
                DuplicateKeyException.class,
                () -> studentRepository.insert(duplicateStudent));
    }

    @Test
    void update_shouldThrowDuplicateKeyException_whenStudentCodeAlreadyExists() {
        studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Student student2 = studentRepository.insert(
                new Student("24990002", "Tran Thi B", 21));

        Student updatedStudent = new Student(
                "24990001",
                "Tran Thi B",
                21);

        assertThrows(
                DuplicateKeyException.class,
                () -> studentRepository.update(
                        student2.getId(),
                        updatedStudent));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenStudentCodeIsNull() {
        Student student = new Student(
                null,
                "Nguyen Van A",
                20);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> studentRepository.insert(student));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenStudentCodeIsBlank() {
        Student student = new Student(
                "   ",
                "Nguyen Van A",
                20);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> studentRepository.insert(student));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenStudentCodeExceedsMaxLength() {
        Student student = new Student(
                "1".repeat(21),
                "Nguyen Van A",
                20);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> studentRepository.insert(student));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenAgeIsNegative() {
        Student student = new Student(
                "24990001",
                "Nguyen Van A",
                -1);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> studentRepository.insert(student));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenNameIsNull() {
        Student student = new Student(
                "24990001",
                null,
                20);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> studentRepository.insert(student));
    }

    @Test
    void deleteById_shouldThrowDataIntegrityViolationException_whenStudentHasEnrollments() {
        Student student = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Course course = courseRepository.insert(
                new Course("TEST101", "Integration Testing", 3));

        enrollmentRepository.insert(
                new Enrollment(student.getId(), course.getId()));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> studentRepository.deleteById(student.getId()));
    }
}