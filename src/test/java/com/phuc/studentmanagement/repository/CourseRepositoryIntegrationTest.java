
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
class CourseRepositoryIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void insert_shouldPersistAndRetrieveCourse() {
        Course course = new Course("CS101", "Java Programming", 4);

        Course createdCourse = courseRepository.insert(course);

        Optional<Course> result = courseRepository.findById(createdCourse.getId());

        assertTrue(createdCourse.getId() > 0);
        assertTrue(result.isPresent());
        assertEquals("CS101", result.get().getCode());
        assertEquals("Java Programming", result.get().getName());
        assertEquals(4, result.get().getCredits());
    }

    @Test
    void findAll_shouldReturnAllInsertedCourses() {
        Course course1 = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Course course2 = courseRepository.insert(
                new Course("CS102", "Data Structures", 4));

        List<Course> result = courseRepository.findAll();

        assertTrue(result.stream()
                .anyMatch(course -> course.getId() == course1.getId()));

        assertTrue(result.stream()
                .anyMatch(course -> course.getId() == course2.getId()));
    }

    @Test
    void findById_shouldReturnCourse_whenCourseExists() {
        Course createdCourse = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Optional<Course> result = courseRepository.findById(createdCourse.getId());

        assertTrue(result.isPresent());
        assertEquals(createdCourse.getId(), result.get().getId());
        assertEquals("CS101", result.get().getCode());
        assertEquals("Java Programming", result.get().getName());
        assertEquals(4, result.get().getCredits());
    }

    @Test
    void findById_shouldReturnEmpty_whenCourseDoesNotExist() {
        Optional<Course> result = courseRepository.findById(-1);

        assertTrue(result.isEmpty());
    }

    @Test
    void update_shouldPersistChanges_whenCourseExists() {
        Course createdCourse = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Course updatedCourse = new Course(
                "CS101",
                "Advanced Java Programming",
                3);

        Optional<Course> updateResult = courseRepository.update(
                createdCourse.getId(),
                updatedCourse);

        assertTrue(updateResult.isPresent());
        assertEquals("Advanced Java Programming", updateResult.get().getName());
        assertEquals(3, updateResult.get().getCredits());

        Optional<Course> result = courseRepository.findById(createdCourse.getId());

        assertTrue(result.isPresent());
        assertEquals("CS101", result.get().getCode());
        assertEquals("Advanced Java Programming", result.get().getName());
        assertEquals(3, result.get().getCredits());
    }

    @Test
    void update_shouldReturnEmpty_whenCourseDoesNotExist() {
        Course course = new Course("CS101", "Java Programming", 4);

        Optional<Course> result = courseRepository.update(-1, course);

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteById_shouldDeleteCourse_whenCourseExists() {
        Course createdCourse = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        boolean deleted = courseRepository.deleteById(
                createdCourse.getId());

        Optional<Course> result = courseRepository.findById(
                createdCourse.getId());

        assertTrue(deleted);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteById_shouldReturnFalse_whenCourseDoesNotExist() {
        boolean result = courseRepository.deleteById(-1);

        assertFalse(result);
    }

    @Test
    void insert_shouldThrowDuplicateKeyException_whenCourseCodeAlreadyExists() {
        courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Course duplicateCourse = new Course(
                "CS101",
                "Advanced Java",
                3);

        assertThrows(
                DuplicateKeyException.class,
                () -> courseRepository.insert(duplicateCourse));
    }

    @Test
    void update_shouldThrowDuplicateKeyException_whenCourseCodeAlreadyExists() {
        courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Course course2 = courseRepository.insert(
                new Course("CS102", "Data Structures", 4));

        Course updatedCourse = new Course(
                "CS101",
                "Data Structures",
                4);

        assertThrows(
                DuplicateKeyException.class,
                () -> courseRepository.update(
                        course2.getId(),
                        updatedCourse));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenCodeIsNull() {
        Course course = new Course(null, "Java Programming", 4);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> courseRepository.insert(course));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenNameIsNull() {
        Course course = new Course("CS101", null, 4);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> courseRepository.insert(course));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenCreditsAreZero() {
        Course course = new Course("CS101", "Java Programming", 0);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> courseRepository.insert(course));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenCreditsAreNegative() {
        Course course = new Course("CS101", "Java Programming", -1);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> courseRepository.insert(course));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenCodeExceedsMaxLength() {
        Course course = new Course(
                "A".repeat(21),
                "Java Programming",
                4);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> courseRepository.insert(course));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenNameExceedsMaxLength() {
        Course course = new Course(
                "CS101",
                "A".repeat(101),
                4);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> courseRepository.insert(course));
    }

    @Test
    void deleteById_shouldThrowDataIntegrityViolationException_whenCourseHasEnrollments() {
        Student student = studentRepository.insert(
                new Student("24990001", "Integration Student", 20));

        Course course = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        enrollmentRepository.insert(
                new Enrollment(student.getId(), course.getId()));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> courseRepository.deleteById(course.getId()));
    }
}