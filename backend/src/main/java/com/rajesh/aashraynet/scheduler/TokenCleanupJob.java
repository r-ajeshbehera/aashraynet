package com.rajesh.aashraynet.scheduler;

import com.rajesh.aashraynet.repository.RevokedTokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Revoked tokens are only needed until they expire. */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TokenCleanupJob {
    private final RevokedTokenRepository revoked;

    public TokenCleanupJob(RevokedTokenRepository revoked) {
        this.revoked = revoked;
    }

    @Scheduled(cron = "${app.jobs.token-cleanup-cron}")
    @Transactional
    public void run() {
        revoked.deleteExpired(LocalDateTime.now(ZoneOffset.UTC));
    }
}
