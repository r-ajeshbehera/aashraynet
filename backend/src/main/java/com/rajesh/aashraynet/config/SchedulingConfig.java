package com.rajesh.aashraynet.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Jobs can be switched off per environment (e.g. in tests) with app.jobs.enabled=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
