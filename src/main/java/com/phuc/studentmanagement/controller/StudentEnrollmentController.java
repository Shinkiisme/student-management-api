package com.phuc.studentmanagement.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.phuc.studentmanagement.dto.EnrollmentResponseDTO;
import com.phuc.studentmanagement.mapper.EnrollmentMapper;
import com.phuc.studentmanagement.service.EnrollmentService;

@RestController
@RequestMapping("/students/{studentId}/enrollments")
public class StudentEnrollmentController {
    private final EnrollmentService enrollmentService;

    public StudentEnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @ownershipAuthorizationService.isOwner(authentication.name, #studentId)")
    public List<EnrollmentResponseDTO> getEnrollmentsByStudentId(@PathVariable("studentId") int studentId) {
        return enrollmentService.getEnrollmentsByStudentId(studentId)
                .stream()
                .map(EnrollmentMapper::toResponseDTO)
                .toList();
    }
}