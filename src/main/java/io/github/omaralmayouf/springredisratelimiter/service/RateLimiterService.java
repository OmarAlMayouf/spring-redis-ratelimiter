package io.github.omaralmayouf.springredisratelimiter.service;

import io.github.omaralmayouf.springredisratelimiter.exception.RateLimitExceededException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for rate limiting using Redis atomic operations.
 */
@Service
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimiterService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if the request is allowed based on the rate limit.
     * Uses Redis INCR and EXPIRE for atomic fixed-window rate limiting.
     *
     * @param key      Redis key (format: ratelimit:{bucketName}:{identifier})
     * @param limit    Maximum allowed requests
     * @param duration Time window in seconds
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkRateLimit(String key, int limit, int duration) {
        // Increment the counter atomically
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            throw new IllegalStateException("Failed to increment rate limit counter for key: " + key);
        }

        // If this is the first request, set the expiration
        if (count == 1) {
            redisTemplate.expire(key, duration, TimeUnit.SECONDS);
        }

        // Check if limit is exceeded
        if (count > limit) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            long retryAfter = (ttl != null && ttl > 0) ? ttl : duration;
            throw new RateLimitExceededException(key, limit, duration, retryAfter);
        }
    }

    /**
     * Builds a Redis key for rate limiting.
     *
     * @param bucketName Bucket name (usually method name)
     * @param identifier Unique identifier (e.g., phone number, email, IP)
     * @return Formatted Redis key
     */
    public String buildKey(String bucketName, String identifier) {
        return String.format("ratelimit:%s:%s", bucketName, identifier);
    }
}

