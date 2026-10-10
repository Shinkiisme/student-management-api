package com.phuc.studentmanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseRequestDTO(
        @NotBlank(message = "Code cannot be empty") @Size(max = 20, message = "Code must not exceed 20 characters") String code,
        @NotBlank(message = "Name cannot be empty") @Size(max = 100, message = "Name must not exceed 100 characters") String name,
        @NotNull(message = "Credits cannot be empty") @Min(value = 1, message = "Credits must be greater than 0") Integer credits) {
}