package com.example.urlshortener.service.impl;

import com.example.urlshortener.dto.UrlRequest;
import com.example.urlshortener.dto.UrlResponse;
import com.example.urlshortener.entity.Url;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.service.UrlHashService;
import com.example.urlshortener.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of UrlService providing URL shortening functionality.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UrlServiceImpl implements UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlServiceImpl.class);
    private final UrlRepository urlRepository;
    private final UrlHashService urlHashService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public UrlResponse shortenUrl(UrlRequest request) {
        log.info("Creating short URL for: {}", request.getLongUrl());

        // Generate short code using the hash service
        String shortCode = urlHashService.generateShortCode(request.getLongUrl());

        // Create and save URL entity
        Url url = Url.builder()
                .shortCode(shortCode)
                .longUrl(request.getLongUrl())
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .isActive(true)
                .build();

        Url savedUrl = urlRepository.save(url);
        log.info("Created short URL with code: {}", shortCode);

        return mapToResponse(savedUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public UrlResponse getOriginalUrl(String shortCode) {
        log.debug("Retrieving original URL for code: {}", shortCode);

        Url url = urlRepository.findActiveByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short code not found: " + shortCode));

        return mapToResponse(url);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByShortCode(String shortCode) {
        return urlRepository.existsByShortCode(shortCode);
    }

    private UrlResponse mapToResponse(Url url) {
        return UrlResponse.builder()
                .shortCode(url.getShortCode())
                .longUrl(url.getLongUrl())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .build();
    }
}