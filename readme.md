# 📦 spring-redis-ratelimiter

A **lightweight, annotation-based rate limiting library** for Spring Boot using Redis to protect endpoints from abuse (OTP spam, login throttling, IP limiting, etc.).

---

## 🚀 Features

* ✅ **Annotation-based**: Add `@RateLimit` to any method
* ✅ **SpEL support**: Dynamic keys using `#phone`, `#email`, `#user.id`, `#p0`, etc.
* ✅ **Fixed-window algorithm**: Uses Redis atomic `INCR` + `EXPIRE`
* ✅ **Distributed-ready**: Works across multiple instances
* ✅ **Thread-safe**: High concurrency safe
* ✅ **Production-ready**: Exception handling + HTTP 429 responses

---

## ⚠️ Prerequisites

Before using:

1. Redis server running (local or remote)
2. Spring Boot application with `spring-boot-starter-data-redis`
3. Redis configuration in your application (library **does not** provide Redis config)

---

## 📦 Installation (v1.0.2)

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

## ⚙️ Redis Configuration

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
```

**Option B: Custom RedisConfig bean**

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

## 🔍 Component Scanning

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

## 🎯 Applying Rate Limiting

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

---

## 🚨 Optional: Global Exception Handler

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
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                             .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                             .body(response);
    }
}
```

---

## ✅ Checklist

* Redis server running (`docker run -d -p 6379:6379 redis:latest`)
* `spring-boot-starter-data-redis` added
* Redis configured in `application.yml` or via `RedisConfig`
* `spring.aop.proxy-target-class=true` set
* Component scanning includes `io.github.omaralmayouf.springredisratelimiter`
* `@RateLimit` annotation added to desired methods

---

## 🎯 Quick Examples

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
```

---

## 🐛 Troubleshooting

| Issue                         | Solution                                                            |
| ----------------------------- | ------------------------------------------------------------------- |
| Redis connection failed       | Ensure Redis is running (`docker run -d -p 6379:6379 redis:latest`) |
| `@RateLimit` not working      | Set `spring.aop.proxy-target-class=true`                            |
| Bean not found: RedisTemplate | Add Spring Data Redis and configure RedisTemplate                   |
| Rate limit not applied        | Include library package in component scanning                       |

---
