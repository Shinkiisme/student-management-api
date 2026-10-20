package com.phuc.studentmanagement.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phuc.studentmanagement.exception.InvalidRefreshTokenException;
import com.phuc.studentmanagement.model.RefreshToken;
import com.phuc.studentmanagement.model.User;
import com.phuc.studentmanagement.repository.RefreshTokenRepository;
import com.phuc.studentmanagement.repository.UserRepository;

@Service
public class RefreshTokenService {
    private static final int TOKEN_SIZE_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public String issue(int userId) {
        String rawToken = generateToken();
        String hashedToken = hashToken(rawToken);

        Instant expiresAt = Instant
                .now()
                .plus(Duration.ofDays(7));

        RefreshToken refreshToken = new RefreshToken(
                0L,
                userId,
                hashedToken,
                null,
                expiresAt,
                null);

        refreshTokenRepository.insert(refreshToken);

        return rawToken;
    }

    @Transactional
    public RefreshTokenRotationResult rotate(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken token = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        User user = userRepository
                .findById(token.getUserId())
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        boolean isRevoked = token.getRevokedAt() != null;
        boolean isExpired = !Instant
                .now()
                .isBefore(token.getExpiresAt());
        boolean isDisabled = !user.isEnabled();

        if (isRevoked || isExpired || isDisabled) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        if (!refreshTokenRepository.revokeById(token.getId())) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        return new RefreshTokenRotationResult(
                token.getUserId(),
                issue(token.getUserId()));
    }

    @Transactional
    public void revoke(String rawToken) {
        String hashedToken = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hashedToken)
                .ifPresent(token -> refreshTokenRepository.revokeById(token.getId()));
    }

    private String generateToken() {
        byte[] randomBytes = new byte[TOKEN_SIZE_BYTES];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] input = rawToken.getBytes(StandardCharsets.UTF_8);
            byte[] hashBytes = digest.digest(input);

            return HexFormat.of()
                    .formatHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}