package com.example.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for URL shortening.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to shorten a URL")
public class ShortenRequest {

    @NotBlank(message = "URL cannot be blank")
    @Size(max = 2048, message = "URL cannot exceed 2048 characters")
    @Pattern(
            regexp = "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/.*)?$|^(https?://)?localhost(:[0-9]+)?(/.*)?$",
            message = "Invalid URL format"
    )
    @Schema(
            description = "The long URL to be shortened",
            example = "https://www.example.com/very/long/path/to/resource",
            required = true
    )
    private String url;
}