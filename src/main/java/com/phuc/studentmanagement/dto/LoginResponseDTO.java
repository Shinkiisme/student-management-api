package com.phuc.studentmanagement.dto;

public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn) {
}