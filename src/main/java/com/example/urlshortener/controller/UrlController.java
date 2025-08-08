package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.dto.UrlResponse;
import com.example.urlshortener.service.UrlService;
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
 * REST Controller for URL shortening operations.
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "URL Shortener", description = "API for shortening URLs and redirecting to original URLs")
public class UrlController {

    private static final Logger log = LoggerFactory.getLogger(UrlController.class);
    private final UrlService urlService;

    /**
     * Shorten a long URL.
     */
    @Operation(
            summary = "Shorten a URL",
            description = "Creates a shortened version of the provided URL and returns a short code"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "URL successfully shortened",
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
            com.example.urlshortener.dto.UrlRequest urlRequest =
                    new com.example.urlshortener.dto.UrlRequest(request.getUrl(), null);

            UrlResponse response = urlService.shortenUrl(urlRequest);

            // Convert to API response format
            ShortenResponse shortenResponse = ShortenResponse.builder()
                    .code(response.getShortCode())
                    .originalUrl(response.getLongUrl())
                    .shortUrl(response.getShortUrl())
                    .createdAt(response.getCreatedAt())
                    .expiresAt(response.getExpiresAt())
                    .build();

            log.info("Successfully created short URL: {}", response.getShortCode());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(shortenResponse);

        } catch (Exception e) {
            log.error("Error creating short URL for: {}", request.getUrl(), e);
            throw e;
        }
    }

    /**
     * Redirect to original URL using short code.
     */
    @Operation(
            summary = "Redirect to original URL",
            description = "Redirects to the original URL associated with the provided short code"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "302",
                    description = "Successfully redirected to original URL",
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
        log.info("Redirecting short code: {}", code);

        try {
            UrlResponse response = urlService.getOriginalUrl(code);
            log.info("Redirecting to: {}", response.getLongUrl());

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create(response.getLongUrl()))
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
            summary = "Health check",
            description = "Returns the health status of the URL shortener service"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(type = "string", example = "URL Shortener Service is running!")
            )
    )
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("URL Shortener Service is running!");
    }

    /**
     * Get URL details by short code (for debugging/analytics).
     */
    @Operation(
            summary = "Get URL details",
            description = "Retrieves detailed information about a shortened URL without redirecting"
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
        log.info("Retrieving details for short code: {}", code);

        try {
            UrlResponse response = urlService.getOriginalUrl(code);

            ShortenResponse details = ShortenResponse.builder()
                    .code(response.getShortCode())
                    .originalUrl(response.getLongUrl())
                    .shortUrl(response.getShortUrl())
                    .createdAt(response.getCreatedAt())
                    .expiresAt(response.getExpiresAt())
                    .build();

            log.info("Successfully retrieved details for: {}", code);
            return ResponseEntity.ok(details);

        } catch (Exception e) {
            log.error("Error retrieving details for short code: {}", code, e);
            throw e;
        }
    }
}