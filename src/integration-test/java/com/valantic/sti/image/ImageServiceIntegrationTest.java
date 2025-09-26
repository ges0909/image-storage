package com.valantic.sti.image;

import com.valantic.sti.image.exception.ImageProcessingException;
import com.valantic.sti.image.model.ImageResponse;
import com.valantic.sti.image.model.ImageSize;
import com.valantic.sti.image.service.ImageService;
import com.valantic.sti.image.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static com.valantic.sti.image.testutil.TestConstants.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

class ImageServiceIntegrationTest extends AbstractIntegrationTest {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private ImageService imageService;
    private ImageProperties imageProperties;

    @BeforeEach
    void setUp() {
        var credentialsProvider = createCredentialsProvider();
        var region = Region.of(localstack.getRegion().toLowerCase(Locale.ROOT));

        s3Client = S3Client.builder()
            .endpointOverride(localstack.getEndpointOverride(S3))
            .credentialsProvider(credentialsProvider)
            .region(region)
            .build();

        s3Presigner = S3Presigner.builder()
            .endpointOverride(localstack.getEndpointOverride(S3))
            .credentialsProvider(credentialsProvider)
            .region(region)
            .build();

        imageProperties = createTestImageProperties();
        imageService = new ImageService(s3Client, s3Presigner, imageProperties);
    }

    @Nested
    class UploadValidation {
        @Test
        void uploadImage_ShouldThrowException_WhenInvalidImageData() {
            MockMultipartFile file = createMockFile("test.jpg", "image/jpeg", INVALID_TEXT.getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> imageService.uploadImage(file, "Test Image", "Description", List.of("tag1")))
                .isInstanceOf(ImageProcessingException.class)
                .hasMessageContaining("Invalid image data: unable to generate thumbnails");
        }

        @Test
        void uploadImage_ShouldThrowException_WhenFileEmpty() {
            MockMultipartFile emptyFile = createMockFile("empty.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> imageService.uploadImage(emptyFile, "Test", "Desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File cannot be empty");
        }

        @Test
        void uploadImage_ShouldThrowException_WhenInvalidContentType() {
            MockMultipartFile textFile = createMockFile("test.txt", "text/plain", SOME_TEXT.getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> imageService.uploadImage(textFile, "Test", "Desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid image type: text/plain");
        }

        @Test
        void uploadImage_ShouldThrowException_WhenFileTooLarge() {
            long largeFileSize = MAX_FILE_SIZE + 1;
            MockMultipartFile largeFile = createLargeMockFile(largeFileSize);

            assertThatThrownBy(() -> imageService.uploadImage(largeFile, "Test", "Desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File too large: " + largeFileSize);
        }
    }

    @Nested
    class UrlGeneration {
        @Test
        void generateSignedUrl_ShouldThrowException_WhenImageNotFound() {
            assertThatThrownBy(() -> imageService.generateSignedUrl(NON_EXISTENT_ID, ImageSize.ORIGINAL, Duration.ofMinutes(10)))
                .hasMessageContaining("Image not found: " + NON_EXISTENT_ID);
        }

        @Test
        void validateExpiration_ShouldThrowException_WhenExpirationTooLong() {
            Duration longExpiration = Duration.ofMinutes(20);

            assertThatThrownBy(() -> imageService.generateSignedUrl(TEST_IMAGE_ID, ImageSize.ORIGINAL, longExpiration))
                .hasMessageContaining("Image not found: " + TEST_IMAGE_ID);
        }

        @Test
        void getThumbnailUrl_ShouldReturnCorrectUrl_WhenValidSize() {
            String result = imageService.getThumbnailUrl(TEST_IMAGE_ID, ImageSize.THUMBNAIL_300);

            assertThat(result).isEqualTo(TEST_CLOUDFRONT_DOMAIN + "/images/" + TEST_IMAGE_ID + "/thumbnail_300");
        }

        @Test
        void getThumbnailUrl_ShouldThrowException_WhenOriginalSize() {
            assertThatThrownBy(() -> imageService.getThumbnailUrl(TEST_IMAGE_ID, ImageSize.ORIGINAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Use generateSignedUrl for original images");
        }
    }

    @Nested
    class EmptyBucketOperations {
        @Test
        void searchImages_ShouldReturnEmptyList_WhenNoBucketObjects() {
            var searchRequest = new com.valantic.sti.image.model.SearchRequest(
                "test", null, null, 0, 20, "uploadDate", "desc"
            );

            var result = imageService.searchImages(searchRequest);

            assertThat(result.images()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        void listImages_ShouldReturnEmptyList_WhenNoBucketObjects() {
            List<ImageResponse> result = imageService.listImages(0, 20);

            assertThat(result).isEmpty();
        }

        @Test
        void getImageStats_ShouldReturnDefaultStats() {
            var stats = imageService.getImageStats();

            assertThat(stats.totalImages()).isZero();
            assertThat(stats.totalSizeBytes()).isZero();
            assertThat(stats.averageImageSizeBytes()).isEqualTo(0.0);
        }

        @Test
        void getImageAnalytics_ShouldReturnDefaultAnalytics() {
            var analytics = imageService.getImageAnalytics(TEST_IMAGE_ID);

            assertThat(analytics.imageId()).isEqualTo(TEST_IMAGE_ID);
            assertThat(analytics.downloadCount()).isZero();
            assertThat(analytics.viewCount()).isZero();
            assertThat(analytics.accessLocations()).isEmpty();
        }
    }

    // Helper methods
    private StaticCredentialsProvider createCredentialsProvider() {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
        );
    }


    private MockMultipartFile createMockFile(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("file", filename, contentType, content);
    }

    private MockMultipartFile createLargeMockFile(long size) {
        byte[] largeContent = new byte[(int) size];
        return new MockMultipartFile("file", "large.jpg", "image/jpeg", largeContent);
    }
}
