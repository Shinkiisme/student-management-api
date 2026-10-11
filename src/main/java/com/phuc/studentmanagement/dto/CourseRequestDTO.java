package com.phuc.studentmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CourseRequestDTO(
                @NotBlank(message = "Code is required") @Size(max = 20, message = "Code must not exceed 20 characters") String code,
                @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must not exceed 100 characters") String name,
                @NotNull(message = "Credits are required") @Positive(message = "Credits must be positive") Integer credits) {
}