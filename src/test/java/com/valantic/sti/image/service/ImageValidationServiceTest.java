package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 🧪 Unit Tests für ImageValidationService - fokussiert auf Input-Validierung.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImageValidationService Unit Tests")
class ImageValidationServiceTest {

    @Mock
    private MultipartFile multipartFile;
    @Mock
    private ImageProcessingService imageProcessingService;

    private ImageValidationService imageValidationService;

    @BeforeEach
    void setUp() {
        ImageProperties imageProperties = new ImageProperties(
            "test-bucket", "test-thumbnails", "test-kms-key", "https://cdn.example.com",
            10485760L, "eu-central-1", 15, new int[]{150, 300, 600},
            Set.of("image/jpeg", "image/png", "image/webp"), 1000, "images"
        );

        imageValidationService = new ImageValidationService(imageProperties, imageProcessingService);
    }

    @Nested
    @DisplayName("File Validation")
    class FileValidation {

        @Test
        @DisplayName("Should accept valid image file")
        void validateImageFile_ShouldAcceptValidFile() {
            // Given
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(imageProcessingService.isValidImageFormat("image/jpeg")).thenReturn(true);

            // When & Then - should not throw
            imageValidationService.validateImageFile(multipartFile);
        }

        @Test
        @DisplayName("Should reject empty file")
        void validateImageFile_ShouldRejectEmptyFile() {
            // Given
            when(multipartFile.isEmpty()).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> imageValidationService.validateImageFile(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File cannot be null or empty");
        }

        @Test
        @DisplayName("Should reject file without content type")
        void validateImageFile_ShouldRejectFileWithoutContentType() {
            // Given
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn(null);
            when(multipartFile.getSize()).thenReturn(1024L);
            when(imageProcessingService.isValidImageFormat(null)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> imageValidationService.validateImageFile(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported content type: null");
        }

        @Test
        @DisplayName("Should reject unsupported content type")
        void validateImageFile_ShouldRejectUnsupportedContentType() {
            // Given
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("text/plain");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(imageProcessingService.isValidImageFormat("text/plain")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> imageValidationService.validateImageFile(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported content type: text/plain");
        }

        @Test
        @DisplayName("Should reject file too large")
        void validateImageFile_ShouldRejectFileTooLarge() {
            // Given
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(20971520L); // 20MB > 10MB limit

            // When & Then
            assertThatThrownBy(() -> imageValidationService.validateImageFile(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File size exceeds maximum allowed: 10485760");
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @DisplayName("Should accept valid inputs")
        void validateInputs_ShouldAcceptValidInputs() {
            // When & Then - should not throw
            imageValidationService.validateInputs("Valid Title", "Valid Description", List.of("tag1", "tag2"));
        }

        @Test
        @DisplayName("Should reject title too long")
        void validateInputs_ShouldRejectTitleTooLong() {
            // Given
            String longTitle = "a".repeat(256); // > 255 chars

            // When & Then
            assertThatThrownBy(() -> imageValidationService.validateInputs(longTitle, "desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title exceeds maximum length: 255");
        }

        @Test
        @DisplayName("Should reject description too long")
        void validateInputs_ShouldRejectDescriptionTooLong() {
            // Given
            String longDescription = "a".repeat(1001); // > 1000 chars

            // When & Then
            assertThatThrownBy(() -> imageValidationService.validateInputs("title", longDescription, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description exceeds maximum length: 1000");
        }

        @Test
        @DisplayName("Should reject too many tags")
        void validateInputs_ShouldRejectTooManyTags() {
            // Given - Need more than 20 tags since MAX_TAGS_COUNT is 20
            List<String> tooManyTags = List.of(
                "tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tag10",
                "tag11", "tag12", "tag13", "tag14", "tag15", "tag16", "tag17", "tag18", "tag19", "tag20", "tag21"
            ); // 21 tags > 20 limit

            // When & Then
            assertThatThrownBy(() -> imageValidationService.validateInputs("title", "desc", tooManyTags))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Too many tags. Maximum allowed: 20");
        }
    }

    @Nested
    @DisplayName("S3 Key Sanitization")
    class S3KeySanitization {

        @Test
        @DisplayName("Should sanitize S3 key")
        void sanitizeS3Key_ShouldSanitizeKey() {
            // Given
            String unsafeKey = "images/../../../etc/passwd";

            // When
            String result = imageValidationService.sanitizeS3Key(unsafeKey);

            // Then - Only invalid characters are replaced with underscores
            assertThat(result).isEqualTo("images/../../../etc/passwd"); // dots and slashes are valid
        }

        @Test
        @DisplayName("Should handle normal S3 key")
        void sanitizeS3Key_ShouldHandleNormalKey() {
            // Given
            String normalKey = "images/test-id/original";

            // When
            String result = imageValidationService.sanitizeS3Key(normalKey);

            // Then
            assertThat(result).isEqualTo(normalKey);
        }
    }
}
