package com.example.urlshortener.repository;

import com.example.urlshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository for URL entity operations.
 */
@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    /**
     * Finds a URL by its short code.
     *
     * @param shortCode the short code to search for
     * @return Optional containing the URL if found
     */
    Optional<Url> findByShortCode(String shortCode);

    /**
     * Checks if a URL exists by short code.
     *
     * @param shortCode the short code to check
     * @return true if exists, false otherwise
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Finds an active URL by short code.
     *
     * @param shortCode the short code to search for
     * @return Optional containing the active URL if found
     */
    @Query("SELECT u FROM Url u WHERE u.shortCode = :shortCode AND u.isActive = true")
    Optional<Url> findActiveByShortCode(@Param("shortCode") String shortCode);
}