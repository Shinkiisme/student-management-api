
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

        @Test
        void findPage_shouldReturnEmptyPage_whenNoEnrollmentsExist() {
                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(0, 10, "id", SortDirection.ASC), null);

                assertTrue(result.content().isEmpty());
                assertEquals(0, result.page());
                assertEquals(10, result.size());
                assertEquals(0L, result.totalElements());
                assertEquals(0, result.totalPages());
        }

        @Test
        void findPage_shouldReturnFirstPageWithCorrectMetadata() {
                PaginationFixture fixture = createPaginationFixture();

                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(0, 2, "id", SortDirection.ASC), null);

                assertEquals(2, result.content().size());
                assertEquals(fixture.firstEnrollment().getId(), result.content().get(0).getId());
                assertEquals(fixture.secondEnrollment().getId(), result.content().get(1).getId());
                assertEquals(0, result.page());
                assertEquals(2, result.size());
                assertEquals(5L, result.totalElements());
                assertEquals(3, result.totalPages());
        }

        @Test
        void findPage_shouldReturnSecondPageWithCorrectOffset() {
                PaginationFixture fixture = createPaginationFixture();

                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(1, 2, "id", SortDirection.ASC), null);

                assertEquals(2, result.content().size());
                assertEquals(fixture.thirdEnrollment().getId(), result.content().get(0).getId());
                assertEquals(fixture.fourthEnrollment().getId(), result.content().get(1).getId());
                assertEquals(1, result.page());
                assertEquals(2, result.size());
                assertEquals(5L, result.totalElements());
                assertEquals(3, result.totalPages());
        }

        @Test
        void findPage_shouldReturnEmptyContent_whenRequestedPageIsOutOfRange() {
                createPaginationFixture();

                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(3, 2, "id", SortDirection.ASC), null);

                assertTrue(result.content().isEmpty());
                assertEquals(3, result.page());
                assertEquals(2, result.size());
                assertEquals(5L, result.totalElements());
                assertEquals(3, result.totalPages());
        }

        @Test
        void findPage_shouldSortByIdDescending() {
                PaginationFixture fixture = createPaginationFixture();

                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(0, 10, "id", SortDirection.DESC), null);

                assertEquals(List.of(
                                fixture.fifthEnrollment().getId(),
                                fixture.fourthEnrollment().getId(),
                                fixture.thirdEnrollment().getId(),
                                fixture.secondEnrollment().getId(),
                                fixture.firstEnrollment().getId()),
                                result.content().stream().map(Enrollment::getId).toList());
        }

        @Test
        void findPage_shouldSortByStudentIdAscendingWithIdTieBreaker() {
                PaginationFixture fixture = createPaginationFixture();

                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(0, 10, "studentId", SortDirection.ASC), null);

                assertEquals(List.of(
                                fixture.secondEnrollment().getId(),
                                fixture.thirdEnrollment().getId(),
                                fixture.fifthEnrollment().getId(),
                                fixture.firstEnrollment().getId(),
                                fixture.fourthEnrollment().getId()),
                                result.content().stream().map(Enrollment::getId).toList());
        }

        @Test
        void findPage_shouldSortByStudentIdDescendingWithIdTieBreaker() {
                PaginationFixture fixture = createPaginationFixture();

                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(0, 10, "studentId", SortDirection.DESC), null);

                assertEquals(List.of(
                                fixture.firstEnrollment().getId(),
                                fixture.fourthEnrollment().getId(),
                                fixture.secondEnrollment().getId(),
                                fixture.thirdEnrollment().getId(),
                                fixture.fifthEnrollment().getId()),
                                result.content().stream().map(Enrollment::getId).toList());
        }

        @Test
        void findPage_shouldSortByCourseIdAscendingWithIdTieBreaker() {
                PaginationFixture fixture = createPaginationFixture();

                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(0, 10, "courseId", SortDirection.ASC), null);

                assertEquals(List.of(
                                fixture.thirdEnrollment().getId(),
                                fixture.fourthEnrollment().getId(),
                                fixture.secondEnrollment().getId(),
                                fixture.firstEnrollment().getId(),
                                fixture.fifthEnrollment().getId()),
                                result.content().stream().map(Enrollment::getId).toList());
        }

        @Test
        void findPage_shouldSortByCourseIdDescendingWithIdTieBreaker() {
                PaginationFixture fixture = createPaginationFixture();

                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(0, 10, "courseId", SortDirection.DESC), null);

                assertEquals(List.of(
                                fixture.firstEnrollment().getId(),
                                fixture.fifthEnrollment().getId(),
                                fixture.secondEnrollment().getId(),
                                fixture.thirdEnrollment().getId(),
                                fixture.fourthEnrollment().getId()),
                                result.content().stream().map(Enrollment::getId).toList());
        }

        @Test
        void findPage_shouldFilterByStudentIdAndExcludeOtherStudentsData() {
                PaginationFixture fixture = createPaginationFixture();

                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(0, 10, "id", SortDirection.ASC),
                                fixture.firstStudent().getId());

                assertEquals(3, result.content().size());
                assertEquals(List.of(
                                fixture.secondEnrollment().getId(),
                                fixture.thirdEnrollment().getId(),
                                fixture.fifthEnrollment().getId()),
                                result.content().stream().map(Enrollment::getId).toList());
                assertTrue(result.content().stream().allMatch(
                                enrollment -> enrollment.getStudentId() == fixture.firstStudent().getId()));
                assertEquals(3L, result.totalElements());
                assertEquals(1, result.totalPages());
        }

        @Test
        void findPage_shouldPaginateWithinStudentFilter() {
                PaginationFixture fixture = createPaginationFixture();

                PageResult<Enrollment> firstPage = enrollmentRepository.findPage(
                                new PageRequest(0, 2, "id", SortDirection.ASC),
                                fixture.firstStudent().getId());
                PageResult<Enrollment> secondPage = enrollmentRepository.findPage(
                                new PageRequest(1, 2, "id", SortDirection.ASC),
                                fixture.firstStudent().getId());

                assertEquals(List.of(
                                fixture.secondEnrollment().getId(),
                                fixture.thirdEnrollment().getId()),
                                firstPage.content().stream().map(Enrollment::getId).toList());
                assertEquals(List.of(fixture.fifthEnrollment().getId()),
                                secondPage.content().stream().map(Enrollment::getId).toList());
                assertEquals(3L, firstPage.totalElements());
                assertEquals(3L, secondPage.totalElements());
                assertEquals(2, firstPage.totalPages());
                assertEquals(2, secondPage.totalPages());
                assertEquals(1, secondPage.page());
        }

        @Test
        void findPage_shouldReturnEmptyPage_whenStudentHasNoEnrollments() {
                createPaginationFixture();
                Student student = studentRepository.insert(
                                new Student("24992003", "Student Without Enrollments", 22));

                PageResult<Enrollment> result = enrollmentRepository.findPage(
                                new PageRequest(0, 10, "id", SortDirection.ASC), student.getId());

                assertTrue(result.content().isEmpty());
                assertEquals(0L, result.totalElements());
                assertEquals(0, result.totalPages());
        }

        @Test
        void findPage_shouldRejectInvalidSortField() {
                PageRequest request = new PageRequest(0, 10, "id; DROP TABLE enrollments", SortDirection.ASC);

                assertThrows(InvalidPaginationException.class,
                                () -> enrollmentRepository.findPage(request, null));
        }

        private PaginationFixture createPaginationFixture() {
                Student firstStudent = studentRepository.insert(
                                new Student("24992001", "Pagination Student A", 20));
                Student secondStudent = studentRepository.insert(
                                new Student("24992002", "Pagination Student B", 21));

                Course firstCourse = courseRepository.insert(
                                new Course("PAGE101", "Pagination Course One", 3));
                Course secondCourse = courseRepository.insert(
                                new Course("PAGE102", "Pagination Course Two", 4));
                Course thirdCourse = courseRepository.insert(
                                new Course("PAGE103", "Pagination Course Three", 5));

                Enrollment firstEnrollment = enrollmentRepository.insert(
                                new Enrollment(secondStudent.getId(), thirdCourse.getId()));
                Enrollment secondEnrollment = enrollmentRepository.insert(
                                new Enrollment(firstStudent.getId(), secondCourse.getId()));
                Enrollment thirdEnrollment = enrollmentRepository.insert(
                                new Enrollment(firstStudent.getId(), firstCourse.getId()));
                Enrollment fourthEnrollment = enrollmentRepository.insert(
                                new Enrollment(secondStudent.getId(), firstCourse.getId()));
                Enrollment fifthEnrollment = enrollmentRepository.insert(
                                new Enrollment(firstStudent.getId(), thirdCourse.getId()));

                return new PaginationFixture(
                                firstStudent,
                                firstEnrollment,
                                secondEnrollment,
                                thirdEnrollment,
                                fourthEnrollment,
                                fifthEnrollment);
        }

        private record PaginationFixture(
                        Student firstStudent,
                        Enrollment firstEnrollment,
                        Enrollment secondEnrollment,
                        Enrollment thirdEnrollment,
                        Enrollment fourthEnrollment,
                        Enrollment fifthEnrollment) {
        }
}