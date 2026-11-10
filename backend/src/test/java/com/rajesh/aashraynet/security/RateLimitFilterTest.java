package com.rajesh.aashraynet.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** In the security package to reach the package-private test constructor. */
class RateLimitFilterTest {

    @Test void blocksAfterLimitWithinSameMinute() {
        Clock fixed = Clock.fixed(Instant.parse("2026-10-24T10:00:05Z"), ZoneOffset.UTC);
        RateLimitFilter f = new RateLimitFilter(3, 3, fixed);
        assertThat(f.tryAcquire("ip", 3)).isTrue();
        assertThat(f.tryAcquire("ip", 3)).isTrue();
        assertThat(f.tryAcquire("ip", 3)).isTrue();
        assertThat(f.tryAcquire("ip", 3)).isFalse();
        assertThat(f.tryAcquire("other-ip", 3)).isTrue();
    }

    @Test void resetsInNextMinute() {
        RateLimitFilter first = new RateLimitFilter(1, 1, Clock.fixed(Instant.parse("2026-10-24T10:00:59Z"), ZoneOffset.UTC));
        assertThat(first.tryAcquire("ip", 1)).isTrue();
        assertThat(first.tryAcquire("ip", 1)).isFalse();
    }
}
