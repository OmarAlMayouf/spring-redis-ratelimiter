package io.github.omaralmayouf.springredisratelimiter.service;

import io.github.omaralmayouf.springredisratelimiter.config.RateLimiterProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing whitelist and blacklist functionality.
 * Supports both static configuration and dynamic Redis-based lists.
 */
@Service
public class WhitelistBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimiterProperties properties;

    public WhitelistBlacklistService(RedisTemplate<String, Object> redisTemplate,
                                     RateLimiterProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * Checks if an identifier is whitelisted (bypass rate limiting).
     * Checks both static configuration and Redis-based dynamic whitelist.
     *
     * @param identifier Unique identifier (e.g., IP, user ID, email)
     * @param bucketName Optional bucket-specific whitelist
     * @return true if whitelisted, false otherwise
     */
    public boolean isWhitelisted(String identifier, String bucketName) {
        if (!properties.isWhitelistBlacklistEnabled()) return false;

        // Check static whitelist
        if (properties.getStaticWhitelist().contains(identifier)) return true;

        // Check Redis dynamic whitelist if enabled
        if (properties.isUseDynamicLists()) {
            // Check global whitelist
            String globalKey = properties.getWhitelistKeyPrefix() + "global:" + identifier;

            if (Boolean.TRUE.equals(redisTemplate.hasKey(globalKey))) return true;

            // Check bucket-specific whitelist
            if (bucketName != null && !bucketName.isEmpty()) {
                String bucketKey = properties.getWhitelistKeyPrefix() + bucketName + ":" + identifier;
                if (Boolean.TRUE.equals(redisTemplate.hasKey(bucketKey))) return true;
            }
        }

        return false;
    }

    /**
     * Checks if an identifier is blacklisted (blocked completely).
     * Checks both static configuration and Redis-based dynamic blacklist.
     *
     * @param identifier Unique identifier (e.g., IP, user ID, email)
     * @param bucketName Optional bucket-specific blacklist
     * @return true if blacklisted, false otherwise
     */
    public boolean isBlacklisted(String identifier, String bucketName) {
        if (!properties.isWhitelistBlacklistEnabled()) return false;

        // Check static blacklist
        if (properties.getStaticBlacklist().contains(identifier)) return true;

        // Check Redis dynamic blacklist if enabled
        if (properties.isUseDynamicLists()) {
            // Check global blacklist
            String globalKey = properties.getBlacklistKeyPrefix() + "global:" + identifier;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(globalKey))) return true;

            // Check bucketspecific blacklist
            if (bucketName != null && !bucketName.isEmpty()) {
                String bucketKey = properties.getBlacklistKeyPrefix() + bucketName + ":" + identifier;
                if (Boolean.TRUE.equals(redisTemplate.hasKey(bucketKey))) return true;
            }
        }

        return false;
    }

    /**
     * Adds an identifier to the global whitelist in Redis.
     *
     * @param identifier Identifier to whitelist
     * @param ttlSeconds Time-to-live in seconds (0 for permanent)
     */
    public void addToWhitelist(String identifier, long ttlSeconds) {
        String key = properties.getWhitelistKeyPrefix() + "global:" + identifier;
        redisTemplate.opsForValue().set(key, "1");
        if (ttlSeconds > 0) redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Adds an identifier to a bucket-specific whitelist in Redis.
     *
     * @param identifier Identifier to whitelist
     * @param bucketName Bucket name
     * @param ttlSeconds Time-to-live in seconds (0 for permanent)
     */
    public void addToWhitelist(String identifier, String bucketName, long ttlSeconds) {
        String key = properties.getWhitelistKeyPrefix() + bucketName + ":" + identifier;
        redisTemplate.opsForValue().set(key, "1");
        if (ttlSeconds > 0) redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Removes an identifier from the global whitelist in Redis.
     *
     * @param identifier Identifier to remove
     */
    public void removeFromWhitelist(String identifier) {
        String key = properties.getWhitelistKeyPrefix() + "global:" + identifier;
        redisTemplate.delete(key);
    }

    /**
     * Removes an identifier from a bucket-specific whitelist in Redis.
     *
     * @param identifier Identifier to remove
     * @param bucketName Bucket name
     */
    public void removeFromWhitelist(String identifier, String bucketName) {
        String key = properties.getWhitelistKeyPrefix() + bucketName + ":" + identifier;
        redisTemplate.delete(key);
    }

    /**
     * Adds an identifier to the global blacklist in Redis.
     *
     * @param identifier Identifier to blacklist
     * @param ttlSeconds Time-to-live in seconds (0 for permanent)
     */
    public void addToBlacklist(String identifier, long ttlSeconds) {
        String key = properties.getBlacklistKeyPrefix() + "global:" + identifier;
        redisTemplate.opsForValue().set(key, "1");
        if (ttlSeconds > 0) redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Adds an identifier to a bucket-specific blacklist in Redis.
     *
     * @param identifier Identifier to blacklist
     * @param bucketName Bucket name
     * @param ttlSeconds Time-to-live in seconds (0 for permanent)
     */
    public void addToBlacklist(String identifier, String bucketName, long ttlSeconds) {
        String key = properties.getBlacklistKeyPrefix() + bucketName + ":" + identifier;
        redisTemplate.opsForValue().set(key, "1");
        if (ttlSeconds > 0) redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Removes an identifier from the global blacklist in Redis.
     *
     * @param identifier Identifier to remove
     */
    public void removeFromBlacklist(String identifier) {
        String key = properties.getBlacklistKeyPrefix() + "global:" + identifier;
        redisTemplate.delete(key);
    }

    /**
     * Removes an identifier from a bucket-specific blacklist in Redis.
     *
     * @param identifier Identifier to remove
     * @param bucketName Bucket name
     */
    public void removeFromBlacklist(String identifier, String bucketName) {
        String key = properties.getBlacklistKeyPrefix() + bucketName + ":" + identifier;
        redisTemplate.delete(key);
    }
}
