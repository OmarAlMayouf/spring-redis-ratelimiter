package io.github.omaralmayouf.springredisratelimiter.aspect;

import io.github.omaralmayouf.springredisratelimiter.annotation.RateLimit;
import io.github.omaralmayouf.springredisratelimiter.service.RateLimiterService;
import io.github.omaralmayouf.springredisratelimiter.util.SpELKeyResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect that intercepts methods annotated with @RateLimit
 * and enforces rate limiting rules.
 */
@Aspect
@Component
public class RateLimitAspect {

    private final RateLimiterService rateLimiterService;

    public RateLimitAspect(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // Determine bucket name (use annotation name or method name)
        String bucketName = rateLimit.name().isEmpty()
                ? method.getName()
                : rateLimit.name();

        // Resolve the identifier from SpEL expression
        String identifier = resolveIdentifier(rateLimit.key(), method, args);

        // Build Redis key
        String redisKey = rateLimiterService.buildKey(bucketName, identifier);

        // Check rate limit
        rateLimiterService.checkRateLimit(redisKey, rateLimit.limit(), rateLimit.duration());

        // If not exceeded, proceed with method execution
        return joinPoint.proceed();
    }

    /**
     * Resolves the identifier from the SpEL key expression.
     * Falls back to "default" if no key is specified.
     */
    private String resolveIdentifier(String keyExpression, Method method, Object[] args) {
        if (keyExpression == null || keyExpression.isEmpty()) {
            return "default";
        }

        String resolved = SpELKeyResolver.resolveKey(keyExpression, method, args);
        return resolved.isEmpty() ? "default" : resolved;
    }
}
