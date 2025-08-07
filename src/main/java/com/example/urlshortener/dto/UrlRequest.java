package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for URL shortening operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlRequest {

    @NotBlank(message = "URL cannot be blank")
    @Size(max = 2048, message = "URL cannot exceed 2048 characters")
    private String longUrl;

    private String customAlias; // Optional custom short code
}