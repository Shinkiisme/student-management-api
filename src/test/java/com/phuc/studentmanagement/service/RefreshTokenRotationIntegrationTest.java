package com.phuc.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.phuc.studentmanagement.config.PostgresTestConfiguration;
import com.phuc.studentmanagement.exception.InvalidRefreshTokenException;
import com.phuc.studentmanagement.model.RefreshToken;
import com.phuc.studentmanagement.repository.RefreshTokenRepository;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
class RefreshTokenRotationIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private int userId;

    @BeforeEach
    void setUp() {
        String username = "rotation_test_" + UUID.randomUUID().toString().substring(0, 16);
        userId = jdbcTemplate.queryForObject("""
                INSERT INTO users (username, password_hash, role, student_id, enabled)
                VALUES (?, ?, 'ADMIN', NULL, TRUE)
                RETURNING id
                """,
                Integer.class,
                username,
                passwordEncoder.encode("TestPassword123!"));
    }

    @Test
    void issue_shouldStoreOnlyHashAndAssociateTokenWithUser() {
        String rawToken = refreshTokenService.issue(userId);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken)).orElseThrow();

        assertEquals(43, rawToken.length());
        assertTrue(rawToken.matches("[A-Za-z0-9_-]{43}"));
        assertEquals(userId, stored.getUserId());
        assertEquals(64, stored.getTokenHash().length());
        assertNotEquals(rawToken, stored.getTokenHash());
        assertNotNull(stored.getCreatedAt());
        assertTrue(stored.getExpiresAt().isAfter(Instant.now()));
        assertNull(stored.getRevokedAt());
    }

    @Test
    void rotate_shouldRevokeOldTokenAndPersistNewToken() {
        String oldRawToken = refreshTokenService.issue(userId);
        RefreshTokenRotationResult rotation = refreshTokenService.rotate(oldRawToken);
        String newRawToken = rotation.refreshToken();

        assertEquals(userId, rotation.userId());

        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hash(oldRawToken)).orElseThrow();
        RefreshToken newToken = refreshTokenRepository.findByTokenHash(hash(newRawToken)).orElseThrow();

        assertNotEquals(oldRawToken, newRawToken);
        assertNotNull(oldToken.getRevokedAt());
        assertNull(newToken.getRevokedAt());
        assertEquals(userId, newToken.getUserId());
        assertTrue(newToken.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void rotate_shouldRejectReuseOfOldToken() {
        String oldRawToken = refreshTokenService.issue(userId);
        RefreshTokenRotationResult rotation = refreshTokenService.rotate(oldRawToken);
        String newRawToken = rotation.refreshToken();

        assertEquals(userId, rotation.userId());

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(oldRawToken));

        RefreshToken newToken = refreshTokenRepository.findByTokenHash(hash(newRawToken)).orElseThrow();
        assertNull(newToken.getRevokedAt());
    }

    @Test
    void rotate_shouldRejectUnknownToken() {
        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate("unknown-refresh-token"));
    }

    @Test
    void rotate_shouldRejectExpiredTokenWithoutRevokingIt() {
        String rawToken = "expired-refresh-token";
        refreshTokenRepository.insert(new RefreshToken(
                0L, userId, hash(rawToken), null, Instant.now().minusSeconds(60), null));

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(rawToken));

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken)).orElseThrow();
        assertNull(stored.getRevokedAt());
    }

    @Test
    void rotate_shouldRejectAlreadyRevokedToken() {
        String rawToken = refreshTokenService.issue(userId);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken)).orElseThrow();
        assertTrue(refreshTokenRepository.revokeById(stored.getId()));

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(rawToken));
    }

    @Test
    void rotate_shouldRejectDisabledUserWithoutRevokingToken() {
        String rawToken = refreshTokenService.issue(userId);
        jdbcTemplate.update("UPDATE users SET enabled = FALSE WHERE id = ?", userId);

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(rawToken));

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken)).orElseThrow();
        assertNull(stored.getRevokedAt());
    }

    @Test
    void rotate_shouldNotRevokeOtherActiveTokensOfSameUser() {
        String firstRawToken = refreshTokenService.issue(userId);
        String secondRawToken = refreshTokenService.issue(userId);

        RefreshTokenRotationResult rotation = refreshTokenService.rotate(firstRawToken);
        String replacement = rotation.refreshToken();

        assertEquals(userId, rotation.userId());

        assertNotNull(refreshTokenRepository.findByTokenHash(hash(firstRawToken))
                .orElseThrow().getRevokedAt());
        assertNull(refreshTokenRepository.findByTokenHash(hash(secondRawToken))
                .orElseThrow().getRevokedAt());
        assertFalse(replacement.equals(secondRawToken));
        assertNotNull(refreshTokenService.rotate(secondRawToken));
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}