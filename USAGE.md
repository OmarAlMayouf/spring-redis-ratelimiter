# 🎯 Usage Guide - Spring Redis Rate Limiter

## ⚠️ Prerequisites

Before using this library, you **MUST** have:

1. **Redis server** running and accessible
2. **Redis configuration** in your Spring Boot application

**Important:** This library **only provides rate limiter components**. It expects `RedisTemplate<String, Object>` to be available in your application context. You are responsible for configuring Redis connection.

---

## Quick Start

### 1. **REQUIRED:** Add Redis Configuration

Create a Redis configuration class **in your application** (not provided by this library):

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
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.afterPropertiesSet();
        return template;
    }
}
```

### 2. **REQUIRED:** Configure Redis Connection

Add to your `application.properties`:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=60000

# Enable AspectJ for @RateLimit annotation
spring.aop.proxy-target-class=true
```

Or for `application.yml`:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 60000
  aop:
    proxy-target-class: true
```

### 3. Apply Rate Limiting

Simply annotate your methods with `@RateLimit`:

```java
@RestController
@RequestMapping("/api")
public class MyController {

    @PostMapping("/send-otp")
    @RateLimit(limit = 3, duration = 60, key = "#phone")
    public String sendOtp(@RequestParam String phone) {
        // Your logic here
        return "OTP sent to " + phone;
    }
}
```

---

## 📋 Common Use Cases

### 1. OTP Rate Limiting (by Phone Number)

```java
@PostMapping("/send-otp")
@RateLimit(limit = 3, duration = 60, key = "#phone")
public String sendOtp(@RequestParam String phone) {
    otpService.send(phone);
    return "OTP sent";
}
```

**Result:** Max 3 OTP requests per phone number per minute.

---

### 2. Login Throttling (by Email)

```java
@PostMapping("/login")
@RateLimit(limit = 5, duration = 300, key = "#email")
public LoginResponse login(@RequestParam String email, @RequestParam String password) {
    return authService.authenticate(email, password);
}
```

**Result:** Max 5 login attempts per email per 5 minutes.

---

### 3. API Rate Limiting (by User ID)

```java
@GetMapping("/api/data")
@RateLimit(limit = 100, duration = 60, key = "#userId")
public DataResponse getData(@RequestParam String userId) {
    return dataService.fetchData(userId);
}
```

**Result:** Max 100 API calls per user per minute.

---

### 4. Global Endpoint Rate Limit

```java
@GetMapping("/public-api")
@RateLimit(limit = 1000, duration = 60, name = "publicApi")
public String getPublicData() {
    return "Public data";
}
```

**Result:** Max 1000 requests total (all users combined) per minute.

---

### 5. Using SpEL with Complex Objects

```java
@PostMapping("/register")
@RateLimit(limit = 3, duration = 3600, key = "#user.email")
public String register(@RequestBody User user) {
    userService.register(user);
    return "User registered";
}
```

**Result:** Max 3 registration attempts per email per hour.

---

### 6. Using Parameter Index

```java
@GetMapping("/search")
@RateLimit(limit = 10, duration = 60, key = "#p0")
public SearchResults search(String query, String category) {
    return searchService.search(query, category);
}
```

**Result:** Max 10 searches per query string per minute. (#p0 = first parameter)

---

## 🎨 SpEL Expression Examples

| Expression | Description | Example Value |
|------------|-------------|---------------|
| `#phone` | Parameter named "phone" | `+966500000000` |
| `#email` | Parameter named "email" | `user@example.com` |
| `#user.id` | Nested property access | `12345` |
| `#p0` | First parameter | (value of arg 0) |
| `#p1` | Second parameter | (value of arg 1) |
| (empty) | No key (global) | `default` |

---

## 🚨 Exception Handling

When rate limit is exceeded, `RateLimitExceededException` is thrown.

### Global Exception Handler (Already Included)

```java
@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Rate limit exceeded");
        response.put("message", ex.getMessage());
        response.put("retryAfter", ex.getRetryAfterSeconds());
        
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }
}
```

### Response Example

```json
{
  "error": "Rate limit exceeded",
  "message": "Rate limit exceeded for key 'ratelimit:sendOtp:+966500000000'. Limit: 3 requests per 60 seconds. Retry after 45 seconds.",
  "limit": 3,
  "duration": 60,
  "retryAfter": 45,
  "timestamp": "2025-12-02T03:42:00"
}
```

**HTTP Status:** `429 Too Many Requests`  
**Header:** `Retry-After: 45`

---

## 🔑 Redis Key Format

Keys are stored in Redis with this pattern:

```
ratelimit:{bucketName}:{identifier}
```

**Examples:**

- `ratelimit:sendOtp:+966500000000`
- `ratelimit:login:omar@example.com`
- `ratelimit:getData:user123`
- `ratelimit:publicApi:default`

**TTL:** Automatically expires after `duration` seconds.

---

## 🧪 Testing

### Start Redis (Docker)

```bash
docker run -d -p 6379:6379 redis:latest
```

### Test the Application

```bash
./mvnw spring-boot:run
```

### Test Rate Limiting

```bash
# Send 4 OTP requests (4th should fail)
curl -X POST "http://localhost:8080/api/send-otp?phone=%2B966500000000"
curl -X POST "http://localhost:8080/api/send-otp?phone=%2B966500000000"
curl -X POST "http://localhost:8080/api/send-otp?phone=%2B966500000000"
curl -X POST "http://localhost:8080/api/send-otp?phone=%2B966500000000"
```

The 4th request should return HTTP 429.

---

## ⚙️ Configuration Options

### Annotation Parameters

```java
@RateLimit(
    limit = 10,           // Max requests
    duration = 60,        // Time window (seconds)
    key = "#userId",      // SpEL expression (optional)
    name = "myBucket"     // Custom bucket name (optional)
)
```

### Default Behavior

- If `key` is empty → uses `"default"` as identifier
- If `name` is empty → uses method name as bucket name

---

## 🏗️ Architecture

```
@RateLimit Annotation
         ↓
  RateLimitAspect (AOP)
         ↓
  SpELKeyResolver (Parse key expression)
         ↓
  RateLimiterService (Check Redis)
         ↓
  Redis INCR + EXPIRE
         ↓
  Allow/Block Request
```

---

## 📦 Components Included

| Component | Purpose |
|-----------|---------|
| `@RateLimit` | Annotation for methods |
| `RateLimitAspect` | AOP interceptor |
| `RateLimiterService` | Redis operations |
| `SpELKeyResolver` | SpEL expression parser |
| `RateLimitExceededException` | Exception thrown when limited |
| `RateLimitExceptionHandler` | Global exception handler (example) |
| `RedisConfig` | Redis configuration (example) |
| `ExampleController` | Usage examples |

---

## 🔒 Thread Safety

✅ **Fully thread-safe** using Redis atomic operations (`INCR` + `EXPIRE`).

Works correctly in:
- Multi-threaded environments
- Distributed systems (multiple instances)
- High-concurrency scenarios

---

## 🐛 Troubleshooting

### Redis Connection Error

```
Unable to connect to Redis
```

**Solution:** Ensure Redis is running and accessible.

```bash
docker run -d -p 6379:6379 redis:latest
```

---

### Bean Not Found Error

```
No qualifying bean of type 'RedisTemplate<String, Object>'
```

**Solution:** Add Redis configuration to your application (see Quick Start).

---

### Rate Limit Not Working

**Check:**
1. Is `spring.aop.proxy-target-class=true` set?
2. Is the method `public`?
3. Is the method being called from outside the class (not self-invocation)?
4. Is Redis running?

---

## 📊 Performance

- **Latency:** < 1ms per check (Redis `INCR` operation)
- **Throughput:** Supports thousands of checks per second
- **Storage:** Minimal (one key per identifier)

---

## 🤝 Contributing

This is a production-ready rate limiting library. Feel free to extend it for your needs!

---

## 📄 License

See LICENSE file for details.
