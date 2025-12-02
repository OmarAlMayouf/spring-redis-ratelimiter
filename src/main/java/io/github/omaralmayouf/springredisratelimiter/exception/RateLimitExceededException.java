package io.github.omaralmayouf.springredisratelimiter.exception;

/**
 * Exception thrown when a rate limit is exceeded.
 */
public class RateLimitExceededException extends RuntimeException {

    private final String key;
    private final int limit;
    private final int duration;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String key, int limit, int duration, long retryAfterSeconds) {
        super(String.format("Rate limit exceeded for key '%s'. Limit: %d requests per %d seconds. Retry after %d seconds.",
                key, limit, duration, retryAfterSeconds));
        this.key = key;
        this.limit = limit;
        this.duration = duration;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getKey() {
        return key;
    }

    public int getLimit() {
        return limit;
    }

    public int getDuration() {
        return duration;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

