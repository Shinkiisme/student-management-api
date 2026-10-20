package com.phuc.studentmanagement.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.phuc.studentmanagement.service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private RefreshTokenCleanupScheduler scheduler;

    @Test
    void cleanup_shouldCallRefreshTokenService() {
        scheduler.cleanup();

        verify(refreshTokenService).cleanupObsoleteTokens();
    }
}