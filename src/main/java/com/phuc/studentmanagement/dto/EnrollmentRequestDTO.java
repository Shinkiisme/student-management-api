package com.phuc.studentmanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EnrollmentRequestDTO(
        @NotNull(message = "Student ID is required") @Positive(message = "Student ID must be positive") Integer studentId,
        @NotNull(message = "Course ID is required") @Positive(message = "Course ID must be positive") Integer courseId) {
}