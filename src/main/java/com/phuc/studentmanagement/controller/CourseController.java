package com.phuc.studentmanagement.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.phuc.studentmanagement.dto.CourseRequestDTO;
import com.phuc.studentmanagement.dto.CourseResponseDTO;
import com.phuc.studentmanagement.exception.ResourceNotFoundException;
import com.phuc.studentmanagement.mapper.CourseMapper;
import com.phuc.studentmanagement.model.Course;
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
    public List<CourseResponseDTO> getAllCourses() {
        return courseService.getAllCourses()
                .stream()
                .map(CourseMapper::toResponseDTO)
                .toList();
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