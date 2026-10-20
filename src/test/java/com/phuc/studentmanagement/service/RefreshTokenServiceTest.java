package com.phuc.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import com.phuc.studentmanagement.exception.InvalidRefreshTokenException;
import com.phuc.studentmanagement.model.RefreshToken;
import com.phuc.studentmanagement.model.User;
import com.phuc.studentmanagement.repository.RefreshTokenRepository;
import com.phuc.studentmanagement.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void issue_shouldReturnUrlSafeTokenOfExpectedLength() {
        String rawToken = refreshTokenService.issue(1);

        assertEquals(43, rawToken.length());
        assertTrue(rawToken.matches("[A-Za-z0-9_-]{43}"));
        verify(refreshTokenRepository).insert(any(RefreshToken.class));
    }

    @Test
    void issue_shouldStoreSha256HashInsteadOfRawToken() throws Exception {
        String rawToken = refreshTokenService.issue(1);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).insert(captor.capture());

        String expectedHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(rawToken.getBytes(StandardCharsets.UTF_8)));

        RefreshToken storedToken = captor.getValue();
        assertEquals(expectedHash, storedToken.getTokenHash());
        assertEquals(64, storedToken.getTokenHash().length());
        assertNotEquals(rawToken, storedToken.getTokenHash());
    }

    @Test
    void issue_shouldAssociateTokenWithUser() {
        refreshTokenService.issue(42);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).insert(captor.capture());

        assertEquals(42, captor.getValue().getUserId());
    }

    @Test
    void issue_shouldSetExpirationToSevenDaysFromCreation() {
        Instant earliestExpiration = Instant.now().plus(Duration.ofDays(7));
        refreshTokenService.issue(1);
        Instant latestExpiration = Instant.now().plus(Duration.ofDays(7));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).insert(captor.capture());

        RefreshToken storedToken = captor.getValue();
        assertFalse(storedToken.getExpiresAt().isBefore(earliestExpiration));
        assertFalse(storedToken.getExpiresAt().isAfter(latestExpiration));
        assertEquals(0L, storedToken.getId());
        assertNull(storedToken.getCreatedAt());
        assertNull(storedToken.getRevokedAt());
    }

    @Test
    void issue_shouldGenerateDifferentTokensForSeparateCalls() {
        String first = refreshTokenService.issue(1);
        String second = refreshTokenService.issue(1);

        assertNotEquals(first, second);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertNotEquals(captor.getAllValues().get(0).getTokenHash(),
                captor.getAllValues().get(1).getTokenHash());
    }

    @Test
    void issue_shouldPropagatePersistenceFailure() {
        when(refreshTokenRepository.insert(any(RefreshToken.class)))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> refreshTokenService.issue(1));
    }

    @Test
    void rotate_shouldRevokeOldTokenAndIssueNewToken() throws Exception {
        String rawToken = "existing-refresh-token";
        String oldHash = sha256(rawToken);
        RefreshToken oldToken = activeToken(101L, 7, oldHash);
        User user = enabledUser();

        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(oldToken));
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.revokeById(101L)).thenReturn(true);

        RefreshTokenRotationResult rotation = refreshTokenService.rotate(rawToken);
        String newRawToken = rotation.refreshToken();

        assertEquals(7, rotation.userId());

        assertNotEquals(rawToken, newRawToken);
        assertEquals(43, newRawToken.length());
        assertTrue(newRawToken.matches("[A-Za-z0-9_-]{43}"));
        verify(refreshTokenRepository).findByTokenHash(oldHash);
        verify(refreshTokenRepository).revokeById(101L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).insert(captor.capture());
        assertEquals(7, captor.getValue().getUserId());
        assertEquals(sha256(newRawToken), captor.getValue().getTokenHash());
        assertNotEquals(oldHash, captor.getValue().getTokenHash());
    }

    @Test
    void rotate_shouldRejectUnknownToken() throws Exception {
        String rawToken = "unknown-refresh-token";
        String tokenHash = sha256(rawToken);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(rawToken));

        verifyNoInteractions(userRepository);
        verify(refreshTokenRepository, never()).revokeById(anyLong());
        verify(refreshTokenRepository, never()).insert(any(RefreshToken.class));
    }

    @Test
    void rotate_shouldRejectRevokedToken() throws Exception {
        String rawToken = "revoked-refresh-token";
        String tokenHash = sha256(rawToken);
        RefreshToken token = new RefreshToken(
                102L, 8, tokenHash, Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600), Instant.now().minusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
        User user = enabledUser();
        when(userRepository.findById(8)).thenReturn(Optional.of(user));

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(rawToken));

        verify(refreshTokenRepository, never()).revokeById(anyLong());
        verify(refreshTokenRepository, never()).insert(any(RefreshToken.class));
    }

    @Test
    void rotate_shouldRejectExpiredToken() throws Exception {
        String rawToken = "expired-refresh-token";
        String tokenHash = sha256(rawToken);
        RefreshToken token = new RefreshToken(
                103L, 8, tokenHash, Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(60), null);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
        User user = enabledUser();
        when(userRepository.findById(8)).thenReturn(Optional.of(user));

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(rawToken));

        verify(refreshTokenRepository, never()).revokeById(anyLong());
        verify(refreshTokenRepository, never()).insert(any(RefreshToken.class));
    }

    @Test
    void rotate_shouldRejectTokenWhenUserDoesNotExist() throws Exception {
        String rawToken = "missing-user-refresh-token";
        String tokenHash = sha256(rawToken);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(activeToken(104L, 99, tokenHash)));
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(rawToken));

        verify(refreshTokenRepository, never()).revokeById(anyLong());
        verify(refreshTokenRepository, never()).insert(any(RefreshToken.class));
    }

    @Test
    void rotate_shouldRejectTokenWhenUserIsDisabled() throws Exception {
        String rawToken = "disabled-user-refresh-token";
        String tokenHash = sha256(rawToken);
        User user = mock(User.class);
        when(user.isEnabled()).thenReturn(false);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(activeToken(105L, 9, tokenHash)));
        when(userRepository.findById(9)).thenReturn(Optional.of(user));

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(rawToken));

        verify(refreshTokenRepository, never()).revokeById(anyLong());
        verify(refreshTokenRepository, never()).insert(any(RefreshToken.class));
    }

    @Test
    void rotate_shouldRejectTokenWhenRevocationFails() throws Exception {
        String rawToken = "concurrent-refresh-token";
        String tokenHash = sha256(rawToken);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(activeToken(106L, 10, tokenHash)));
        User user = enabledUser();
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.revokeById(106L)).thenReturn(false);

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(rawToken));

        verify(refreshTokenRepository).revokeById(106L);
        verify(refreshTokenRepository, never()).insert(any(RefreshToken.class));
    }

    @Test
    void rotate_shouldPropagateFailureWhenIssuingReplacementToken() throws Exception {
        String rawToken = "database-failure-refresh-token";
        String tokenHash = sha256(rawToken);
        when(refreshTokenRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(activeToken(107L, 11, tokenHash)));
        User user = enabledUser();
        when(userRepository.findById(11)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.revokeById(107L)).thenReturn(true);
        when(refreshTokenRepository.insert(any(RefreshToken.class)))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> refreshTokenService.rotate(rawToken));

        verify(refreshTokenRepository).revokeById(107L);
        verify(refreshTokenRepository).insert(any(RefreshToken.class));
    }

    @Test
    void cleanupObsoleteTokens_shouldUseCutoffThirtyDaysAgo() {
        Instant earliestCutoff = Instant.now().minus(Duration.ofDays(30));

        refreshTokenService.cleanupObsoleteTokens();

        Instant latestCutoff = Instant.now().minus(Duration.ofDays(30));
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteObsolete(captor.capture());

        Instant actualCutoff = captor.getValue();
        assertFalse(actualCutoff.isBefore(earliestCutoff));
        assertFalse(actualCutoff.isAfter(latestCutoff));
    }

    @Test
    void cleanupObsoleteTokens_shouldReturnDeletedCount() {
        when(refreshTokenRepository.deleteObsolete(any(Instant.class))).thenReturn(7);

        int deletedCount = refreshTokenService.cleanupObsoleteTokens();

        assertEquals(7, deletedCount);
        verify(refreshTokenRepository).deleteObsolete(any(Instant.class));
    }

    @Test
    void cleanupObsoleteTokens_shouldReturnZero_whenNoTokensAreDeleted() {
        int deletedCount = refreshTokenService.cleanupObsoleteTokens();

        assertEquals(0, deletedCount);
        verify(refreshTokenRepository).deleteObsolete(any(Instant.class));
    }

    @Test
    void cleanupObsoleteTokens_shouldPropagateRepositoryFailure() {
        when(refreshTokenRepository.deleteObsolete(any(Instant.class)))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> refreshTokenService.cleanupObsoleteTokens());
    }

    private RefreshToken activeToken(long id, int userId, String hash) {
        return new RefreshToken(
                id, userId, hash, Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600), null);
    }

    private User enabledUser() {
        User user = mock(User.class);
        when(user.isEnabled()).thenReturn(true);
        return user;
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}