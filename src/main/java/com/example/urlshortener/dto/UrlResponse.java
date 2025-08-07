package com.example.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for URL operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlResponse {

    private String shortCode;
    private String longUrl;
    private String shortUrl; // Full shortened URL
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long clickCount;
}