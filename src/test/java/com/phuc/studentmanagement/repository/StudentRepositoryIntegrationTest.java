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

        @Test
        void findPage_shouldReturnFirstPage() {
                List<Student> students = insertPaginationStudents();
                PageRequest request = new PageRequest(0, 2, "id", SortDirection.ASC);

                PageResult<Student> result = studentRepository.findPage(request, null);

                assertEquals(2, result.content().size());
                assertEquals(0, result.page());
                assertEquals(2, result.size());
                assertEquals(5, result.totalElements());
                assertEquals(3, result.totalPages());
                assertEquals(List.of(students.get(0).getId(), students.get(1).getId()),
                                result.content().stream().map(student -> student.getId()).toList());
        }

        @Test
        void findPage_shouldReturnSecondPage() {
                List<Student> students = insertPaginationStudents();
                PageRequest request = new PageRequest(1, 2, "id", SortDirection.ASC);

                PageResult<Student> result = studentRepository.findPage(request, null);

                assertEquals(2, result.content().size());
                assertEquals(1, result.page());
                assertEquals(2, result.size());
                assertEquals(5, result.totalElements());
                assertEquals(3, result.totalPages());
                assertEquals(List.of(students.get(2).getId(), students.get(3).getId()),
                                result.content().stream().map(student -> student.getId()).toList());
        }

        @Test
        void findPage_shouldReturnEmptyContent_whenPageExceedsLastPage() {
                insertPaginationStudents();
                PageRequest request = new PageRequest(3, 2, "id", SortDirection.ASC);

                PageResult<Student> result = studentRepository.findPage(request, null);

                assertTrue(result.content().isEmpty());
                assertEquals(3, result.page());
                assertEquals(2, result.size());
                assertEquals(5, result.totalElements());
                assertEquals(3, result.totalPages());
        }

        @Test
        void findPage_shouldReturnEmptyResult_whenNoStudentsExist() {
                PageRequest request = new PageRequest(0, 10, "id", SortDirection.ASC);

                PageResult<Student> result = studentRepository.findPage(request, null);

                assertTrue(result.content().isEmpty());
                assertEquals(0, result.totalElements());
                assertEquals(0, result.totalPages());
        }

        @Test
        void findPage_shouldSortByIdDescending() {
                List<Student> students = insertPaginationStudents();
                PageRequest request = new PageRequest(0, 5, "id", SortDirection.DESC);

                PageResult<Student> result = studentRepository.findPage(request, null);

                assertEquals(List.of(
                                students.get(4).getId(),
                                students.get(3).getId(),
                                students.get(2).getId(),
                                students.get(1).getId(),
                                students.get(0).getId()),
                                result.content().stream().map(student -> student.getId()).toList());
        }

        @Test
        void findPage_shouldSortByNameAscending() {
                List<Student> students = insertPaginationStudents();
                PageRequest request = new PageRequest(0, 5, "name", SortDirection.ASC);

                PageResult<Student> result = studentRepository.findPage(request, null);

                assertEquals(List.of(
                                students.get(1).getId(),
                                students.get(3).getId(),
                                students.get(0).getId(),
                                students.get(2).getId(),
                                students.get(4).getId()),
                                result.content().stream().map(student -> student.getId()).toList());
        }

        @Test
        void findPage_shouldSortByNameDescending() {
                List<Student> students = insertPaginationStudents();
                PageRequest request = new PageRequest(0, 5, "name", SortDirection.DESC);

                PageResult<Student> result = studentRepository.findPage(request, null);

                assertEquals(List.of(
                                students.get(4).getId(),
                                students.get(2).getId(),
                                students.get(0).getId(),
                                students.get(1).getId(),
                                students.get(3).getId()),
                                result.content().stream().map(student -> student.getId()).toList());
        }

        @Test
        void findPage_shouldUseIdAsTieBreaker_whenNamesAreEqual() {
                List<Student> students = insertPaginationStudents();
                PageRequest request = new PageRequest(0, 2, "name", SortDirection.ASC);

                PageResult<Student> result = studentRepository.findPage(request, null);

                assertEquals(List.of(students.get(1).getId(), students.get(3).getId()),
                                result.content().stream().map(student -> student.getId()).toList());
        }

        @Test
        void findPage_shouldSortByStudentCode() {
                List<Student> students = insertPaginationStudents();
                PageRequest request = new PageRequest(0, 5, "studentCode", SortDirection.DESC);

                PageResult<Student> result = studentRepository.findPage(request, null);

                assertEquals(List.of(
                                students.get(4).getStudentCode(),
                                students.get(3).getStudentCode(),
                                students.get(2).getStudentCode(),
                                students.get(1).getStudentCode(),
                                students.get(0).getStudentCode()),
                                result.content().stream().map(student -> student.getStudentCode()).toList());
        }

        @Test
        void findPage_shouldSortByAgeDescending() {
                List<Student> students = insertPaginationStudents();
                PageRequest request = new PageRequest(0, 5, "age", SortDirection.DESC);

                PageResult<Student> result = studentRepository.findPage(request, null);

                assertEquals(List.of(
                                students.get(3).getId(),
                                students.get(1).getId(),
                                students.get(0).getId(),
                                students.get(2).getId(),
                                students.get(4).getId()),
                                result.content().stream().map(student -> student.getId()).toList());
        }

        @Test
        void findPage_shouldFilterByAgeAndCountMatches() {
                List<Student> students = insertPaginationStudents();
                PageRequest request = new PageRequest(0, 2, "id", SortDirection.ASC);

                PageResult<Student> result = studentRepository.findPage(request, 20);

                assertEquals(2, result.content().size());
                assertEquals(3, result.totalElements());
                assertEquals(2, result.totalPages());
                assertTrue(result.content().stream().allMatch(student -> student.getAge() == 20));
                assertEquals(List.of(students.get(0).getId(), students.get(2).getId()),
                                result.content().stream().map(student -> student.getId()).toList());
        }

        @Test
        void findPage_shouldReturnSecondPage_whenFilteringByAge() {
                List<Student> students = insertPaginationStudents();
                PageRequest request = new PageRequest(1, 2, "id", SortDirection.ASC);

                PageResult<Student> result = studentRepository.findPage(request, 20);

                assertEquals(1, result.content().size());
                assertEquals(3, result.totalElements());
                assertEquals(2, result.totalPages());
                assertEquals(students.get(4).getId(), result.content().get(0).getId());
        }

        @Test
        void findPage_shouldReturnEmptyResult_whenAgeDoesNotMatch() {
                insertPaginationStudents();
                PageRequest request = new PageRequest(0, 10, "id", SortDirection.ASC);

                PageResult<Student> result = studentRepository.findPage(request, 99);

                assertTrue(result.content().isEmpty());
                assertEquals(0, result.totalElements());
                assertEquals(0, result.totalPages());
        }

        @Test
        void findPage_shouldThrowIllegalArgumentException_whenSortFieldIsInvalid() {
                PageRequest request = new PageRequest(0, 10, "invalidField", SortDirection.ASC);

                InvalidPaginationException exception = assertThrows(
                                InvalidPaginationException.class,
                                () -> studentRepository.findPage(request, null));

                assertEquals("Invalid sort field: invalidField", exception.getMessage());
        }

        private List<Student> insertPaginationStudents() {
                return List.of(
                                studentRepository.insert(new Student("24990001", "Binh", 20)),
                                studentRepository.insert(new Student("24990002", "An", 21)),
                                studentRepository.insert(new Student("24990003", "Cuong", 20)),
                                studentRepository.insert(new Student("24990004", "An", 22)),
                                studentRepository.insert(new Student("24990005", "Dung", 20)));
        }
}