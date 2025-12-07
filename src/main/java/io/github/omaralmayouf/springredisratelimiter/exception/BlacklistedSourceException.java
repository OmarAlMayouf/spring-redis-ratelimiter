package io.github.omaralmayouf.springredisratelimiter.exception;

/**
 * Exception thrown when a request comes from a blacklisted source.
 */
public class BlacklistedSourceException extends RuntimeException {

    private final String identifier;
    private final String bucketName;

    public BlacklistedSourceException(String identifier, String bucketName) {
        super(String.format("Access denied. Identifier '%s' is blacklisted for bucket '%s'.", identifier, bucketName));
        this.identifier = identifier;
        this.bucketName = bucketName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getBucketName() {
        return bucketName;
    }
}
