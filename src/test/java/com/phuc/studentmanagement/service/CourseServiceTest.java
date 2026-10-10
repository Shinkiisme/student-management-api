
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

import com.phuc.studentmanagement.exception.CourseCodeAlreadyExistsException;
import com.phuc.studentmanagement.exception.ResourceInUseException;
import com.phuc.studentmanagement.model.Course;
import com.phuc.studentmanagement.repository.CourseRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void getAllCourses_shouldReturnAllCourses() {
        Course course1 = new Course(1, "CS101", "Java Programming", 4);
        Course course2 = new Course(2, "CS102", "Data Structures", 4);

        List<Course> expectedCourses = List.of(course1, course2);

        when(courseRepository.findAll()).thenReturn(expectedCourses);

        List<Course> actualCourses = courseService.getAllCourses();

        assertEquals(expectedCourses, actualCourses);
        verify(courseRepository).findAll();
    }

    @Test
    void getCourseById_shouldReturnCourse_whenCourseExists() {
        Course course = new Course(1, "CS101", "Java Programming", 4);

        when(courseRepository.findById(1)).thenReturn(Optional.of(course));

        Optional<Course> result = courseService.getCourseById(1);

        assertTrue(result.isPresent());
        assertSame(course, result.get());
        verify(courseRepository).findById(1);
    }

    @Test
    void getCourseById_shouldReturnEmpty_whenCourseDoesNotExist() {
        when(courseRepository.findById(999)).thenReturn(Optional.empty());

        Optional<Course> result = courseService.getCourseById(999);

        assertTrue(result.isEmpty());
        verify(courseRepository).findById(999);
    }

    @Test
    void addCourse_shouldReturnCreatedCourse_whenCodeIsUnique() {
        Course course = new Course("CS101", "Java Programming", 4);
        Course createdCourse = new Course(1, "CS101", "Java Programming", 4);

        when(courseRepository.insert(course)).thenReturn(createdCourse);

        Course result = courseService.addCourse(course);

        assertSame(createdCourse, result);
        verify(courseRepository).insert(course);
    }

    @Test
    void addCourse_shouldThrowCourseCodeAlreadyExistsException_whenCodeIsDuplicate() {
        Course course = new Course("CS101", "Java Programming", 4);

        DuplicateKeyException expectedCause = new DuplicateKeyException("Duplicate course code");

        when(courseRepository.insert(course)).thenThrow(expectedCause);

        CourseCodeAlreadyExistsException exception = assertThrows(
                CourseCodeAlreadyExistsException.class,
                () -> courseService.addCourse(course));

        assertEquals("Course code CS101 already exists", exception.getMessage());
        assertSame(expectedCause, exception.getCause());

        verify(courseRepository).insert(course);
    }

    @Test
    void updateCourse_shouldReturnUpdatedCourse_whenCourseExists() {
        Course course = new Course("CS101", "Advanced Java", 4);
        Course updatedCourse = new Course(1, "CS101", "Advanced Java", 4);

        when(courseRepository.update(1, course))
                .thenReturn(Optional.of(updatedCourse));

        Optional<Course> result = courseService.updateCourse(1, course);

        assertTrue(result.isPresent());
        assertSame(updatedCourse, result.get());

        verify(courseRepository).update(1, course);
    }

    @Test
    void updateCourse_shouldReturnEmpty_whenCourseDoesNotExist() {
        Course course = new Course("CS101", "Advanced Java", 4);

        when(courseRepository.update(999, course))
                .thenReturn(Optional.empty());

        Optional<Course> result = courseService.updateCourse(999, course);

        assertTrue(result.isEmpty());
        verify(courseRepository).update(999, course);
    }

    @Test
    void updateCourse_shouldThrowCourseCodeAlreadyExistsException_whenCodeIsDuplicate() {
        Course course = new Course("CS101", "Data Structures", 4);

        DuplicateKeyException expectedCause = new DuplicateKeyException("Duplicate course code");

        when(courseRepository.update(2, course)).thenThrow(expectedCause);

        CourseCodeAlreadyExistsException exception = assertThrows(
                CourseCodeAlreadyExistsException.class,
                () -> courseService.updateCourse(2, course));

        assertEquals("Course code CS101 already exists", exception.getMessage());
        assertSame(expectedCause, exception.getCause());

        verify(courseRepository).update(2, course);
    }

    @Test
    void deleteCourse_shouldReturnTrue_whenCourseExists() {
        when(courseRepository.deleteById(1)).thenReturn(true);

        boolean result = courseService.deleteCourse(1);

        assertTrue(result);
        verify(courseRepository).deleteById(1);
    }

    @Test
    void deleteCourse_shouldReturnFalse_whenCourseDoesNotExist() {
        when(courseRepository.deleteById(999)).thenReturn(false);

        boolean result = courseService.deleteCourse(999);

        assertFalse(result);
        verify(courseRepository).deleteById(999);
    }

    @Test
    void deleteCourse_shouldThrowResourceInUseException_whenCourseIsReferenced() {
        SQLException sqlException = new SQLException("Foreign key violation", "23503");

        DataIntegrityViolationException expectedCause = new DataIntegrityViolationException(
                "Cannot delete course",
                sqlException);

        when(courseRepository.deleteById(1)).thenThrow(expectedCause);

        ResourceInUseException exception = assertThrows(
                ResourceInUseException.class,
                () -> courseService.deleteCourse(1));

        assertEquals(
                "Course with id 1 cannot be deleted because it is in use",
                exception.getMessage());
        assertSame(expectedCause, exception.getCause());

        verify(courseRepository).deleteById(1);
    }

    @Test
    void deleteCourse_shouldRethrowDataIntegrityViolationException_whenErrorIsNotForeignKey() {
        SQLException sqlException = new SQLException("Check constraint violation", "23514");

        DataIntegrityViolationException expectedException = new DataIntegrityViolationException(
                "Database constraint violation",
                sqlException);

        when(courseRepository.deleteById(1)).thenThrow(expectedException);

        DataIntegrityViolationException actualException = assertThrows(
                DataIntegrityViolationException.class,
                () -> courseService.deleteCourse(1));

        assertSame(expectedException, actualException);

        verify(courseRepository).deleteById(1);
    }
}