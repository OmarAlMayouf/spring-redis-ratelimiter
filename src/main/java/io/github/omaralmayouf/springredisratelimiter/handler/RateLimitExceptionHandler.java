package io.github.omaralmayouf.springredisratelimiter.handler;

import io.github.omaralmayouf.springredisratelimiter.exception.BlacklistedSourceException;
import io.github.omaralmayouf.springredisratelimiter.exception.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for rate limit exceeded and blacklist errors.
 * Returns HTTP 429 (Too Many Requests) for rate limits and HTTP 403 (Forbidden) for blacklisted sources.
 */
@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Rate limit exceeded");
        response.put("message", ex.getMessage());
        response.put("limit", ex.getLimit());
        response.put("duration", ex.getDuration());
        response.put("retryAfter", ex.getRetryAfterSeconds());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }

    @ExceptionHandler(BlacklistedSourceException.class)
    public ResponseEntity<Map<String, Object>> handleBlacklistedSource(BlacklistedSourceException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Access denied");
        response.put("message", ex.getMessage());
        response.put("identifier", ex.getIdentifier());
        response.put("bucketName", ex.getBucketName());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }
}
