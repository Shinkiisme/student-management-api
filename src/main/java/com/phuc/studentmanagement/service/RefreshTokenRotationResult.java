package com.phuc.studentmanagement.service;

public record RefreshTokenRotationResult(
        int userId,
        String refreshToken) {
}