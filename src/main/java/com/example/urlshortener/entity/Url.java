package com.example.urlshortener.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a URL mapping - H2 Compatible.
 */
@Entity
@Table(name = "urls", indexes = {
        @Index(name = "idx_urls_short_code", columnList = "shortCode", unique = true),
        @Index(name = "idx_urls_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    /**
     * Increments the click count for this URL mapping.
     */
    public void incrementClickCount() {
        this.clickCount = this.clickCount + 1;
    }

    /**
     * Checks if this URL mapping is expired.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if this URL mapping is accessible (active and not expired).
     *
     * @return true if accessible, false otherwise
     */
    public boolean isAccessible() {
        return isActive && !isExpired();
    }
}