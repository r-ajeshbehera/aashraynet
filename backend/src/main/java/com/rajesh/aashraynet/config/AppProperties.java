package com.rajesh.aashraynet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Typed view of the "app.*" settings in application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, RateLimit rateLimit, Nearest nearest) {
    public record Jwt(String secret, long expirationMinutes) { }
    public record Cors(List<String> allowedOrigins) { }
    public record RateLimit(int publicPerMinute, int loginPerMinute) { }
    public record Nearest(double defaultRadiusKm) { }
}
