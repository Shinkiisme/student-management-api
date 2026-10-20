package com.phuc.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.phuc.studentmanagement.config.PostgresTestConfiguration;
import com.phuc.studentmanagement.exception.InvalidRefreshTokenException;
import com.phuc.studentmanagement.model.RefreshToken;
import com.phuc.studentmanagement.repository.RefreshTokenRepository;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class RefreshTokenTransactionIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @MockitoSpyBean
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private int userId;
    private String originalRawToken;

    @BeforeEach
    void setUp() {
        String username = "rotation_tx_test_" + UUID.randomUUID().toString().substring(0, 16);
        userId = jdbcTemplate.queryForObject("""
                INSERT INTO users (username, password_hash, role, student_id, enabled)
                VALUES (?, ?, 'ADMIN', NULL, TRUE)
                RETURNING id
                """, Integer.class, username, passwordEncoder.encode("TestPassword123!"));

        originalRawToken = refreshTokenService.issue(userId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void rotate_shouldRollbackRevocation_whenReplacementInsertFails() {
        RefreshToken original = refreshTokenRepository.findByTokenHash(hash(originalRawToken))
                .orElseThrow();

        doThrow(new IllegalStateException("Simulated insert failure"))
                .when(refreshTokenRepository).insert(any(RefreshToken.class));

        assertThrows(IllegalStateException.class,
                () -> refreshTokenService.rotate(originalRawToken));

        OffsetDateTime revokedAt = jdbcTemplate.queryForObject(
                "SELECT revoked_at FROM refresh_tokens WHERE id = ?",
                OffsetDateTime.class,
                original.getId());

        Long tokenCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                Long.class,
                userId);

        assertNull(revokedAt);
        assertEquals(1L, tokenCount);
    }

    @Test
    void rotate_shouldAllowOnlyOneConcurrentUseOfSameToken() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<String> rotation = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to start");
            }
            try {
                return refreshTokenService.rotate(originalRawToken).refreshToken();
            } catch (InvalidRefreshTokenException exception) {
                return null;
            }
        };

        try {
            Future<String> first = executor.submit(rotation);
            Future<String> second = executor.submit(rotation);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            String firstReplacement = first.get(20, TimeUnit.SECONDS);
            String secondReplacement = second.get(20, TimeUnit.SECONDS);
            int successCount = (firstReplacement == null ? 0 : 1)
                    + (secondReplacement == null ? 0 : 1);

            assertEquals(1, successCount);

            String replacement = firstReplacement != null ? firstReplacement : secondReplacement;

            assertNotNull(refreshTokenRepository.findByTokenHash(hash(originalRawToken))
                    .orElseThrow().getRevokedAt());
            assertNull(refreshTokenRepository.findByTokenHash(hash(replacement))
                    .orElseThrow().getRevokedAt());

            Long tokenCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?",
                    Long.class,
                    userId);
            assertEquals(2L, tokenCount);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
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