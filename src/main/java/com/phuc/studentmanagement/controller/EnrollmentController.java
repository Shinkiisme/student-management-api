package com.phuc.studentmanagement.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.phuc.studentmanagement.dto.EnrollmentRequestDTO;
import com.phuc.studentmanagement.dto.EnrollmentResponseDTO;
import com.phuc.studentmanagement.exception.ResourceNotFoundException;
import com.phuc.studentmanagement.mapper.EnrollmentMapper;
import com.phuc.studentmanagement.model.Enrollment;
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
    public List<EnrollmentResponseDTO> getAllEnrollments() {
        return enrollmentService.getAllEnrollments()
                .stream()
                .map(EnrollmentMapper::toResponseDTO)
                .toList();
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