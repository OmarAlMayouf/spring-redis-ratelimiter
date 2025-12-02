# 📦 **spring-redis-ratelimiter**

A lightweight, annotation-based rate limiting library for **Spring Boot** that uses **Redis** to protect endpoints from abuse (e.g., OTP spam, login throttling, IP limiting).

---

## 🚀 Features

* ✅ **Annotation-based** - Simply add `@RateLimit` to any method
* ✅ **SpEL support** - Dynamic keys using `#phone`, `#email`, `#user.id`, `#p0`, etc.
* ✅ **Fixed-window algorithm** - Uses Redis atomic operations (INCR + EXPIRE)
* ✅ **Distributed-ready** - Works across multiple application instances
* ✅ **Thread-safe** - Handles high-concurrency scenarios
* ✅ **Production-ready** - Complete with exception handling and HTTP 429 responses

---

## ⚠️ Prerequisites

Before using this library, you **MUST** have:

1. **Redis server** running and accessible
2. **Redis configuration** in your Spring Boot application

This library **only provides rate limiter components**. It expects `RedisTemplate<String, Object>` to be available in your application context.

---

## 📦 Installation

### Maven

```xml
<dependency>
    <groupId>io.github.OmarAlMayouf</groupId>
    <artifactId>spring-redis-ratelimiter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<!-- Required: Spring Data Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Gradle

```gradle
dependencies {
    implementation 'io.github.OmarAlMayouf:spring-redis-ratelimiter:0.0.1-SNAPSHOT'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

---

## 🎯 Quick Start

### 1. **REQUIRED:** Configure Redis in Your Application

You **must** provide your own Redis configuration. Create a configuration class:

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

### 2. **REQUIRED:** Configure Redis Connection Properties

Add to your `application.properties` or `application.yml`:

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

**Result:** Maximum 3 OTP requests per phone number per minute! 🎉

---

## 📋 Common Use Cases

### 1️⃣ OTP Rate Limiting (by Phone Number)

```java
@PostMapping("/send-otp")
@RateLimit(limit = 3, duration = 60, key = "#phone")
public String sendOtp(@RequestParam String phone) {
    otpService.send(phone);
    return "OTP sent";
}
```

**Limit:** 3 requests per phone number per minute.

---

### 2️⃣ Login Throttling (by Email)

```java
@PostMapping("/login")
@RateLimit(limit = 5, duration = 300, key = "#email")
public LoginResponse login(@RequestParam String email, @RequestParam String password) {
    return authService.authenticate(email, password);
}
```

**Limit:** 5 login attempts per email per 5 minutes.

---

### 3️⃣ API Rate Limiting (by User ID)

```java
@GetMapping("/api/data")
@RateLimit(limit = 100, duration = 60, key = "#userId")
public DataResponse getData(@RequestParam String userId) {
    return dataService.fetchData(userId);
}
```

**Limit:** 100 API calls per user per minute.

---

### 4️⃣ Global Endpoint Rate Limit

```java
@GetMapping("/public-api")
@RateLimit(limit = 1000, duration = 60, name = "publicApi")
public String getPublicData() {
    return "Public data";
}
```

**Limit:** 1000 requests total (all users combined) per minute.

---

### 5️⃣ Using SpEL with Complex Objects

```java
@PostMapping("/register")
@RateLimit(limit = 3, duration = 3600, key = "#user.email")
public String register(@RequestBody User user) {
    userService.register(user);
    return "User registered";
}
```

**Limit:** 3 registration attempts per email per hour.

---

### 6️⃣ Using Parameter Index

```java
@GetMapping("/search")
@RateLimit(limit = 10, duration = 60, key = "#p0")
public SearchResults search(String query, String category) {
    return searchService.search(query, category);
}
```

**Limit:** 10 searches per query string per minute. (#p0 = first parameter)

---

## 🎨 SpEL Expression Examples

| Expression | Description | Example Value |
|------------|-------------|---------------|
| `#phone` | Parameter named "phone" | `+966500000000` |
| `#email` | Parameter named "email" | `user@example.com` |
| `#user.id` | Nested property access | `12345` |
| `#user.email` | Nested property access | `user@example.com` |
| `#p0` | First parameter | (value of arg 0) |
| `#p1` | Second parameter | (value of arg 1) |
| (empty) | No key (global) | `default` |

---

## ⚙️ Annotation Parameters

```java
@RateLimit(
    limit = 10,           // Required: Max requests
    duration = 60,        // Required: Time window (seconds)
    key = "#userId",      // Optional: SpEL expression
    name = "myBucket"     // Optional: Custom bucket name
)
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | int | ✅ | Maximum number of allowed requests |
| `duration` | int | ✅ | Time window in seconds |
| `key` | String | ❌ | SpEL expression for dynamic identifier |
| `name` | String | ❌ | Custom bucket name (default: method name) |

### Default Behavior

- If `key` is empty → uses `"default"` as identifier
- If `name` is empty → uses method name as bucket name

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

## 🧩 How It Works

The library uses Redis atomic operations for thread-safe rate limiting:

```
1. count = INCR(redisKey)
2. if count == 1:
      EXPIRE(redisKey, duration)
3. if count > limit:
      THROW RateLimitExceededException
   else:
      ALLOW REQUEST
```

This approach ensures:
- ⚡ **Fast** - Single Redis operation per request
- 🔒 **Thread-safe** - Atomic INCR operation
- 🌐 **Distributed** - Works across multiple instances
- 🎯 **Accurate** - No race conditions

---

## 🚨 Exception Handling

When rate limit is exceeded, `RateLimitExceededException` is thrown.

### Global Exception Handler (Recommended)

```java
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
}
```

### Error Response Example

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

## 🧱 Architecture

```
@RateLimit Annotation
         ↓
  RateLimitAspect (AOP Interceptor)
         ↓
  SpELKeyResolver (Parse SpEL expression)
         ↓
  RateLimiterService (Check Redis)
         ↓
  Redis: INCR + EXPIRE
         ↓
  Allow or Block Request
```

### Library Structure

```
spring-redis-ratelimiter/
 ├── annotation/
 │     └── RateLimit.java               # @RateLimit annotation
 ├── aspect/
 │     └── RateLimitAspect.java         # AOP interceptor
 ├── service/
 │     └── RateLimiterService.java      # Redis operations
 ├── exception/
 │     └── RateLimitExceededException.java
 └── util/
       └── SpELKeyResolver.java         # SpEL parser
```

---

## 🧪 Testing

### 1. Start Redis (Docker)

```bash
docker run -d -p 6379:6379 redis:latest
```

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

### 3. Test Rate Limiting

```bash
# Send 4 OTP requests (4th should fail with HTTP 429)
curl -X POST "http://localhost:8080/api/send-otp?phone=%2B966500000000"
curl -X POST "http://localhost:8080/api/send-otp?phone=%2B966500000000"
curl -X POST "http://localhost:8080/api/send-otp?phone=%2B966500000000"
curl -X POST "http://localhost:8080/api/send-otp?phone=%2B966500000000"
```

The 4th request will return **HTTP 429 Too Many Requests**.

---

## 🐛 Troubleshooting

### ❌ Redis Connection Error

```
Unable to connect to Redis
```

**Solution:** Ensure Redis is running and accessible.

```bash
docker run -d -p 6379:6379 redis:latest
```

---

### ❌ Bean Not Found Error

```
No qualifying bean of type 'RedisTemplate<String, Object>'
```

**Solution:** Add Redis configuration to your application:

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.afterPropertiesSet();
        return template;
    }
}
```

---

### ❌ Rate Limit Not Working

**Check:**
1. ✅ Is `spring.aop.proxy-target-class=true` set in `application.properties`?
2. ✅ Is the method `public`?
3. ✅ Is the method being called from outside the class (not self-invocation)?
4. ✅ Is Redis running and accessible?
5. ✅ Are the required beans (`RedisTemplate`, `RedisConnectionFactory`) configured?

---

## 📊 Performance

- **Latency:** < 1ms per check (single Redis INCR operation)
- **Throughput:** Thousands of checks per second
- **Storage:** Minimal - one key per identifier with automatic expiration
- **Scalability:** Horizontal scaling supported (distributed rate limiting)

---

## 📖 Additional Resources

- **Detailed Usage Guide:** See [USAGE.md](USAGE.md) for more examples
- **Example Controller:** See `ExampleController.java` for complete examples
- **Redis Configuration:** See `RedisConfig.java` for setup reference

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

## 📄 License

See [LICENSE](LICENSE) file for details.

---

## 🎯 Summary

This library provides a **simple, powerful, and production-ready** rate limiting solution for Spring Boot applications using Redis. Just add the annotation, and you're protected! 🛡️

```java
@RateLimit(limit = 3, duration = 60, key = "#phone")
```

That's it! 🚀
