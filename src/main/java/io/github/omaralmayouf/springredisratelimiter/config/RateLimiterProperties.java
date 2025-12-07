package io.github.omaralmayouf.springredisratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration properties for rate limiter whitelist/blacklist.
 */
@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    /**
     * Enable or disable whitelist/blacklist feature globally.
     */
    private boolean whitelistBlacklistEnabled = true;

    /**
     * Redis key prefix for whitelist entries.
     */
    private String whitelistKeyPrefix = "ratelimit:whitelist:";

    /**
     * Redis key prefix for blacklist entries.
     */
    private String blacklistKeyPrefix = "ratelimit:blacklist:";

    /**
     * Static whitelist of identifiers that bypass rate limiting.
     */
    private Set<String> staticWhitelist = new HashSet<>();

    /**
     * Static blacklist of identifiers that are completely blocked.
     */
    private Set<String> staticBlacklist = new HashSet<>();

    /**
     * Whether to use Redis for dynamic whitelist/blacklist management.
     * If false, only static lists from properties are used.
     */
    private boolean useDynamicLists = true;

    public boolean isWhitelistBlacklistEnabled() {
        return whitelistBlacklistEnabled;
    }

    public void setWhitelistBlacklistEnabled(boolean whitelistBlacklistEnabled) {
        this.whitelistBlacklistEnabled = whitelistBlacklistEnabled;
    }

    public String getWhitelistKeyPrefix() {
        return whitelistKeyPrefix;
    }

    public void setWhitelistKeyPrefix(String whitelistKeyPrefix) {
        this.whitelistKeyPrefix = whitelistKeyPrefix;
    }

    public String getBlacklistKeyPrefix() {
        return blacklistKeyPrefix;
    }

    public void setBlacklistKeyPrefix(String blacklistKeyPrefix) {
        this.blacklistKeyPrefix = blacklistKeyPrefix;
    }

    public Set<String> getStaticWhitelist() {
        return staticWhitelist;
    }

    public void setStaticWhitelist(Set<String> staticWhitelist) {
        this.staticWhitelist = staticWhitelist;
    }

    public Set<String> getStaticBlacklist() {
        return staticBlacklist;
    }

    public void setStaticBlacklist(Set<String> staticBlacklist) {
        this.staticBlacklist = staticBlacklist;
    }

    public boolean isUseDynamicLists() {
        return useDynamicLists;
    }

    public void setUseDynamicLists(boolean useDynamicLists) {
        this.useDynamicLists = useDynamicLists;
    }
}

