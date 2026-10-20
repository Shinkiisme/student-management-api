package com.phuc.studentmanagement.controller;

import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.phuc.studentmanagement.dto.EnrollmentRequestDTO;
import com.phuc.studentmanagement.dto.EnrollmentResponseDTO;
import com.phuc.studentmanagement.dto.PageResponseDTO;
import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.exception.ResourceNotFoundException;
import com.phuc.studentmanagement.mapper.EnrollmentMapper;
import com.phuc.studentmanagement.model.Enrollment;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;
import com.phuc.studentmanagement.pagination.SortDirection;
import com.phuc.studentmanagement.service.EnrollmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public PageResponseDTO<EnrollmentResponseDTO> getAllEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Integer studentId) {

        if (studentId != null && studentId <= 0) {
            throw new InvalidPaginationException("Student ID must be positive");
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

        PageResult<Enrollment> result = enrollmentService.getEnrollmentsPage(request, studentId);

        return new PageResponseDTO<>(
                result.content()
                        .stream()
                        .map(EnrollmentMapper::toResponseDTO)
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponseDTO> getEnrollmentById(@PathVariable int id) {
        return enrollmentService.getEnrollmentById(id)
                .map(EnrollmentMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
    }

    @PostMapping
    public ResponseEntity<EnrollmentResponseDTO> addEnrollment(@Valid @RequestBody EnrollmentRequestDTO request) {
        Enrollment enrollment = EnrollmentMapper.toEnrollment(request);
        Enrollment createdEnrollment = enrollmentService.addEnrollment(enrollment);

        return ResponseEntity.status(201).body(EnrollmentMapper.toResponseDTO(createdEnrollment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnrollment(@PathVariable int id) {
        if (enrollmentService.deleteEnrollment(id)) {
            return ResponseEntity.noContent().build();
        }

        throw new ResourceNotFoundException("Enrollment", id);
    }
}