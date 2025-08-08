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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * URL Service implementation with Redis caching support.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CachedUrlService implements UrlService {

    private static final Logger log = LoggerFactory.getLogger(CachedUrlService.class);
    private static final String URL_CACHE_PREFIX = "url:";
    private static final String URL_CACHE_NAME = "urlCache";

    private final UrlRepository urlRepository;
    private final UrlHashService urlHashService;
    private final RedisTemplate<String, String> stringRedisTemplate;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.cache.url.ttl:3600}")
    private long urlCacheTtlSeconds;

    @Override
    @CachePut(value = URL_CACHE_NAME, key = "#result.shortCode")
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
        log.info("Created short URL with code: {} and cached it", shortCode);

        // Also manually cache the simple key-value mapping for fast redirect lookup
        cacheUrlMapping(shortCode, request.getLongUrl());

        return mapToResponse(savedUrl);
    }

    @Override
    @Cacheable(value = URL_CACHE_NAME, key = "#shortCode")
    @Transactional(readOnly = true)
    public UrlResponse getOriginalUrl(String shortCode) {
        log.debug("Cache miss - retrieving original URL for code from DB: {}", shortCode);

        Url url = urlRepository.findActiveByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short code not found: " + shortCode));

        log.info("Retrieved URL from DB and caching for code: {}", shortCode);
        return mapToResponse(url);
    }

    /**
     * Fast redirect method that uses Redis for direct URL lookup.
     * This bypasses full object caching for maximum performance.
     */
    public String getOriginalUrlForRedirect(String shortCode) {
        String cacheKey = URL_CACHE_PREFIX + shortCode;

        try {
            // First, try to get from Redis cache
            String cachedUrl = stringRedisTemplate.opsForValue().get(cacheKey);

            if (cachedUrl != null) {
                log.debug("Cache HIT for redirect: {} -> {}", shortCode, cachedUrl);

                // Increment click count asynchronously (don't block redirect)
                incrementClickCountAsync(shortCode);

                return cachedUrl;
            }

            log.debug("Cache MISS for redirect: {}", shortCode);

            // Cache miss - get from database
            Url url = urlRepository.findActiveByShortCode(shortCode)
                    .orElseThrow(() -> new UrlNotFoundException("Short code not found: " + shortCode));

            // Cache the result
            cacheUrlMapping(shortCode, url.getLongUrl());

            // Increment click count
            incrementClickCountAsync(shortCode);

            log.info("Retrieved URL from DB and cached for redirect: {} -> {}", shortCode, url.getLongUrl());
            return url.getLongUrl();

        } catch (Exception e) {
            log.error("Error during cached redirect lookup for {}: {}", shortCode, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByShortCode(String shortCode) {
        // Check cache first, then database
        String cacheKey = URL_CACHE_PREFIX + shortCode;

        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cacheKey))) {
            return true;
        }

        return urlRepository.existsByShortCode(shortCode);
    }

    /**
     * Invalidate cache when URL is deleted or deactivated.
     */
    @CacheEvict(value = URL_CACHE_NAME, key = "#shortCode")
    public void invalidateCache(String shortCode) {
        String cacheKey = URL_CACHE_PREFIX + shortCode;
        stringRedisTemplate.delete(cacheKey);
        log.info("Invalidated cache for short code: {}", shortCode);
    }

    /**
     * Manually cache URL mapping for fast redirect lookups.
     */
    private void cacheUrlMapping(String shortCode, String longUrl) {
        try {
            String cacheKey = URL_CACHE_PREFIX + shortCode;
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    longUrl,
                    urlCacheTtlSeconds,
                    TimeUnit.SECONDS
            );
            log.debug("Cached URL mapping: {} -> {}", shortCode, longUrl);
        } catch (Exception e) {
            log.warn("Failed to cache URL mapping for {}: {}", shortCode, e.getMessage());
            // Don't fail the operation if caching fails
        }
    }

    /**
     * Asynchronously increment click count to avoid blocking redirects.
     */
    private void incrementClickCountAsync(String shortCode) {
        // In a real application, you might use @Async or a message queue
        // For now, we'll do a simple async operation
        try {
            // Update database asynchronously
            new Thread(() -> {
                try {
                    urlRepository.findByShortCode(shortCode).ifPresent(url -> {
                        url.setClickCount(url.getClickCount() + 1);
                        urlRepository.save(url);
                    });
                } catch (Exception e) {
                    log.error("Failed to increment click count for {}: {}", shortCode, e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            log.warn("Failed to start async click count increment for {}: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Get cache statistics for monitoring.
     */
    public CacheStats getCacheStats() {
        try {
            // Fixed: Use explicit RedisCallback to avoid ambiguous method reference
            Long totalKeys = stringRedisTemplate.execute((RedisCallback<Long>) connection -> {
                try {
                    // Use keyspace info instead of deprecated dbSize()
                    return connection.commands().dbSize();
                } catch (Exception e) {
                    log.warn("Could not get database size, returning 0: {}", e.getMessage());
                    return 0L;
                }
            });

            return CacheStats.builder()
                    .totalCachedUrls(totalKeys != null ? totalKeys : 0L)
                    .cacheEnabled(true)
                    .ttlSeconds(urlCacheTtlSeconds)
                    .build();
        } catch (Exception e) {
            log.error("Failed to get cache stats: {}", e.getMessage());
            return CacheStats.builder()
                    .totalCachedUrls(0L)
                    .cacheEnabled(false)
                    .ttlSeconds(urlCacheTtlSeconds)
                    .build();
        }
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

    /**
     * Cache statistics DTO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CacheStats {
        private Long totalCachedUrls;
        private boolean cacheEnabled;
        private long ttlSeconds;
    }
}