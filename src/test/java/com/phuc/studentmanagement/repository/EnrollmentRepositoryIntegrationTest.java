
package com.phuc.studentmanagement.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
class EnrollmentRepositoryIntegrationTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void insert_shouldPersistAndRetrieveEnrollment() {
        Student student = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Course course = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Enrollment enrollment = new Enrollment(
                student.getId(),
                course.getId());

        Enrollment createdEnrollment = enrollmentRepository.insert(enrollment);

        Optional<Enrollment> result = enrollmentRepository.findById(createdEnrollment.getId());

        assertTrue(createdEnrollment.getId() > 0);
        assertTrue(result.isPresent());
        assertEquals(createdEnrollment.getId(), result.get().getId());
        assertEquals(student.getId(), result.get().getStudentId());
        assertEquals(course.getId(), result.get().getCourseId());
    }

    @Test
    void findAll_shouldReturnAllInsertedEnrollments() {
        Student student = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Course course1 = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Course course2 = courseRepository.insert(
                new Course("CS102", "Data Structures", 4));

        Enrollment enrollment1 = enrollmentRepository.insert(
                new Enrollment(student.getId(), course1.getId()));

        Enrollment enrollment2 = enrollmentRepository.insert(
                new Enrollment(student.getId(), course2.getId()));

        List<Enrollment> result = enrollmentRepository.findAll();

        assertEquals(2, result.size());
        assertEquals(enrollment1.getId(), result.get(0).getId());
        assertEquals(enrollment2.getId(), result.get(1).getId());
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoEnrollmentsExist() {
        List<Enrollment> result = enrollmentRepository.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void findById_shouldReturnEnrollment_whenEnrollmentExists() {
        Student student = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Course course = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Enrollment createdEnrollment = enrollmentRepository.insert(
                new Enrollment(student.getId(), course.getId()));

        Optional<Enrollment> result = enrollmentRepository.findById(createdEnrollment.getId());

        assertTrue(result.isPresent());
        assertEquals(createdEnrollment.getId(), result.get().getId());
        assertEquals(student.getId(), result.get().getStudentId());
        assertEquals(course.getId(), result.get().getCourseId());
    }

    @Test
    void findById_shouldReturnEmpty_whenEnrollmentDoesNotExist() {
        Optional<Enrollment> result = enrollmentRepository.findById(-1);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByStudentId_shouldReturnOnlyMatchingEnrollments() {
        Student student1 = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Student student2 = studentRepository.insert(
                new Student("24990002", "Tran Thi B", 21));

        Course course1 = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Course course2 = courseRepository.insert(
                new Course("CS102", "Data Structures", 4));

        Enrollment enrollment1 = enrollmentRepository.insert(
                new Enrollment(student1.getId(), course1.getId()));

        Enrollment enrollment2 = enrollmentRepository.insert(
                new Enrollment(student1.getId(), course2.getId()));

        enrollmentRepository.insert(
                new Enrollment(student2.getId(), course1.getId()));

        List<Enrollment> result = enrollmentRepository.findByStudentId(student1.getId());

        assertEquals(2, result.size());

        assertTrue(result.stream().allMatch(enrollment -> enrollment.getStudentId() == student1.getId()));

        assertEquals(enrollment1.getId(), result.get(0).getId());
        assertEquals(enrollment2.getId(), result.get(1).getId());
    }

    @Test
    void findByStudentId_shouldReturnEmptyList_whenStudentHasNoEnrollments() {
        Student student = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        List<Enrollment> result = enrollmentRepository.findByStudentId(student.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteById_shouldDeleteEnrollment_whenEnrollmentExists() {
        Student student = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Course course = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Enrollment enrollment = enrollmentRepository.insert(
                new Enrollment(student.getId(), course.getId()));

        boolean deleted = enrollmentRepository.deleteById(enrollment.getId());

        Optional<Enrollment> result = enrollmentRepository.findById(enrollment.getId());

        assertTrue(deleted);
        assertTrue(result.isEmpty());

        assertTrue(studentRepository.findById(student.getId()).isPresent());
        assertTrue(courseRepository.findById(course.getId()).isPresent());
    }

    @Test
    void deleteById_shouldReturnFalse_whenEnrollmentDoesNotExist() {
        boolean result = enrollmentRepository.deleteById(-1);

        assertFalse(result);
    }

    @Test
    void insert_shouldThrowDuplicateKeyException_whenEnrollmentAlreadyExists() {
        Student student = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Course course = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        enrollmentRepository.insert(
                new Enrollment(student.getId(), course.getId()));

        Enrollment duplicateEnrollment = new Enrollment(
                student.getId(),
                course.getId());

        assertThrows(
                DuplicateKeyException.class,
                () -> enrollmentRepository.insert(duplicateEnrollment));
    }

    @Test
    void insert_shouldAllowSameStudentInDifferentCourses() {
        Student student = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Course course1 = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Course course2 = courseRepository.insert(
                new Course("CS102", "Data Structures", 4));

        Enrollment enrollment1 = enrollmentRepository.insert(
                new Enrollment(student.getId(), course1.getId()));

        Enrollment enrollment2 = enrollmentRepository.insert(
                new Enrollment(student.getId(), course2.getId()));

        assertNotEquals(enrollment1.getId(), enrollment2.getId());

        List<Enrollment> result = enrollmentRepository.findByStudentId(student.getId());

        assertEquals(2, result.size());
    }

    @Test
    void insert_shouldAllowDifferentStudentsInSameCourse() {
        Student student1 = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Student student2 = studentRepository.insert(
                new Student("24990002", "Tran Thi B", 21));

        Course course = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Enrollment enrollment1 = enrollmentRepository.insert(
                new Enrollment(student1.getId(), course.getId()));

        Enrollment enrollment2 = enrollmentRepository.insert(
                new Enrollment(student2.getId(), course.getId()));

        assertNotEquals(enrollment1.getId(), enrollment2.getId());

        assertEquals(course.getId(), enrollment1.getCourseId());
        assertEquals(course.getId(), enrollment2.getCourseId());

        assertNotEquals(
                enrollment1.getStudentId(),
                enrollment2.getStudentId());
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenStudentDoesNotExist() {
        Course course = courseRepository.insert(
                new Course("CS101", "Java Programming", 4));

        Enrollment enrollment = new Enrollment(
                -1,
                course.getId());

        assertThrows(
                DataIntegrityViolationException.class,
                () -> enrollmentRepository.insert(enrollment));
    }

    @Test
    void insert_shouldThrowDataIntegrityViolationException_whenCourseDoesNotExist() {
        Student student = studentRepository.insert(
                new Student("24990001", "Nguyen Van A", 20));

        Enrollment enrollment = new Enrollment(
                student.getId(),
                -1);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> enrollmentRepository.insert(enrollment));
    }
}