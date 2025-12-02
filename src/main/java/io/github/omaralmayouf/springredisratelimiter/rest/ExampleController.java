package io.github.omaralmayouf.springredisratelimiter.rest;

import io.github.omaralmayouf.springredisratelimiter.annotation.RateLimit;
import org.springframework.web.bind.annotation.*;

/**
 * Example controller demonstrating rate limiting usage.
 */
@RestController
@RequestMapping("/api")
public class ExampleController {

    /**
     * Example 1: OTP endpoint - limit by phone number
     * Allows 3 requests per 60 seconds per phone number
     */
    @PostMapping("/send-otp")
    @RateLimit(limit = 3, duration = 60, key = "#phone")
    public String sendOtp(@RequestParam String phone) {
        // Your OTP sending logic here
        return "OTP sent to " + phone;
    }

    /**
     * Example 2: Login endpoint - limit by email
     * Allows 5 login attempts per 300 seconds (5 minutes) per email
     */
    @PostMapping("/login")
    @RateLimit(limit = 5, duration = 300, key = "#email")
    public String login(@RequestParam String email, @RequestParam String password) {
        // Your authentication logic here
        return "Login successful for " + email;
    }

    /**
     * Example 3: Using parameter index notation
     * Allows 10 requests per 60 seconds based on first parameter
     */
    @GetMapping("/products")
    @RateLimit(limit = 10, duration = 60, key = "#p0")
    public String getProducts(String category) {
        return "Products in category: " + category;
    }

    /**
     * Example 4: Global rate limit (no dynamic key)
     * Applies same limit to all requests
     */
    @GetMapping("/public-data")
    @RateLimit(limit = 100, duration = 60, name = "publicData")
    public String getPublicData() {
        return "Public data";
    }

    /**
     * Example 5: Complex object property access
     * Using SpEL to access nested properties
     */
    @PostMapping("/register")
    @RateLimit(limit = 3, duration = 3600, key = "#user.email")
    public String register(@RequestBody User user) {
        return "User registered: " + user.getEmail();
    }

    // Simple User class for demonstration
    public static class User {
        private String email;
        private String name;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

