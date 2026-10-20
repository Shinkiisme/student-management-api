package com.phuc.studentmanagement.model;

import java.time.Instant;

public class RefreshToken {
    private long id;
    private int userId;
    private String tokenHash;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant revokedAt;

    public RefreshToken(
            long id,
            int userId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public long getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}