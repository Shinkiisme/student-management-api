package com.phuc.studentmanagement.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.phuc.studentmanagement.service.RefreshTokenService;

@Component
public class RefreshTokenCleanupScheduler {
    private final RefreshTokenService refreshTokenService;
    private final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);

    public RefreshTokenCleanupScheduler(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void cleanup() {
        int deletedCount = refreshTokenService.cleanupObsoleteTokens();

        log.info("Refresh token cleanup completed: {} tokens deleted", deletedCount);
    }
}