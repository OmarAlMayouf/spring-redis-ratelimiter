# Spring Redis Rate Limiter

An **Open-Source, lightweight, annotation-based rate limiting library** for Spring Boot using Redis to protect endpoints from abuse (OTP spam, login throttling, IP limiting, etc.).

> Please note that this library is under constant improvement

---

## Example use cases:
* Rate limiting per user, email, IP, or custom identifier
* Protecting login endpoints from brute-force attacks
* Throttling API requests in multi-instance deployments
* Whitelisting trusted sources
* Blacklisting specific sources

---

## Features

* **Annotation-based**: Add `@RateLimit` to any method
* **SpEL support**: Dynamic keys using `#phone`, `#email`, `#user.id`, `#p0`, etc.
* **Fixed-window algorithm**: Uses Redis atomic `INCR` + `EXPIRE`
* **Distributed-ready**: Works across multiple instances
* **Thread-safe**: High concurrency safe
* **Whitelist/Blacklist**: Skip rate limiting or block sources
* **Production-ready**: Exception handling + HTTP 429/403 responses

---

## Prerequisites

Before using:

1. Redis server running (local or remote)
2. Spring Boot application with `spring-boot-starter-data-redis`
3. Redis configuration in your application (library **does not** provide Redis config)

---

## Installation (v1.0.2)

**Maven**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.OmarAlMayouf</groupId>
        <artifactId>spring-redis-ratelimiter</artifactId>
        <version>v1.0.2</version>
    </dependency>

    <!-- Required: Spring Data Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
```

**Gradle**

```gradle
implementation 'io.github.OmarAlMayouf:spring-redis-ratelimiter:v1.0.2'
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

---

## Redis Configuration

**Option A: application.yml**

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 60000
  aop:
    proxy-target-class: true  # Required for @RateLimit

# Optional: Whitelist/Blacklist Configuration
rate-limiter:
  whitelist-blacklist-enabled: true
  use-dynamic-lists: true
  static-whitelist:
    - admin@company.com
    - internal-service
  static-blacklist:
    - blocked@example.com
    - banned-ip
```

**Option B: application.properties**

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.aop.proxy-target-class=true

# Optional: Whitelist/Blacklist Configuration
rate-limiter.whitelist-blacklist-enabled=true
rate-limiter.use-dynamic-lists=true
rate-limiter.static-whitelist=admin@company.com,internal-service
rate-limiter.static-blacklist=blocked@example.com,banned-ip
```

**Option C: Custom RedisConfig bean**

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.afterPropertiesSet();
        return template;
    }
}
```

---

## Component Scanning

```java
@SpringBootApplication(scanBasePackages = {
    "com.yourcompany.yourapp",  
    "io.github.omaralmayouf.springredisratelimiter"  
})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

---

## Usage

### Basic Rate Limiting

```java
@PostMapping("/send-otp")
@RateLimit(limit = 3, duration = 60, key = "#phone")
public String sendOtp(@RequestParam String phone) {
    otpService.send(phone);
    return "OTP sent to " + phone;
}
```

* Max 3 requests per phone per 60 seconds
* Automatically returns HTTP 429 if exceeded
* Works across multiple instances

### Whitelist/Blacklist

Control rate limiting behavior for specific identifiers:
- **Whitelist**: Bypass rate limiting for trusted sources
- **Blacklist**: Block access completely for specific sources

**Execution Priority:**
1. Blacklist Check → Returns HTTP 403 Forbidden
2. Whitelist Check → Bypasses rate limiting
3. Rate Limit Check → Normal rate limiting applies

**Example:**

```java
@RateLimit(limit = 10, duration = 60, key = "#userId")
@GetMapping("/api/data")
public String getData(@RequestParam String userId) {
    // Blacklisted sources → HTTP 403
    // Whitelisted sources → No rate limit
    // Others → Normal rate limiting
    return "Data for " + userId;
}
```

### Dynamic Management

```java
@Autowired
private WhitelistBlacklistService service;

// Add to whitelist (0 = permanent, >0 = TTL in seconds)
service.addToWhitelist("user@example.com", 0);
service.addToWhitelist("user@example.com", 86400); // 24 hours

// Add to blacklist
service.addToBlacklist("blocked@example.com", 0);
service.addToBlacklist("blocked@example.com", 3600); // 1 hour

// Endpoint-specific rules
service.addToWhitelist("partner", "externalApi", 604800);
service.addToBlacklist("user", "comments", 0);

// Remove from lists
service.removeFromWhitelist("user@example.com");
service.removeFromBlacklist("user@example.com");
```

---

## Configuration Options

### Rate Limiter Properties

| Property | Default | Description |
|----------|---------|-------------|
| `whitelist-blacklist-enabled` | `true` | Enable/disable whitelist/blacklist feature |
| `use-dynamic-lists` | `true` | Enable Redis-based dynamic lists |
| `whitelist-key-prefix` | `ratelimit:whitelist:` | Redis key prefix for whitelist |
| `blacklist-key-prefix` | `ratelimit:blacklist:` | Redis key prefix for blacklist |
| `static-whitelist` | `[]` | Static whitelist (comma-separated) |
| `static-blacklist` | `[]` | Static blacklist (comma-separated) |

---

## Exception Handling

### Default Responses

**Rate Limit Exceeded (HTTP 429):**
```json
{
  "error": "Rate limit exceeded",
  "message": "Rate limit exceeded for key 'ratelimit:api:user'. Limit: 10 requests per 60 seconds. Retry after 45 seconds.",
  "limit": 10,
  "duration": 60,
  "retryAfter": 45,
  "timestamp": "2025-12-07T15:30:00"
}
```

**Blacklisted Source (HTTP 403):**
```json
{
  "error": "Access denied",
  "message": "Access denied. Identifier 'user123' is blacklisted for bucket 'api'.",
  "identifier": "user123",
  "bucketName": "api",
  "timestamp": "2025-12-07T15:30:00"
}
```

### Custom Exception Handler

The library includes built-in exception handlers, but you can customize them:

```java
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Rate limit exceeded");
        response.put("message", ex.getMessage());
        response.put("limit", ex.getLimit());
        response.put("duration", ex.getDuration());
        response.put("retryAfter", ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                             .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                             .body(response);
    }
    
    @ExceptionHandler(BlacklistedSourceException.class)
    public ResponseEntity<Map<String, Object>> handleBlacklisted(BlacklistedSourceException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Access denied");
        response.put("message", "Your account has been suspended.");
        response.put("support", "contact@company.com");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}
```

---

## Checklist

* Redis server running (`docker run -d -p 6379:6379 redis:latest`)
* `spring-boot-starter-data-redis` added
* Redis configured in `application.yml` or via `RedisConfig`
* `spring.aop.proxy-target-class=true` set
* Component scanning includes `io.github.omaralmayouf.springredisratelimiter`
* `@RateLimit` annotation added to desired methods
* *(Optional)* Configure whitelist/blacklist in application.properties

---

## Examples

```java
// OTP: 3 per phone/minute
@RateLimit(limit = 3, duration = 60, key = "#phone")
@PostMapping("/send-otp")
public String sendOtp(String phone) { ... }

// Login: 5 per email/5 minutes
@RateLimit(limit = 5, duration = 300, key = "#email")
@PostMapping("/login")
public LoginResponse login(String email, String password) { ... }

// Global: 1000 requests/min
@RateLimit(limit = 1000, duration = 60)
@GetMapping("/public-api")
public String getPublicData() { ... }

// With whitelist/blacklist
@RateLimit(limit = 10, duration = 60, key = "#userId")
@GetMapping("/api/data")
public String getData(String userId) { 
    // Blacklisted → 403
    // Whitelisted → No limit
    // Others → 10 requests/minute
    return "data";
}
```

---

## Troubleshooting

| Issue                         | Solution                                                            |
| ----------------------------- | ------------------------------------------------------------------- |
| Redis connection failed       | Ensure Redis is running (`docker run -d -p 6379:6379 redis:latest`) |
| `@RateLimit` not working      | Set `spring.aop.proxy-target-class=true`                            |
| Bean not found: RedisTemplate | Add Spring Data Redis and configure RedisTemplate                   |
| Rate limit not applied        | Include library package in component scanning                       |
| Whitelist not working         | Check `rate-limiter.whitelist-blacklist-enabled=true` and verify configuration |
| Can't inject WhitelistBlacklistService | Ensure component scanning includes library package |

---

## Library Components

### Core Classes
- **`@RateLimit`** - Annotation for rate limiting
- **`RateLimiterService`** - Core rate limiting logic
- **`RateLimitAspect`** - AOP aspect interceptor
- **`RateLimitExceededException`** - Thrown when rate limit exceeded
- **`RateLimitExceptionHandler`** - Global exception handler

### Whitelist/Blacklist Classes
- **`WhitelistBlacklistService`** - Manage whitelist/blacklist
- **`RateLimiterProperties`** - Configuration properties
- **`BlacklistedSourceException`** - Thrown when source is blacklisted

### Utilities
- **`SpELKeyResolver`** - Resolves SpEL expressions

---

## Performance Notes

- **Static lists**: Zero Redis calls - checked in-memory
- **Dynamic lists**: 1-2 Redis operations per request
- **Blacklist check**: Fast-fail before rate limit computation
- **Whitelist check**: Saves rate limit computation for trusted sources
- **Thread-safe**: All operations are atomic and concurrent-safe

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

## License

This project is licensed under the MIT License.

---

## Author

**Omar AlMayouf**
- GitHub: [@OmarAlMayouf](https://github.com/OmarAlMayouf)

---

## Support

If you find this library helpful, please consider giving it a star on GitHub!

For issues or questions, please open an issue on the GitHub repository.
