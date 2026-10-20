package com.phuc.studentmanagement.controller;

import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.phuc.studentmanagement.dto.EnrollmentResponseDTO;
import com.phuc.studentmanagement.dto.PageResponseDTO;
import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.mapper.EnrollmentMapper;
import com.phuc.studentmanagement.model.Enrollment;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;
import com.phuc.studentmanagement.pagination.SortDirection;
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
    public PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByStudentId(
            @PathVariable("studentId") int studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
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
}