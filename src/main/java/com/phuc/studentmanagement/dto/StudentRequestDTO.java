package com.phuc.studentmanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentRequestDTO(
        @NotBlank(message = "Student code cannot be empty") @Size(max = 20, message = "Student code must not exceed 20 characters") String studentCode,

        @NotBlank(message = "Name cannot be empty") @Size(max = 100, message = "Name must not exceed 100 characters") String name,

        @NotNull(message = "Age is required") @Min(value = 0, message = "Age cannot be negative") Integer age) {
}