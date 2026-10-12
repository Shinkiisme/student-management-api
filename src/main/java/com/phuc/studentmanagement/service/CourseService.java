package com.phuc.studentmanagement.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.phuc.studentmanagement.exception.CourseCodeAlreadyExistsException;
import com.phuc.studentmanagement.exception.ResourceInUseException;
import com.phuc.studentmanagement.model.Course;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;
import com.phuc.studentmanagement.repository.CourseRepository;

@Service
public class CourseService {
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public PageResult<Course> getCoursesPage(PageRequest request, Integer credits) {
        return courseRepository.findPage(request, credits);
    }

    public Optional<Course> getCourseById(int id) {
        return courseRepository.findById(id);
    }

    public Course addCourse(Course course) {
        try {
            return courseRepository.insert(course);
        } catch (DuplicateKeyException exception) {
            throw new CourseCodeAlreadyExistsException(course.getCode(), exception);
        }
    }

    public Optional<Course> updateCourse(int id, Course course) {
        try {
            return courseRepository.update(id, course);
        } catch (DuplicateKeyException exception) {
            throw new CourseCodeAlreadyExistsException(course.getCode(), exception);
        }
    }

    public boolean deleteCourse(int id) {
        try {
            return courseRepository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            Throwable cause = exception.getMostSpecificCause();

            if (cause instanceof SQLException sqlException && "23503".equals(sqlException.getSQLState())) {
                throw new ResourceInUseException("Course", id, exception);
            }

            throw exception;
        }
    }
}