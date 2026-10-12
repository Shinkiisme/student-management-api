package com.phuc.studentmanagement.controller;

import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.phuc.studentmanagement.dto.CourseRequestDTO;
import com.phuc.studentmanagement.dto.CourseResponseDTO;
import com.phuc.studentmanagement.dto.PageResponseDTO;
import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.exception.ResourceNotFoundException;
import com.phuc.studentmanagement.mapper.CourseMapper;
import com.phuc.studentmanagement.model.Course;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;
import com.phuc.studentmanagement.pagination.SortDirection;
import com.phuc.studentmanagement.service.CourseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/courses")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public PageResponseDTO<CourseResponseDTO> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Integer credits) {
        if (credits != null && credits <= 0) {
            throw new InvalidPaginationException("Credits must be positive");
        }

        SortDirection sortDirection;

        try {
            sortDirection = SortDirection.valueOf(
                    direction.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidPaginationException("Direction must be asc or desc");
        }

        PageRequest request;

        try {
            request = new PageRequest(page, size, sortBy, sortDirection);
        } catch (IllegalArgumentException exception) {
            throw new InvalidPaginationException(exception.getMessage());
        }

        PageResult<Course> courses = courseService.getCoursesPage(request, credits);

        return new PageResponseDTO<>(
                courses.content()
                        .stream()
                        .map(CourseMapper::toResponseDTO)
                        .toList(),
                courses.page(),
                courses.size(),
                courses.totalElements(),
                courses.totalPages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponseDTO> getCourseById(@PathVariable int id) {
        return courseService.getCourseById(id)
                .map(CourseMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }

    @PostMapping
    public ResponseEntity<CourseResponseDTO> addCourse(@Valid @RequestBody CourseRequestDTO request) {
        Course course = CourseMapper.toCourse(request);
        Course createdCourse = courseService.addCourse(course);

        return ResponseEntity.status(201).body(CourseMapper.toResponseDTO(createdCourse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponseDTO> updateCourse(@PathVariable int id,
            @Valid @RequestBody CourseRequestDTO request) {
        Course course = CourseMapper.toCourse(request);

        return courseService.updateCourse(id, course)
                .map(CourseMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable int id) {
        if (courseService.deleteCourse(id)) {
            return ResponseEntity.noContent().build();
        }

        throw new ResourceNotFoundException("Course", id);
    }
}