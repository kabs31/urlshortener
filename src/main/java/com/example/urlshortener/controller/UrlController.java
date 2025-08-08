package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.dto.UrlRequest;
import com.example.urlshortener.dto.UrlResponse;
import com.example.urlshortener.service.UrlService;
import com.example.urlshortener.service.impl.CachedUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST Controller for URL shortening operations with Redis caching.
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "URL Shortener", description = "API for shortening URLs and redirecting to original URLs with Redis caching")
public class UrlController {

    private static final Logger log = LoggerFactory.getLogger(UrlController.class);
    private final UrlService urlService;
    private final CachedUrlService cachedUrlService;

    /**
     * Shorten a long URL.
     */
    @Operation(
            summary = "Shorten a URL",
            description = "Creates a shortened version of the provided URL and caches it in Redis"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "URL successfully shortened and cached",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShortenResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid URL format or validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
            @Valid @RequestBody
            @Parameter(description = "URL shortening request", required = true)
            ShortenRequest request
    ) {
        log.info("Received request to shorten URL: {}", request.getUrl());

        try {
            // Convert to internal DTO format
            UrlRequest urlRequest = new UrlRequest(request.getUrl(), null);

            // Use cached service for shortening (will automatically cache result)
            UrlResponse response = cachedUrlService.shortenUrl(urlRequest);

            // Convert to API response format
            ShortenResponse shortenResponse = ShortenResponse.builder()
                    .code(response.getShortCode())
                    .originalUrl(response.getLongUrl())
                    .shortUrl(response.getShortUrl())
                    .createdAt(response.getCreatedAt())
                    .expiresAt(response.getExpiresAt())
                    .build();

            log.info("Successfully created and cached short URL: {}", response.getShortCode());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(shortenResponse);

        } catch (Exception e) {
            log.error("Error creating short URL for: {}", request.getUrl(), e);
            throw e;
        }
    }

    /**
     * Redirect to original URL using short code with Redis caching.
     * This is the main redirect endpoint optimized for performance.
     */
    @Operation(
            summary = "Redirect to original URL (Cached)",
            description = "Redirects to the original URL with Redis cache lookup first, then DB fallback"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "302",
                    description = "Successfully redirected to original URL (cache hit or miss)",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Short code not found",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Short code has expired or is inactive",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirectToOriginalUrl(
            @PathVariable
            @Parameter(
                    description = "The short code to redirect",
                    example = "abc123",
                    required = true
            )
            String code
    ) {
        log.info("Redirecting short code with cache lookup: {}", code);

        try {
            // Use optimized cached redirect method
            // This will:
            // 1. Check Redis cache first (key: url:{code})
            // 2. If cache miss, query DB and cache result
            // 3. Increment click count asynchronously
            String longUrl = cachedUrlService.getOriginalUrlForRedirect(code);

            log.info("Redirecting {} to: {} (cached lookup)", code, longUrl);

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create(longUrl))
                    .build();

        } catch (Exception e) {
            log.error("Error redirecting short code: {}", code, e);
            throw e;
        }
    }

    /**
     * Health check endpoint.
     */
    @Operation(
            summary = "Health check with cache status",
            description = "Returns the health status of the URL shortener service and Redis cache"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(
                    mediaType = "application/json"
            )
    )
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        try {
            CachedUrlService.CacheStats cacheStats = cachedUrlService.getCacheStats();

            HealthResponse health = HealthResponse.builder()
                    .status("UP")
                    .message("URL Shortener Service is running!")
                    .cacheEnabled(cacheStats.isCacheEnabled())
                    .totalCachedUrls(cacheStats.getTotalCachedUrls())
                    .cacheTtlSeconds(cacheStats.getTtlSeconds())
                    .build();

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());

            HealthResponse health = HealthResponse.builder()
                    .status("DOWN")
                    .message("Service error: " + e.getMessage())
                    .cacheEnabled(false)
                    .totalCachedUrls(0L)
                    .cacheTtlSeconds(0L)
                    .build();

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    /**
     * Get URL details by short code with caching.
     */
    @Operation(
            summary = "Get URL details (Cached)",
            description = "Retrieves detailed information about a shortened URL using cache when possible"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "URL details retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShortenResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Short code not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/api/details/{code}")
    public ResponseEntity<ShortenResponse> getUrlDetails(
            @PathVariable
            @Parameter(
                    description = "The short code to get details for",
                    example = "abc123",
                    required = true
            )
            String code
    ) {
        log.info("Retrieving cached details for short code: {}", code);

        try {
            // Use the cached service method (will hit cache or DB)
            UrlResponse response = cachedUrlService.getOriginalUrl(code);

            ShortenResponse details = ShortenResponse.builder()
                    .code(response.getShortCode())
                    .originalUrl(response.getLongUrl())
                    .shortUrl(response.getShortUrl())
                    .createdAt(response.getCreatedAt())
                    .expiresAt(response.getExpiresAt())
                    .build();

            log.info("Successfully retrieved cached details for: {}", code);
            return ResponseEntity.ok(details);

        } catch (Exception e) {
            log.error("Error retrieving details for short code: {}", code, e);
            throw e;
        }
    }

    /**
     * Cache management endpoint - invalidate specific short code.
     */
    @Operation(
            summary = "Invalidate cache for short code",
            description = "Removes a specific short code from Redis cache"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cache invalidated successfully",
            content = @Content(mediaType = "text/plain")
    )
    @DeleteMapping("/api/cache/{code}")
    public ResponseEntity<String> invalidateCache(
            @PathVariable
            @Parameter(
                    description = "The short code to remove from cache",
                    example = "abc123",
                    required = true
            )
            String code
    ) {
        log.info("Invalidating cache for short code: {}", code);

        try {
            cachedUrlService.invalidateCache(code);
            return ResponseEntity.ok("Cache invalidated for: " + code);
        } catch (Exception e) {
            log.error("Error invalidating cache for: {}", code, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to invalidate cache: " + e.getMessage());
        }
    }

    /**
     * Health response DTO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @Schema(description = "Health check response with cache information")
    public static class HealthResponse {
        @Schema(description = "Service status", example = "UP")
        private String status;

        @Schema(description = "Status message", example = "URL Shortener Service is running!")
        private String message;

        @Schema(description = "Whether Redis cache is enabled", example = "true")
        private boolean cacheEnabled;

        @Schema(description = "Total number of cached URLs", example = "1250")
        private Long totalCachedUrls;

        @Schema(description = "Cache TTL in seconds", example = "3600")
        private Long cacheTtlSeconds;
    }
}