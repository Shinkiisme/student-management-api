package com.phuc.studentmanagement.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.phuc.studentmanagement.config.PostgresTestConfiguration;
import com.phuc.studentmanagement.model.RefreshToken;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@Transactional
class RefreshTokenRepositoryIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private int userId;

    @BeforeEach
    void setUp() {
        String username = "refresh_test_" + UUID.randomUUID().toString().substring(0, 16);
        userId = jdbcTemplate.queryForObject("""
                INSERT INTO users (username, password_hash, role, student_id, enabled)
                VALUES (?, ?, 'ADMIN', NULL, TRUE)
                RETURNING id
                """, Integer.class, username, passwordEncoder.encode("TestPassword123!"));
    }

    @Test
    void insert_shouldPersistTokenAndReturnGeneratedFields() {
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        RefreshToken token = new RefreshToken(0L, userId, "a".repeat(64), null, expiresAt, null);

        RefreshToken created = refreshTokenRepository.insert(token);

        assertTrue(created.getId() > 0);
        assertEquals(userId, created.getUserId());
        assertEquals("a".repeat(64), created.getTokenHash());
        assertNotNull(created.getCreatedAt());
        assertEquals(expiresAt, created.getExpiresAt());
        assertNull(created.getRevokedAt());
    }

    @Test
    void findByTokenHash_shouldReturnToken_whenHashExists() {
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        RefreshToken created = refreshTokenRepository.insert(
                new RefreshToken(0L, userId, "b".repeat(64), null, expiresAt, null));

        Optional<RefreshToken> result = refreshTokenRepository.findByTokenHash("b".repeat(64));

        assertTrue(result.isPresent());
        assertEquals(created.getId(), result.get().getId());
        assertEquals(userId, result.get().getUserId());
        assertEquals("b".repeat(64), result.get().getTokenHash());
        assertEquals(expiresAt, result.get().getExpiresAt());
        assertNull(result.get().getRevokedAt());
    }

    @Test
    void findByTokenHash_shouldReturnEmpty_whenHashDoesNotExist() {
        Optional<RefreshToken> result = refreshTokenRepository.findByTokenHash("c".repeat(64));

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTokenHash_shouldMapRevokedAt_whenTokenIsRevoked() {
        RefreshToken created = refreshTokenRepository.insert(
                new RefreshToken(0L, userId, "d".repeat(64), null,
                        Instant.now().plus(7, ChronoUnit.DAYS), null));
        Instant revokedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        jdbcTemplate.update("""
                UPDATE refresh_tokens
                SET revoked_at = ?
                WHERE id = ?
                """, OffsetDateTime.ofInstant(revokedAt, ZoneOffset.UTC), created.getId());

        RefreshToken result = refreshTokenRepository.findByTokenHash("d".repeat(64)).orElseThrow();

        assertEquals(revokedAt, result.getRevokedAt());
    }

    @Test
    void insert_shouldRejectDuplicateTokenHash() {
        String tokenHash = "e".repeat(64);
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        refreshTokenRepository.insert(new RefreshToken(0L, userId, tokenHash, null, expiresAt, null));

        assertThrows(DuplicateKeyException.class, () -> refreshTokenRepository.insert(
                new RefreshToken(0L, userId, tokenHash, null, expiresAt, null)));
    }

    @Test
    void insert_shouldRejectUnknownUser() {
        RefreshToken token = new RefreshToken(
                0L, -1, "f".repeat(64), null,
                Instant.now().plus(7, ChronoUnit.DAYS), null);

        assertThrows(DataIntegrityViolationException.class, () -> refreshTokenRepository.insert(token));
    }

    @Test
    void revokeById_shouldSetRevokedAt_whenTokenIsActive() {
        RefreshToken created = refreshTokenRepository.insert(
                new RefreshToken(0L, userId, "g".repeat(64), null,
                        Instant.now().plus(7, ChronoUnit.DAYS), null));

        boolean revoked = refreshTokenRepository.revokeById(created.getId());

        assertTrue(revoked);
        RefreshToken result = refreshTokenRepository.findByTokenHash("g".repeat(64)).orElseThrow();
        assertNotNull(result.getRevokedAt());
    }

    @Test
    void revokeById_shouldReturnFalse_whenTokenIsAlreadyRevoked() {
        RefreshToken created = refreshTokenRepository.insert(
                new RefreshToken(0L, userId, "h".repeat(64), null,
                        Instant.now().plus(7, ChronoUnit.DAYS), null));

        assertTrue(refreshTokenRepository.revokeById(created.getId()));
        Instant firstRevokedAt = refreshTokenRepository.findByTokenHash("h".repeat(64))
                .orElseThrow().getRevokedAt();

        boolean revokedAgain = refreshTokenRepository.revokeById(created.getId());

        assertFalse(revokedAgain);
        Instant revokedAtAfterSecondAttempt = refreshTokenRepository.findByTokenHash("h".repeat(64))
                .orElseThrow().getRevokedAt();
        assertEquals(firstRevokedAt, revokedAtAfterSecondAttempt);
    }

    @Test
    void revokeById_shouldReturnFalse_whenTokenDoesNotExist() {
        boolean revoked = refreshTokenRepository.revokeById(-1L);

        assertFalse(revoked);
    }
}