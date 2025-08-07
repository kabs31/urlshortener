package com.example.urlshortener.service;

import com.example.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service for generating short codes from long URLs using Base62-encoded MD5 hashing.
 */
@Service
@RequiredArgsConstructor
public class UrlHashService {

    private static final Logger log = LoggerFactory.getLogger(UrlHashService.class);
    private final UrlRepository urlRepository;

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final int MAX_COLLISION_ATTEMPTS = 100;

    /**
     * Generates a unique 6-character short code for the given long URL.
     */
    public String generateShortCode(String longUrl) {
        if (longUrl == null || longUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        String normalizedUrl = normalizeUrl(longUrl.trim());
        log.debug("Generating short code for URL: {}", normalizedUrl);

        String baseInput = normalizedUrl;
        int collisionCounter = 0;

        while (collisionCounter < MAX_COLLISION_ATTEMPTS) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] hashBytes = md5.digest(baseInput.getBytes(StandardCharsets.UTF_8));

                String shortCode = toBase62(hashBytes).substring(0, SHORT_CODE_LENGTH);

                if (!exists(shortCode)) {
                    log.info("Generated unique short code '{}' for URL: {}", shortCode, normalizedUrl);
                    return shortCode;
                }

                collisionCounter++;
                baseInput = normalizedUrl + "_" + collisionCounter;
                log.warn("Collision detected for code '{}', attempt {}/{}", shortCode, collisionCounter, MAX_COLLISION_ATTEMPTS);

            } catch (NoSuchAlgorithmException e) {
                log.error("MD5 algorithm not available", e);
                throw new RuntimeException("Hash generation failed", e);
            }
        }

        throw new RuntimeException("Unable to generate unique short code after " + MAX_COLLISION_ATTEMPTS + " attempts");
    }

    /**
     * Checks if a short code already exists in the database.
     */
    public boolean exists(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be null or empty");
        }

        boolean exists = urlRepository.existsByShortCode(code.trim());
        log.debug("Checking existence of code '{}': {}", code, exists);
        return exists;
    }

    private String toBase62(byte[] bytes) {
        StringBuilder result = new StringBuilder();

        java.math.BigInteger bigInt = new java.math.BigInteger(1, bytes);
        java.math.BigInteger base = java.math.BigInteger.valueOf(62);

        while (bigInt.compareTo(java.math.BigInteger.ZERO) > 0) {
            java.math.BigInteger remainder = bigInt.remainder(base);
            result.insert(0, BASE62_ALPHABET.charAt(remainder.intValue()));
            bigInt = bigInt.divide(base);
        }

        while (result.length() < SHORT_CODE_LENGTH) {
            result.insert(0, BASE62_ALPHABET.charAt(0));
        }

        return result.toString();
    }

    private String normalizeUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }
}