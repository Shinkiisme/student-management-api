package com.phuc.studentmanagement.controller;

import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.phuc.studentmanagement.dto.PageResponseDTO;
import com.phuc.studentmanagement.dto.StudentRequestDTO;
import com.phuc.studentmanagement.dto.StudentResponseDTO;
import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.exception.ResourceNotFoundException;
import com.phuc.studentmanagement.mapper.StudentMapper;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;
import com.phuc.studentmanagement.pagination.SortDirection;
import com.phuc.studentmanagement.service.StudentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public PageResponseDTO<StudentResponseDTO> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Integer age) {

        if (age != null && age < 0) {
            throw new InvalidPaginationException("Age cannot be negative");
        }

        SortDirection sortDirection;

        try {
            sortDirection = SortDirection.valueOf(direction.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidPaginationException("Direction must be asc or desc");
        }

        PageRequest request;

        try {
            request = new PageRequest(
                    page,
                    size,
                    sortBy,
                    sortDirection);
        } catch (IllegalArgumentException exception) {
            throw new InvalidPaginationException(exception.getMessage());
        }

        PageResult<Student> result = studentService.getStudentsPage(request, age);

        return new PageResponseDTO<>(
                result.content()
                        .stream()
                        .map(StudentMapper::toResponseDTO)
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @ownershipAuthorizationService.isOwner(authentication.name, #id)")
    public ResponseEntity<StudentResponseDTO> getStudentById(@PathVariable("id") int id) {
        return studentService.getStudentById(id)
                .map(StudentMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    @PostMapping
    public ResponseEntity<StudentResponseDTO> addStudent(@Valid @RequestBody StudentRequestDTO request) {
        Student student = StudentMapper.toStudent(request);
        Student createdStudent = studentService.addStudent(student);

        return ResponseEntity.status(201)
                .body(StudentMapper.toResponseDTO(createdStudent));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> updateStudent(
            @PathVariable int id,
            @Valid @RequestBody StudentRequestDTO request) {
        Student student = StudentMapper.toStudent(request);

        return studentService.updateStudent(id, student)
                .map(StudentMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable int id) {
        if (studentService.deleteStudent(id)) {
            return ResponseEntity.noContent()
                    .build();
        }

        throw new ResourceNotFoundException("Student", id);
    }
}