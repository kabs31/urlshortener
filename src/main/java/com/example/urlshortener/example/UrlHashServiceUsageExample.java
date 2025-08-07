package com.example.urlshortener.example;

import com.example.urlshortener.service.UrlHashService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Demonstrates practical usage of UrlHashService.
 *
 * This example shows how to integrate the UrlHashService into your application
 * for generating short codes with proper error handling and validation.
 */
@Component
@RequiredArgsConstructor
public class UrlHashServiceUsageExample {

    private static final Logger log = LoggerFactory.getLogger(UrlHashServiceUsageExample.class);
    private final UrlHashService urlHashService;

    /**
     * Example 1: Basic URL shortening
     */
    public void basicUsageExample() {
        try {
            String longUrl = "https://www.example.com/very/long/path/to/resource";
            String shortCode = urlHashService.generateShortCode(longUrl);

            log.info("Generated short code '{}' for URL: {}", shortCode, longUrl);
            // Output: Generated short code 'a1B2c3' for URL: https://www.example.com/very/long/path/to/resource

        } catch (IllegalArgumentException e) {
            log.error("Invalid URL provided: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to generate short code: {}", e.getMessage());
        }
    }

    /**
     * Example 2: Checking for existing codes before generation
     */
    public void checkExistingCodeExample() {
        String potentialCode = "abc123";

        if (urlHashService.exists(potentialCode)) {
            log.warn("Code '{}' already exists, cannot use as custom alias", potentialCode);
        } else {
            log.info("Code '{}' is available for use", potentialCode);
        }
    }

    /**
     * Example 3: Batch processing with error handling
     */
    public void batchProcessingExample() {
        String[] urls = {
                "https://www.google.com",
                "https://www.github.com/user/repo",
                "", // Invalid URL - will cause exception
                "stackoverflow.com/questions/123", // Will be normalized
                "https://www.example.com/very/long/path/with/many/segments"
        };

        for (String url : urls) {
            try {
                String shortCode = urlHashService.generateShortCode(url);
                log.info("✓ Successfully generated '{}' for: {}", shortCode, url);

            } catch (IllegalArgumentException e) {
                log.error("✗ Invalid URL '{}': {}", url, e.getMessage());
            } catch (RuntimeException e) {
                log.error("✗ Failed to process '{}': {}", url, e.getMessage());
            }
        }
    }

    /**
     * Example 4: Custom validation and business logic
     */
    public void customValidationExample() {
        String longUrl = "https://malicious-site.com/phishing";

        // Custom validation before generating short code
        if (isUrlAllowed(longUrl)) {
            try {
                String shortCode = urlHashService.generateShortCode(longUrl);
                log.info("Generated short code '{}' for validated URL", shortCode);

                // Double-check the code doesn't exist (redundant but safe)
                if (!urlHashService.exists(shortCode)) {
                    log.info("Confirmed: Short code '{}' is unique", shortCode);
                }

            } catch (Exception e) {
                log.error("Failed to generate short code: {}", e.getMessage());
            }
        } else {
            log.warn("URL rejected by custom validation: {}", longUrl);
        }
    }

    /**
     * Example 5: Performance testing and collision simulation
     */
    public void performanceTestExample() {
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int errorCount = 0;

        // Generate codes for similar URLs (likely to cause collisions)
        String baseUrl = "https://www.example.com/page";

        for (int i = 0; i < 100; i++) {
            try {
                String url = baseUrl + "?id=" + i;
                String shortCode = urlHashService.generateShortCode(url);
                successCount++;

                if (i % 10 == 0) {
                    log.info("Generated {} codes so far, latest: '{}'", successCount, shortCode);
                }

            } catch (Exception e) {
                errorCount++;
                log.error("Error generating code #{}: {}", i, e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Performance test completed in {}ms - Success: {}, Errors: {}",
                duration, successCount, errorCount);
    }

    /**
     * Example 6: Integration with service layer
     */
    public String integrateWithServiceLayer(String longUrl) {
        // Validate input
        if (longUrl == null || longUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        // Normalize URL
        String normalizedUrl = longUrl.trim();

        // Generate short code with retry logic
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String shortCode = urlHashService.generateShortCode(normalizedUrl);
                log.info("Successfully generated short code on attempt {}", attempt);
                return shortCode;

            } catch (RuntimeException e) {
                log.warn("Attempt {} failed: {}", attempt, e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("Failed to generate short code after " + maxRetries + " attempts", e);
                }

                // Brief pause before retry
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }

        return null; // Should never reach here
    }

    /**
     * Helper method for custom URL validation
     */
    private boolean isUrlAllowed(String url) {
        // Example business logic for URL validation
        String lowerUrl = url.toLowerCase();

        // Block known malicious domains
        String[] blockedDomains = {"malicious-site.com", "spam.com", "phishing.net"};
        for (String domain : blockedDomains) {
            if (lowerUrl.contains(domain)) {
                return false;
            }
        }

        // Block certain file extensions
        String[] blockedExtensions = {".exe", ".bat", ".scr"};
        for (String ext : blockedExtensions) {
            if (lowerUrl.endsWith(ext)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Example 7: Demonstrating collision resolution
     */
    public void demonstrateCollisionResolution() {
        log.info("=== Collision Resolution Demo ===");

        // These URLs are designed to potentially create similar hashes
        String[] similarUrls = {
                "https://example.com/test",
                "https://example.com/test1",
                "https://example.com/test2"
        };

        for (String url : similarUrls) {
            try {
                String shortCode = urlHashService.generateShortCode(url);
                boolean exists = urlHashService.exists(shortCode);

                log.info("URL: {} → Code: {} (Exists check: {})", url, shortCode, exists);

            } catch (Exception e) {
                log.error("Failed to process {}: {}", url, e.getMessage());
            }
        }
    }
}