package com.phuc.studentmanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudentRequestDTO(
                @NotBlank(message = "Name cannot be empty") String name,

                @NotNull(message = "Age is required") @Min(value = 1, message = "Age cannot be negative") Integer age) {
}