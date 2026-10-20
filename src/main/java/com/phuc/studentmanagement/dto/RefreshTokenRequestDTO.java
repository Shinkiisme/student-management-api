package com.phuc.studentmanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(@NotBlank String refreshToken) {
}