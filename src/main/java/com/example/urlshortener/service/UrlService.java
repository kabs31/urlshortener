package com.example.urlshortener.service;

import com.example.urlshortener.dto.UrlRequest;
import com.example.urlshortener.dto.UrlResponse;

/**
 * Service interface for URL shortening operations.
 */
public interface UrlService {

    /**
     * Creates a shortened URL from the given long URL.
     *
     * @param request the URL request containing the long URL
     * @return UrlResponse containing the short code and metadata
     */
    UrlResponse shortenUrl(UrlRequest request);

    /**
     * Retrieves the original long URL for a given short code.
     *
     * @param shortCode the short code to resolve
     * @return UrlResponse containing the original URL and metadata
     */
    UrlResponse getOriginalUrl(String shortCode);

    /**
     * Checks if a short code already exists in the database.
     *
     * @param shortCode the short code to check
     * @return true if the short code exists, false otherwise
     */
    boolean existsByShortCode(String shortCode);
}