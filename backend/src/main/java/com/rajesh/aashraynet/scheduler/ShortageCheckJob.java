package com.rajesh.aashraynet.scheduler;

import com.rajesh.aashraynet.service.ShortageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Hourly shortage detection. Idempotent: see ShortageService.detect(). Failures are logged, never rethrown. */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ShortageCheckJob {
    private static final Logger log = LoggerFactory.getLogger(ShortageCheckJob.class);
    private final ShortageService shortages;

    public ShortageCheckJob(ShortageService shortages) {
        this.shortages = shortages;
    }

    @Scheduled(cron = "${app.jobs.shortage-cron}")
    public void run() {
        try {
            shortages.detect();
        } catch (RuntimeException e) {
            log.error("Shortage check failed; it will run again at the next schedule", e);
        }
    }
}
