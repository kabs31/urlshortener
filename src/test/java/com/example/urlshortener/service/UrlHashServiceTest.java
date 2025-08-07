package com.example.urlshortener.service;

import com.example.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UrlHashService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UrlHashService Tests")
class UrlHashServiceTest {

    @Mock
    private UrlRepository urlRepository; // ✅ Changed from UrlService to UrlRepository

    @InjectMocks
    private UrlHashService urlHashService;

    @Nested
    @DisplayName("generateShortCode() Tests")
    class GenerateShortCodeTests {

        @Test
        @DisplayName("Should generate 6-character short code for valid URL")
        void shouldGenerateSixCharacterShortCode() {
            // Given
            String longUrl = "https://www.example.com/very/long/path/to/resource";
            when(urlRepository.existsByShortCode(anyString())).thenReturn(false);

            // When
            String shortCode = urlHashService.generateShortCode(longUrl);

            // Then
            assertThat(shortCode)
                    .isNotNull()
                    .hasSize(6)
                    .matches("[0-9A-Za-z]+");

            verify(urlRepository).existsByShortCode(shortCode);
        }

        @Test
        @DisplayName("Should generate consistent short codes for same URL")
        void shouldGenerateConsistentShortCodes() {
            // Given
            String longUrl = "https://www.example.com/test";
            when(urlRepository.existsByShortCode(anyString())).thenReturn(false);

            // When
            String shortCode1 = urlHashService.generateShortCode(longUrl);
            String shortCode2 = urlHashService.generateShortCode(longUrl);

            // Then
            assertThat(shortCode1).isEqualTo(shortCode2);
        }

        @Test
        @DisplayName("Should handle collision by generating different code")
        void shouldHandleCollisionByGeneratingDifferentCode() {
            // Given
            String longUrl = "https://www.example.com/collision-test";
            when(urlRepository.existsByShortCode(anyString()))
                    .thenReturn(true)  // First attempt - collision
                    .thenReturn(false); // Second attempt - success

            // When
            String shortCode = urlHashService.generateShortCode(longUrl);

            // Then
            assertThat(shortCode)
                    .isNotNull()
                    .hasSize(6);

            verify(urlRepository, times(2)).existsByShortCode(anyString());
        }

        @ParameterizedTest
        @DisplayName("Should throw exception for null or empty URL")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        void shouldThrowExceptionForInvalidUrl(String invalidUrl) {
            // When/Then
            assertThatThrownBy(() -> urlHashService.generateShortCode(invalidUrl))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("URL cannot be null or empty");

            verifyNoInteractions(urlRepository);
        }
    }

    @Nested
    @DisplayName("exists() Tests")
    class ExistsTests {

        @Test
        @DisplayName("Should return true when short code exists")
        void shouldReturnTrueWhenShortCodeExists() {
            // Given
            String existingCode = "abc123";
            when(urlRepository.existsByShortCode(existingCode)).thenReturn(true);

            // When
            boolean exists = urlHashService.exists(existingCode);

            // Then
            assertThat(exists).isTrue();
            verify(urlRepository).existsByShortCode(existingCode);
        }

        @Test
        @DisplayName("Should return false when short code does not exist")
        void shouldReturnFalseWhenShortCodeDoesNotExist() {
            // Given
            String nonExistingCode = "xyz789";
            when(urlRepository.existsByShortCode(nonExistingCode)).thenReturn(false);

            // When
            boolean exists = urlHashService.exists(nonExistingCode);

            // Then
            assertThat(exists).isFalse();
            verify(urlRepository).existsByShortCode(nonExistingCode);
        }

        @ParameterizedTest
        @DisplayName("Should throw exception for null or empty code")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        void shouldThrowExceptionForInvalidCode(String invalidCode) {
            // When/Then
            assertThatThrownBy(() -> urlHashService.exists(invalidCode))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Code cannot be null or empty");

            verifyNoInteractions(urlRepository);
        }
    }
}