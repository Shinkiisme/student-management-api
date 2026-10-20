package com.phuc.studentmanagement.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.phuc.studentmanagement.model.RefreshToken;

@Repository
public class RefreshTokenRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final RowMapper<RefreshToken> REFRESH_TOKEN_ROW_MAPPER = (rs, rowNum) -> {
        OffsetDateTime revokedAt = rs.getObject("revoked_at", OffsetDateTime.class);

        return new RefreshToken(
                rs.getLong("id"),
                rs.getInt("user_id"),
                rs.getString("token_hash"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
                revokedAt == null ? null : revokedAt.toInstant());
    };

    public RefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RefreshToken insert(RefreshToken token) {
        String sql = """
                INSERT INTO refresh_tokens(user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                RETURNING id, user_id, token_hash, created_at, expires_at, revoked_at
                """;

        return jdbcTemplate.queryForObject(
                sql,
                REFRESH_TOKEN_ROW_MAPPER,
                token.getUserId(),
                token.getTokenHash(),
                OffsetDateTime.ofInstant(token.getExpiresAt(), ZoneOffset.UTC));
    }

    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        String sql = """
                SELECT id, user_id, token_hash, created_at, expires_at, revoked_at
                FROM refresh_tokens
                WHERE token_hash = ?
                """;

        return jdbcTemplate.query(
                sql,
                REFRESH_TOKEN_ROW_MAPPER,
                tokenHash)
                .stream()
                .findFirst();
    }

    public boolean revokeById(long id) {
        String sql = """
                UPDATE refresh_tokens
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE id = ?
                    AND revoked_at IS NULL
                """;

        int affectedRow = jdbcTemplate.update(sql, id);

        return affectedRow == 1;
    }

    public int deleteObsolete(Instant cutoff) {
        String sql = """
                    DELETE FROM refresh_tokens
                    WHERE expires_at < ?
                        OR revoked_at < ?
                """;

        return jdbcTemplate.update(
                sql,
                OffsetDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(cutoff, ZoneOffset.UTC));
    }
}