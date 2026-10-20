package com.phuc.studentmanagement.dto;

public record LoginResponseDTO(
                String accessToken,
                String refreshToken,
                String tokenType,
                long expiresIn) {
}