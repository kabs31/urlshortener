package com.example.urlshortener.controller;

import com.example.urlshortener.dto.UrlRequest;
import com.example.urlshortener.dto.UrlResponse;
import com.example.urlshortener.service.UrlService;
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
@RequestMapping("/urls")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UrlController {

    private static final Logger log = LoggerFactory.getLogger(UrlController.class);
    private final UrlService urlService;

    /**
     * Create a shortened URL.
     *
     * @param request the URL shortening request
     * @return ResponseEntity with the shortened URL response
     */
    @PostMapping
    public ResponseEntity<UrlResponse> createShortUrl(@Valid @RequestBody UrlRequest request) {
        log.info("Received request to shorten URL: {}", request.getLongUrl());

        try {
            UrlResponse response = urlService.shortenUrl(request);
            log.info("Successfully created short URL: {}", response.getShortCode());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (Exception e) {
            log.error("Error creating short URL for: {}", request.getLongUrl(), e);
            throw e;
        }
    }

    /**
     * Retrieve original URL by short code.
     *
     * @param shortCode the short code to resolve
     * @return ResponseEntity with the original URL
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlResponse> getOriginalUrl(@PathVariable String shortCode) {
        log.info("Retrieving original URL for short code: {}", shortCode);

        try {
            UrlResponse response = urlService.getOriginalUrl(shortCode);
            log.info("Successfully retrieved original URL for: {}", shortCode);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving URL for short code: {}", shortCode, e);
            throw e;
        }
    }

    /**
     * Redirect to original URL using short code.
     *
     * @param shortCode the short code to redirect
     * @return ResponseEntity with redirect response
     */
    @GetMapping("/{shortCode}/redirect")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String shortCode) {
        log.info("Redirecting short code: {}", shortCode);

        try {
            UrlResponse response = urlService.getOriginalUrl(shortCode);
            log.info("Redirecting to: {}", response.getLongUrl());

            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create(response.getLongUrl()))
                    .build();

        } catch (Exception e) {
            log.error("Error redirecting short code: {}", shortCode, e);
            throw e;
        }
    }

    /**
     * Health check endpoint.
     *
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("URL Shortener Service is running!");
    }
}