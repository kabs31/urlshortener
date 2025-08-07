package com.example.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UrlHashService.
 *
 * Tests cover:
 * - Short code generation with various URL formats
 * - Collision detection and resolution
 * - Input validation and edge cases
 * - Error handling scenarios
 *
 * @author Test Suite
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UrlHashService Tests")
class UrlHashServiceTest {

    @Mock
    private UrlService urlService;

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
            when(urlService.existsByShortCode(anyString())).thenReturn(false);

            // When
            String shortCode = urlHashService.generateShortCode(longUrl);

            // Then
            assertThat(shortCode)
                    .isNotNull()
                    .hasSize(6)
                    .matches("[0-9A-Za-z]+"); // Base62 pattern

            verify(urlService).existsByShortCode(shortCode);
        }

        @Test
        @DisplayName("Should generate consistent short codes for same URL")
        void shouldGenerateConsistentShortCodes() {
            // Given
            String longUrl = "https://www.example.com/test";
            when(urlService.existsByShortCode(anyString())).thenReturn(false);

            // When
            String shortCode1 = urlHashService.generateShortCode(longUrl);
            String shortCode2 = urlHashService.generateShortCode(longUrl);

            // Then
            assertThat(shortCode1).isEqualTo(shortCode2);
        }

        @Test
        @DisplayName("Should generate different short codes for different URLs")
        void shouldGenerateDifferentShortCodesForDifferentUrls() {
            // Given
            String url1 = "https://www.example.com/path1";
            String url2 = "https://www.example.com/path2";
            when(urlService.existsByShortCode(anyString())).thenReturn(false);

            // When
            String shortCode1 = urlHashService.generateShortCode(url1);
            String shortCode2 = urlHashService.generateShortCode(url2);

            // Then
            assertThat(shortCode1).isNotEqualTo(shortCode2);
        }

        @ParameterizedTest
        @DisplayName("Should normalize URLs without protocol")
        @ValueSource(strings = {
                "www.example.com",
                "example.com/path",
                "subdomain.example.com/path/to/resource"
        })
        void shouldNormalizeUrlsWithoutProtocol(String urlWithoutProtocol) {
            // Given
            String urlWithProtocol = "https://" + urlWithoutProtocol;
            when(urlService.existsByShortCode(anyString())).thenReturn(false);

            // When
            String shortCode1 = urlHashService.generateShortCode(urlWithoutProtocol);
            String shortCode2 = urlHashService.generateShortCode(urlWithProtocol);

            // Then
            assertThat(shortCode1).isEqualTo(shortCode2);
        }

        @Test
        @DisplayName("Should handle collision by generating different code")
        void shouldHandleCollisionByGeneratingDifferentCode() {
            // Given
            String longUrl = "https://www.example.com/collision-test";
            when(urlService.existsByShortCode(anyString()))
                    .thenReturn(true)  // First attempt - collision
                    .thenReturn(false); // Second attempt - success

            // When
            String shortCode = urlHashService.generateShortCode(longUrl);

            // Then
            assertThat(shortCode)
                    .isNotNull()
                    .hasSize(6);

            verify(urlService, times(2)).existsByShortCode(anyString());
        }

        @Test
        @DisplayName("Should throw exception after maximum collision attempts")
        void shouldThrowExceptionAfterMaxCollisionAttempts() {
            // Given
            String longUrl = "https://www.example.com/max-collisions";
            when(urlService.existsByShortCode(anyString())).thenReturn(true); // Always collision

            // When/Then
            assertThatThrownBy(() -> urlHashService.generateShortCode(longUrl))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unable to generate unique short code after 100 attempts");

            verify(urlService, times(100)).existsByShortCode(anyString());
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

            verifyNoInteractions(urlService);
        }

        @Test
        @DisplayName("Should handle URLs with special characters")
        void shouldHandleUrlsWithSpecialCharacters() {
            // Given
            String urlWithSpecialChars = "https://www.example.com/path?param=value&other=123#section";
            when(urlService.existsByShortCode(anyString())).thenReturn(false);

            // When
            String shortCode = urlHashService.generateShortCode(urlWithSpecialChars);

            // Then
            assertThat(shortCode)
                    .isNotNull()
                    .hasSize(6)
                    .matches("[0-9A-Za-z]+");
        }

        @Test
        @DisplayName("Should handle very long URLs")
        void shouldHandleVeryLongUrls() {
            // Given
            String longPath = "very/long/path/".repeat(100);
            String veryLongUrl = "https://www.example.com/" + longPath;
            when(urlService.existsByShortCode(anyString())).thenReturn(false);

            // When
            String shortCode = urlHashService.generateShortCode(veryLongUrl);

            // Then
            assertThat(shortCode)
                    .isNotNull()
                    .hasSize(6);
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
            when(urlService.existsByShortCode(existingCode)).thenReturn(true);

            // When
            boolean exists = urlHashService.exists(existingCode);

            // Then
            assertThat(exists).isTrue();
            verify(urlService).existsByShortCode(existingCode);
        }

        @Test
        @DisplayName("Should return false when short code does not exist")
        void shouldReturnFalseWhenShortCodeDoesNotExist() {
            // Given
            String nonExistingCode = "xyz789";
            when(urlService.existsByShortCode(nonExistingCode)).thenReturn(false);

            // When
            boolean exists = urlHashService.exists(nonExistingCode);

            // Then
            assertThat(exists).isFalse();
            verify(urlService).existsByShortCode(nonExistingCode);
        }

        @Test
        @DisplayName("Should trim whitespace from code before checking")
        void shouldTrimWhitespaceFromCode() {
            // Given
            String codeWithWhitespace = "  abc123  ";
            String trimmedCode = "abc123";
            when(urlService.existsByShortCode(trimmedCode)).thenReturn(true);

            // When
            boolean exists = urlHashService.exists(codeWithWhitespace);

            // Then
            assertThat(exists).isTrue();
            verify(urlService).existsByShortCode(trimmedCode);
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

            verifyNoInteractions(urlService);
        }

        @Test
        @DisplayName("Should handle various code formats")
        void shouldHandleVariousCodeFormats() {
            // Given
            String[] validCodes = {"123456", "AbCdEf", "aB1cD2", "000000", "ZzZzZz"};
            when(urlService.existsByShortCode(anyString())).thenReturn(false);

            // When/Then
            for (String code : validCodes) {
                boolean exists = urlHashService.exists(code);
                assertThat(exists).isFalse();
            }

            verify(urlService, times(validCodes.length)).existsByShortCode(anyString());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should demonstrate complete flow with collision resolution")
        void shouldDemonstrateCompleteFlowWithCollisionResolution() {
            // Given
            String longUrl = "https://www.example.com/integration-test";
            String firstAttemptCode = "temp01"; // This will be mocked as existing

            // Mock collision on first attempt, success on second
            when(urlService.existsByShortCode(anyString()))
                    .thenReturn(true)   // First call - collision detected
                    .thenReturn(false); // Second call - no collision

            // When
            String shortCode = urlHashService.generateShortCode(longUrl);

            // Then
            assertThat(shortCode)
                    .isNotNull()
                    .hasSize(6);

            // Verify collision detection was called twice
            verify(urlService, times(2)).existsByShortCode(anyString());
        }

        @Test
        @DisplayName("Should demonstrate usage example")
        void shouldDemonstrateUsageExample() {
            // Given - Setup a realistic scenario
            String longUrl = "https://www.example.com/products/electronics/smartphones/iphone-15-pro";
            when(urlService.existsByShortCode(anyString())).thenReturn(false);

            // When - Generate short code
            String shortCode = urlHashService.generateShortCode(longUrl);

            // Then - Verify and demonstrate usage
            assertThat(shortCode).hasSize(6);

            // Demonstrate checking if code exists
            when(urlService.existsByShortCode(shortCode)).thenReturn(true);
            boolean exists = urlHashService.exists(shortCode);
            assertThat(exists).isTrue();

            // Log the result (in real usage)
            System.out.printf("Generated short code '%s' for URL: %s%n", shortCode, longUrl);
        }
    }
}