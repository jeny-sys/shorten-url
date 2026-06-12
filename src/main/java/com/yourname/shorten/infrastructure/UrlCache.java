package com.yourname.shorten.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class UrlCache {

    private static final String URL_KEY = "url:";
    private static final String NULL_KEY = "null:";

    private final StringRedisTemplate redis;
    private final long ttlSeconds;
    private final long jitterSeconds;
    private final long nullTtlSeconds;

    public UrlCache(StringRedisTemplate redis,
                    @Value("${app.cache.ttl-seconds:86400}") long ttlSeconds,
                    @Value("${app.cache.ttl-jitter-seconds:600}") long jitterSeconds,
                    @Value("${app.cache.null-ttl-seconds:300}") long nullTtlSeconds) {
        this.redis = redis;
        this.ttlSeconds = ttlSeconds;
        this.jitterSeconds = jitterSeconds;
        this.nullTtlSeconds = nullTtlSeconds;
    }

    public Optional<String> get(String shortCode) {
        return Optional.ofNullable(redis.opsForValue().get(URL_KEY + shortCode));
    }

    public void put(String shortCode, String longUrl) {
        long jitter = jitterSeconds == 0 ? 0 : ThreadLocalRandom.current().nextLong(0, jitterSeconds);
        redis.opsForValue().set(URL_KEY + shortCode, longUrl, Duration.ofSeconds(ttlSeconds + jitter));
    }

    public void putNull(String shortCode) {
        redis.opsForValue().set(NULL_KEY + shortCode, "1", Duration.ofSeconds(nullTtlSeconds));
    }

    public boolean isKnownMissing(String shortCode) {
        return Boolean.TRUE.equals(redis.hasKey(NULL_KEY + shortCode));
    }
}
