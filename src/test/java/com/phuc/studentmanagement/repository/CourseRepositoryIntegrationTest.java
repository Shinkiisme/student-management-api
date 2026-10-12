
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
import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.model.Course;
import com.phuc.studentmanagement.model.Enrollment;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;
import com.phuc.studentmanagement.pagination.SortDirection;

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

        @Test
        void findPage_shouldReturnFirstPage() {
                Course course1 = courseRepository.insert(
                                new Course("CS101", "Java Programming", 4));

                Course course2 = courseRepository.insert(
                                new Course("CS102", "Data Structures", 3));

                courseRepository.insert(
                                new Course("CS103", "Databases", 4));

                courseRepository.insert(
                                new Course("CS104", "Operating Systems", 3));

                courseRepository.insert(
                                new Course("CS105", "Computer Networks", 3));

                PageRequest request = new PageRequest(
                                0, 2, "id", SortDirection.ASC);

                PageResult<Course> result = courseRepository.findPage(request, null);

                assertEquals(2, result.content().size());
                assertEquals(0, result.page());
                assertEquals(2, result.size());
                assertEquals(5, result.totalElements());
                assertEquals(3, result.totalPages());

                assertEquals(
                                List.of(course1.getId(), course2.getId()),
                                result.content().stream()
                                                .map(course -> course.getId())
                                                .toList());
        }

        @Test
        void findPage_shouldReturnSecondPage() {
                courseRepository.insert(
                                new Course("CS101", "Java Programming", 4));

                courseRepository.insert(
                                new Course("CS102", "Data Structures", 3));

                Course course3 = courseRepository.insert(
                                new Course("CS103", "Databases", 4));

                Course course4 = courseRepository.insert(
                                new Course("CS104", "Operating Systems", 3));

                PageRequest request = new PageRequest(
                                1, 2, "id", SortDirection.ASC);

                PageResult<Course> result = courseRepository.findPage(request, null);

                assertEquals(2, result.content().size());
                assertEquals(1, result.page());
                assertEquals(2, result.size());
                assertEquals(4, result.totalElements());
                assertEquals(2, result.totalPages());

                assertEquals(
                                List.of(course3.getId(), course4.getId()),
                                result.content().stream()
                                                .map(course -> course.getId())
                                                .toList());
        }

        @Test
        void findPage_shouldReturnEmptyContent_whenPageExceedsTotalPages() {
                courseRepository.insert(
                                new Course("CS101", "Java Programming", 4));

                courseRepository.insert(
                                new Course("CS102", "Data Structures", 3));

                PageRequest request = new PageRequest(
                                2, 2, "id", SortDirection.ASC);

                PageResult<Course> result = courseRepository.findPage(request, null);

                assertTrue(result.content().isEmpty());
                assertEquals(2, result.page());
                assertEquals(2, result.size());
                assertEquals(2, result.totalElements());
                assertEquals(1, result.totalPages());
        }

        @Test
        void findPage_shouldReturnAllCourses_whenPageSizeExceedsTotalElements() {
                courseRepository.insert(
                                new Course("CS101", "Java Programming", 4));

                courseRepository.insert(
                                new Course("CS102", "Data Structures", 3));

                courseRepository.insert(
                                new Course("CS103", "Databases", 4));

                PageRequest request = new PageRequest(
                                0, 10, "id", SortDirection.ASC);

                PageResult<Course> result = courseRepository.findPage(request, null);

                assertEquals(3, result.content().size());
                assertEquals(0, result.page());
                assertEquals(10, result.size());
                assertEquals(3, result.totalElements());
                assertEquals(1, result.totalPages());
        }

        @Test
        void findPage_shouldReturnEmptyPage_whenNoCoursesExist() {
                PageRequest request = new PageRequest(
                                0, 10, "id", SortDirection.ASC);

                PageResult<Course> result = courseRepository.findPage(request, null);

                assertTrue(result.content().isEmpty());
                assertEquals(0, result.page());
                assertEquals(10, result.size());
                assertEquals(0, result.totalElements());
                assertEquals(0, result.totalPages());
        }

        @Test
        void findPage_shouldSortByIdDescending() {
                Course course1 = courseRepository.insert(
                                new Course("CS101", "Java Programming", 4));

                Course course2 = courseRepository.insert(
                                new Course("CS102", "Data Structures", 3));

                Course course3 = courseRepository.insert(
                                new Course("CS103", "Databases", 4));

                PageRequest request = new PageRequest(
                                0, 10, "id", SortDirection.DESC);

                PageResult<Course> result = courseRepository.findPage(request, null);

                assertEquals(
                                List.of(
                                                course3.getId(),
                                                course2.getId(),
                                                course1.getId()),
                                result.content().stream()
                                                .map(course -> course.getId())
                                                .toList());
        }

        @Test
        void findPage_shouldSortByCodeAscending() {
                courseRepository.insert(
                                new Course("CS303", "Operating Systems", 4));

                courseRepository.insert(
                                new Course("CS101", "Java Programming", 3));

                courseRepository.insert(
                                new Course("CS202", "Data Structures", 4));

                PageRequest request = new PageRequest(
                                0, 10, "code", SortDirection.ASC);

                PageResult<Course> result = courseRepository.findPage(request, null);

                assertEquals(
                                List.of("CS101", "CS202", "CS303"),
                                result.content().stream()
                                                .map(course -> course.getCode())
                                                .toList());
        }

        @Test
        void findPage_shouldSortByNameDescending() {
                courseRepository.insert(
                                new Course("CS101", "Algorithms", 4));

                courseRepository.insert(
                                new Course("CS102", "Zoology", 3));

                courseRepository.insert(
                                new Course("CS103", "Databases", 4));

                PageRequest request = new PageRequest(
                                0, 10, "name", SortDirection.DESC);

                PageResult<Course> result = courseRepository.findPage(request, null);

                assertEquals(
                                List.of("Zoology", "Databases", "Algorithms"),
                                result.content().stream()
                                                .map(course -> course.getName())
                                                .toList());
        }

        @Test
        void findPage_shouldSortByCreditsAscending_withIdAsTieBreaker() {
                Course course1 = courseRepository.insert(
                                new Course("CS101", "Java Programming", 4));

                Course course2 = courseRepository.insert(
                                new Course("CS102", "Data Structures", 2));

                Course course3 = courseRepository.insert(
                                new Course("CS103", "Databases", 4));

                Course course4 = courseRepository.insert(
                                new Course("CS104", "Operating Systems", 3));

                PageRequest request = new PageRequest(
                                0, 10, "credits", SortDirection.ASC);

                PageResult<Course> result = courseRepository.findPage(request, null);

                assertEquals(
                                List.of(
                                                course2.getId(),
                                                course4.getId(),
                                                course1.getId(),
                                                course3.getId()),
                                result.content().stream()
                                                .map(course -> course.getId())
                                                .toList());
        }

        @Test
        void findPage_shouldSortByCreditsDescending_withIdAsTieBreaker() {
                Course course1 = courseRepository.insert(
                                new Course("CS101", "Java Programming", 2));

                Course course2 = courseRepository.insert(
                                new Course("CS102", "Data Structures", 4));

                Course course3 = courseRepository.insert(
                                new Course("CS103", "Databases", 4));

                Course course4 = courseRepository.insert(
                                new Course("CS104", "Operating Systems", 3));

                PageRequest request = new PageRequest(
                                0, 10, "credits", SortDirection.DESC);

                PageResult<Course> result = courseRepository.findPage(request, null);

                assertEquals(
                                List.of(
                                                course2.getId(),
                                                course3.getId(),
                                                course4.getId(),
                                                course1.getId()),
                                result.content().stream()
                                                .map(course -> course.getId())
                                                .toList());
        }

        @Test
        void findPage_shouldFilterByCredits() {
                Course course1 = courseRepository.insert(
                                new Course("CS101", "Java Programming", 4));

                courseRepository.insert(
                                new Course("CS102", "Data Structures", 3));

                Course course3 = courseRepository.insert(
                                new Course("CS103", "Databases", 4));

                PageRequest request = new PageRequest(
                                0, 10, "id", SortDirection.ASC);

                PageResult<Course> result = courseRepository.findPage(request, 4);

                assertEquals(2, result.content().size());
                assertEquals(2, result.totalElements());
                assertEquals(1, result.totalPages());

                assertTrue(result.content().stream()
                                .allMatch(course -> course.getCredits() == 4));

                assertEquals(
                                List.of(course1.getId(), course3.getId()),
                                result.content().stream()
                                                .map(course -> course.getId())
                                                .toList());
        }

        @Test
        void findPage_shouldReturnEmptyPage_whenNoCoursesMatchCredits() {
                courseRepository.insert(
                                new Course("CS101", "Java Programming", 4));

                courseRepository.insert(
                                new Course("CS102", "Data Structures", 3));

                PageRequest request = new PageRequest(
                                0, 10, "id", SortDirection.ASC);

                PageResult<Course> result = courseRepository.findPage(request, 5);

                assertTrue(result.content().isEmpty());
                assertEquals(0, result.totalElements());
                assertEquals(0, result.totalPages());
        }

        @Test
        void findPage_shouldPaginateFilteredCourses() {
                courseRepository.insert(
                                new Course("CS101", "Java Programming", 4));

                courseRepository.insert(
                                new Course("CS102", "Data Structures", 3));

                courseRepository.insert(
                                new Course("CS103", "Databases", 4));

                Course course4 = courseRepository.insert(
                                new Course("CS104", "Operating Systems", 4));

                PageRequest request = new PageRequest(
                                1, 2, "code", SortDirection.ASC);

                PageResult<Course> result = courseRepository.findPage(request, 4);

                assertEquals(1, result.content().size());
                assertEquals(1, result.page());
                assertEquals(2, result.size());
                assertEquals(3, result.totalElements());
                assertEquals(2, result.totalPages());

                assertEquals(course4.getId(), result.content().get(0).getId());
                assertEquals("CS104", result.content().get(0).getCode());
                assertEquals(4, result.content().get(0).getCredits());
        }

        @Test
        void findPage_shouldThrowInvalidPaginationException_whenSortFieldIsInvalid() {
                PageRequest request = new PageRequest(
                                0, 10, "invalid", SortDirection.ASC);

                InvalidPaginationException exception = assertThrows(
                                InvalidPaginationException.class,
                                () -> courseRepository.findPage(request, null));

                assertEquals(
                                "Invalid sort field: invalid",
                                exception.getMessage());
        }
}