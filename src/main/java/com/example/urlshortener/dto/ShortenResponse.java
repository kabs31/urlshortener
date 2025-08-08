package com.example.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for URL shortening operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing the shortened URL details")
public class ShortenResponse {

    @Schema(
            description = "The short code generated for the URL",
            example = "abc123",
            required = true
    )
    private String code;

    @Schema(
            description = "The original long URL",
            example = "https://www.example.com/very/long/path/to/resource"
    )
    private String originalUrl;

    @Schema(
            description = "The complete shortened URL",
            example = "http://localhost:8080/api/v1/abc123"
    )
    private String shortUrl;

    @Schema(
            description = "Timestamp when the URL was shortened",
            example = "2025-08-08T14:30:00"
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Expiration timestamp (if applicable)",
            example = "2025-08-08T14:30:00"
    )
    private LocalDateTime expiresAt;
}