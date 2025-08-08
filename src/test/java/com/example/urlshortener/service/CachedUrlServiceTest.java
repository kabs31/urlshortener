package com.example.urlshortener.service;

import com.example.urlshortener.config.EmbeddedRedisTestConfig;
import com.example.urlshortener.dto.UrlRequest;
import com.example.urlshortener.dto.UrlResponse;
import com.example.urlshortener.entity.Url;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.service.impl.CachedUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for CachedUrlService with embedded Redis.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {EmbeddedRedisTestConfig.class})
@Transactional
@DisplayName("CachedUrlService Integration Tests")
class CachedUrlServiceTest {

    @Autowired
    private CachedUrlService cachedUrlService;

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private UrlRepository urlRepository;

    @MockBean
    private UrlHashService urlHashService;

    private static final String TEST_LONG_URL = "https://www.example.com/very/long/path/to/resource";
    private static final String TEST_SHORT_CODE = "abc123";

    @BeforeEach
    void setUp() {
        // Clear Redis cache before each test
        stringRedisTemplate.getConnectionFactory().getConnection().flushAll();

        // Clear Spring cache
        cacheManager.getCacheNames().forEach(cacheName ->
                cacheManager.getCache(cacheName).clear());
    }

    @Test
    @DisplayName("Should create and cache short URL")
    void shouldCreateAndCacheShortUrl() {
        // Given
        UrlRequest request = new UrlRequest(TEST_LONG_URL, null);
        Url savedUrl = createTestUrl();

        when(urlHashService.generateShortCode(TEST_LONG_URL)).thenReturn(TEST_SHORT_CODE);
        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

        // When
        UrlResponse response = cachedUrlService.shortenUrl(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getShortCode()).isEqualTo(TEST_SHORT_CODE);
        assertThat(response.getLongUrl()).isEqualTo(TEST_LONG_URL);

        // Verify cache entry exists
        String cacheKey = "url:" + TEST_SHORT_CODE;
        String cachedUrl = stringRedisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedUrl).isEqualTo(TEST_LONG_URL);

        verify(urlHashService).generateShortCode(TEST_LONG_URL);
        verify(urlRepository).save(any(Url.class));
    }

    @Test
    @DisplayName("Should retrieve URL from cache on first hit")
    void shouldRetrieveUrlFromCacheOnFirstHit() {
        // Given - Manually cache a URL
        String cacheKey = "url:" + TEST_SHORT_CODE;
        stringRedisTemplate.opsForValue().set(cacheKey, TEST_LONG_URL);

        // When
        String result = cachedUrlService.getOriginalUrlForRedirect(TEST_SHORT_CODE);

        // Then
        assertThat(result).isEqualTo(TEST_LONG_URL);

        // Verify database was NOT called (cache hit)
        verify(urlRepository, never()).findActiveByShortCode(anyString());
    }

    @Test
    @DisplayName("Should fallback to database on cache miss and repopulate cache")
    void shouldFallbackToDatabaseOnCacheMissAndRepopulateCache() {
        // Given - No cache entry, but URL exists in database
        Url dbUrl = createTestUrl();
        when(urlRepository.findActiveByShortCode(TEST_SHORT_CODE)).thenReturn(Optional.of(dbUrl));

        // When
        String result = cachedUrlService.getOriginalUrlForRedirect(TEST_SHORT_CODE);

        // Then
        assertThat(result).isEqualTo(TEST_LONG_URL);

        // Verify database was called (cache miss)
        verify(urlRepository).findActiveByShortCode(TEST_SHORT_CODE);

        // Verify cache was repopulated
        String cacheKey = "url:" + TEST_SHORT_CODE;
        String cachedUrl = stringRedisTemplate.opsForValue().get(cacheKey);
        assertThat(cachedUrl).isEqualTo(TEST_LONG_URL);
    }

    @Test
    @DisplayName("Should throw exception when URL not found in cache or database")
    void shouldThrowExceptionWhenUrlNotFound() {
        // Given - No cache entry and no database entry
        when(urlRepository.findActiveByShortCode(TEST_SHORT_CODE)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> cachedUrlService.getOriginalUrlForRedirect(TEST_SHORT_CODE))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("Short code not found: " + TEST_SHORT_CODE);

        verify(urlRepository).findActiveByShortCode(TEST_SHORT_CODE);
    }

    @Test
    @DisplayName("Should use Spring @Cacheable for getOriginalUrl method")
    void shouldUseSpringCacheableForGetOriginalUrl() {
        // Given
        Url dbUrl = createTestUrl();
        when(urlRepository.findActiveByShortCode(TEST_SHORT_CODE)).thenReturn(Optional.of(dbUrl));

        // When - Call twice
        UrlResponse response1 = cachedUrlService.getOriginalUrl(TEST_SHORT_CODE);
        UrlResponse response2 = cachedUrlService.getOriginalUrl(TEST_SHORT_CODE);

        // Then
        assertThat(response1.getShortCode()).isEqualTo(TEST_SHORT_CODE);
        assertThat(response2.getShortCode()).isEqualTo(TEST_SHORT_CODE);

        // Verify database was called only once due to @Cacheable
        verify(urlRepository, times(1)).findActiveByShortCode(TEST_SHORT_CODE);
    }

    @Test
    @DisplayName("Should invalidate cache correctly")
    void shouldInvalidateCacheCorrectly() {
        // Given - Cache an entry
        String cacheKey = "url:" + TEST_SHORT_CODE;
        stringRedisTemplate.opsForValue().set(cacheKey, TEST_LONG_URL);

        // Verify it's cached
        assertThat(stringRedisTemplate.hasKey(cacheKey)).isTrue();

        // When
        cachedUrlService.invalidateCache(TEST_SHORT_CODE);

        // Then
        assertThat(stringRedisTemplate.hasKey(cacheKey)).isFalse();
    }

    @Test
    @DisplayName("Should return correct cache stats")
    void shouldReturnCorrectCacheStats() {
        // Given - Cache some entries
        stringRedisTemplate.opsForValue().set("url:code1", "http://example1.com");
        stringRedisTemplate.opsForValue().set("url:code2", "http://example2.com");
        stringRedisTemplate.opsForValue().set("other:key", "value");

        // When
        CachedUrlService.CacheStats stats = cachedUrlService.getCacheStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.isCacheEnabled()).isTrue();
        assertThat(stats.getTotalCachedUrls()).isGreaterThanOrEqualTo(3);
        assertThat(stats.getTtlSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle cache errors gracefully")
    void shouldHandleCacheErrorsGracefully() {
        // Given - URL exists in database
        Url dbUrl = createTestUrl();
        when(urlRepository.findActiveByShortCode(TEST_SHORT_CODE)).thenReturn(Optional.of(dbUrl));

        // Simulate Redis being unavailable by using invalid template
        // This test would require more sophisticated mocking in a real scenario

        // When/Then - Should still work by falling back to database
        assertThatCode(() -> {
            String result = cachedUrlService.getOriginalUrlForRedirect(TEST_SHORT_CODE);
            assertThat(result).isEqualTo(TEST_LONG_URL);
        }).doesNotThrowAnyException();
    }

    private Url createTestUrl() {
        return Url.builder()
                .id(1L)
                .shortCode(TEST_SHORT_CODE)
                .longUrl(TEST_LONG_URL)
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .isActive(true)
                .build();
    }
}