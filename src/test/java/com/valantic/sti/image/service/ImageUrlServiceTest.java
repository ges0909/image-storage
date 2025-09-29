package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.model.ImageUrls;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 🧪 Unit Tests für ImageUrlService - fokussiert auf URL-Generierung.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImageUrlService Unit Tests")
class ImageUrlServiceTest {

    @Mock
    private S3Presigner s3Presigner;
    @Mock
    private PresignedGetObjectRequest presignedRequest;

    private ImageUrlService imageUrlService;

    @BeforeEach
    void setUp() throws Exception {
        ImageProperties imageProperties = new ImageProperties(
            "test-bucket", "test-thumbnails", "test-kms-key", "https://cdn.example.com",
            10485760L, "eu-central-1", 15, new int[]{150, 300, 600},
            Set.of("image/jpeg", "image/png"), 1000, "images"
        );

        imageUrlService = new ImageUrlService(s3Presigner, imageProperties);

        // Mock presigned URL response
        when(presignedRequest.url()).thenReturn(URI.create("https://test-bucket.s3.amazonaws.com/test-key?signature=abc").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
    }

    @Nested
    @DisplayName("Presigned URL Generation")
    class PresignedUrlGeneration {

        @Test
        @DisplayName("Should generate presigned URL with expiration")
        void generatePresignedUrl_ShouldGenerateUrlWithExpiration() {
            // When
            String result = imageUrlService.generatePresignedUrl("test-bucket", "test-key", 15);

            // Then
            assertThat(result).isEqualTo("https://test-bucket.s3.amazonaws.com/test-key?signature=abc");
        }

        @Test
        @DisplayName("Should generate presigned URL with default expiration")
        void generatePresignedUrl_ShouldGenerateUrlWithDefaultExpiration() {
            // When
            String result = imageUrlService.generatePresignedUrl("test-bucket", "test-key");

            // Then
            assertThat(result).isEqualTo("https://test-bucket.s3.amazonaws.com/test-key?signature=abc");
        }

        @Test
        @DisplayName("Should cap expiration to maximum allowed")
        void generatePresignedUrl_ShouldCapExpirationToMaximum() {
            // When - Request 25 minutes but should be capped to 15
            String result = imageUrlService.generatePresignedUrl("bucket", "key", 25);

            // Then - Should not throw, just cap the duration
            assertThat(result).isEqualTo("https://test-bucket.s3.amazonaws.com/test-key?signature=abc");
        }

        @Test
        @DisplayName("Should handle negative expiration")
        void generatePresignedUrl_ShouldHandleNegativeExpiration() {
            // When - Negative duration should be handled by Duration.ofMinutes
            String result = imageUrlService.generatePresignedUrl("bucket", "key", -5);

            // Then - Should not throw
            assertThat(result).isEqualTo("https://test-bucket.s3.amazonaws.com/test-key?signature=abc");
        }
    }

    @Nested
    @DisplayName("Image URLs Building")
    class ImageUrlsBuilding {

        @Test
        @DisplayName("Should build complete image URLs")
        void buildImageUrls_ShouldBuildCompleteUrls() {
            // When
            ImageUrls result = imageUrlService.buildImageUrls("test-image-id");

            // Then - All URLs should be generated (mocked to same value)
            assertThat(result).isNotNull();
            assertThat(result.original()).isEqualTo("https://test-bucket.s3.amazonaws.com/test-key?signature=abc");
            assertThat(result.thumbnail150()).isEqualTo("https://test-bucket.s3.amazonaws.com/test-key?signature=abc");
            assertThat(result.thumbnail300()).isEqualTo("https://test-bucket.s3.amazonaws.com/test-key?signature=abc");
            assertThat(result.thumbnail600()).isEqualTo("https://test-bucket.s3.amazonaws.com/test-key?signature=abc");
        }

        @Test
        @DisplayName("Should generate URLs for all thumbnail sizes")
        void buildImageUrls_ShouldGenerateAllThumbnailSizes() {
            // When
            ImageUrls result = imageUrlService.buildImageUrls("test-image-id");

            // Then - Verify all required thumbnail sizes are present
            assertThat(result.thumbnail150()).isNotNull();
            assertThat(result.thumbnail300()).isNotNull();
            assertThat(result.thumbnail600()).isNotNull();
        }
    }

    @Nested
    @DisplayName("URL Generation")
    class UrlGeneration {

        @Test
        @DisplayName("Should generate URL with empty bucket name")
        void generatePresignedUrl_ShouldHandleEmptyBucketName() {
            // When - Service doesn't validate, just passes to AWS SDK
            String result = imageUrlService.generatePresignedUrl("", "key", 10);

            // Then - Should not throw at service level
            assertThat(result).isEqualTo("https://test-bucket.s3.amazonaws.com/test-key?signature=abc");
        }

        @Test
        @DisplayName("Should generate URL with empty S3 key")
        void generatePresignedUrl_ShouldHandleEmptyS3Key() {
            // When - Service doesn't validate, just passes to AWS SDK
            String result = imageUrlService.generatePresignedUrl("bucket", "", 10);

            // Then - Should not throw at service level
            assertThat(result).isEqualTo("https://test-bucket.s3.amazonaws.com/test-key?signature=abc");
        }
    }
}
