package io.github.omaralmayouf.springredisratelimiter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for applying rate limiting to methods.
 * Uses Redis for distributed rate limiting across multiple instances.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of allowed requests within the duration window.
     */
    int limit();

    /**
     * Time window in seconds.
     */
    int duration();

    /**
     * SpEL expression for the rate limit key.
     * Examples: "#phone", "#email", "#user.id", "#p0"
     * If empty, uses method name as bucket identifier.
     */
    String key() default "";

    /**
     * Optional bucket name (default = method name).
     */
    String name() default "";
}

