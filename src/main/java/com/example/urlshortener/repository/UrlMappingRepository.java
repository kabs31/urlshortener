package com.example.urlshortener.repository;

import com.example.urlshortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for UrlMapping entity operations.
 */
@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, UUID> {

    /**
     * Finds a URL mapping by its short code.
     *
     * @param shortCode the short code to search for
     * @return Optional containing the URL mapping if found
     */
    Optional<UrlMapping> findByShortCode(String shortCode);

    /**
     * Checks if a URL mapping exists by short code.
     *
     * @param shortCode the short code to check
     * @return true if exists, false otherwise
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Finds an active URL mapping by short code.
     *
     * @param shortCode the short code to search for
     * @return Optional containing the active URL mapping if found
     */
    @Query("SELECT u FROM UrlMapping u WHERE u.shortCode = :shortCode AND u.isActive = true")
    Optional<UrlMapping> findActiveByShortCode(@Param("shortCode") String shortCode);

    /**
     * Finds an accessible URL mapping by short code (active and not expired).
     *
     * @param shortCode the short code to search for
     * @param now current timestamp for expiration check
     * @return Optional containing the accessible URL mapping if found
     */
    @Query("SELECT u FROM UrlMapping u WHERE u.shortCode = :shortCode AND u.isActive = true AND (u.expiresAt IS NULL OR u.expiresAt > :now)")
    Optional<UrlMapping> findAccessibleByShortCode(@Param("shortCode") String shortCode, @Param("now") LocalDateTime now);

    /**
     * Finds all URL mappings created by a specific user.
     *
     * @param createdBy the user who created the mappings
     * @return List of URL mappings
     */
    List<UrlMapping> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Finds all expired URL mappings.
     *
     * @param now current timestamp
     * @return List of expired URL mappings
     */
    @Query("SELECT u FROM UrlMapping u WHERE u.expiresAt IS NOT NULL AND u.expiresAt <= :now")
    List<UrlMapping> findExpiredMappings(@Param("now") LocalDateTime now);

    /**
     * Finds top N most clicked URL mappings.
     *
     * @param limit maximum number of results
     * @return List of top clicked URL mappings
     */
    @Query("SELECT u FROM UrlMapping u WHERE u.isActive = true ORDER BY u.clickCount DESC LIMIT :limit")
    List<UrlMapping> findTopClickedMappings(@Param("limit") int limit);

    /**
     * Increments the click count for a URL mapping.
     *
     * @param shortCode the short code of the URL mapping
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    int incrementClickCount(@Param("shortCode") String shortCode);

    /**
     * Deactivates expired URL mappings.
     *
     * @param now current timestamp
     * @return number of deactivated records
     */
    @Modifying
    @Query("UPDATE UrlMapping u SET u.isActive = false WHERE u.expiresAt IS NOT NULL AND u.expiresAt <= :now AND u.isActive = true")
    int deactivateExpiredMappings(@Param("now") LocalDateTime now);

    /**
     * Counts total active URL mappings.
     *
     * @return count of active mappings
     */
    @Query("SELECT COUNT(u) FROM UrlMapping u WHERE u.isActive = true")
    long countActiveMappings();

    /**
     * Counts URL mappings created by a specific user.
     *
     * @param createdBy the user who created the mappings
     * @return count of mappings created by the user
     */
    long countByCreatedBy(String createdBy);

    /**
     * Finds URL mappings created within a date range.
     *
     * @param startDate start of the date range
     * @param endDate end of the date range
     * @return List of URL mappings created within the range
     */
    @Query("SELECT u FROM UrlMapping u WHERE u.createdAt BETWEEN :startDate AND :endDate ORDER BY u.createdAt DESC")
    List<UrlMapping> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}