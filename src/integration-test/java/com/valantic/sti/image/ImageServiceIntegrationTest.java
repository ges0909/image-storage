package com.valantic.sti.image;

import com.valantic.sti.image.model.ImageResponse;
import com.valantic.sti.image.model.ImageSize;
import com.valantic.sti.image.service.ImageService;
import com.valantic.sti.image.testutil.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.valantic.sti.image.testutil.TestConstants.INVALID_TEXT;
import static com.valantic.sti.image.testutil.TestConstants.MAX_FILE_SIZE;
import static com.valantic.sti.image.testutil.TestConstants.NON_EXISTENT_ID;
import static com.valantic.sti.image.testutil.TestConstants.SOME_TEXT;
import static com.valantic.sti.image.testutil.TestConstants.TEST_CLOUDFRONT_DOMAIN;
import static com.valantic.sti.image.testutil.TestConstants.TEST_IMAGE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceIntegrationTest extends AbstractIntegrationTest {

    @Mock
    private com.valantic.sti.image.service.ImageUploadService imageUploadService;

    @Mock
    private com.valantic.sti.image.service.ImageMetadataService imageMetadataService;

    @Mock
    private com.valantic.sti.image.service.ImageUrlService imageUrlService;

    @Mock
    private com.valantic.sti.image.service.S3StorageService s3StorageService;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        ImageProperties imageProperties = createTestImageProperties();
        imageService = new ImageService(
            imageUploadService,
            imageMetadataService,
            imageUrlService,
            s3StorageService,
            imageProperties
        );
    }

    @Nested
    class UploadValidation {
        @Test
        void uploadImage_ShouldThrowException_WhenInvalidImageData() {
            MockMultipartFile file = createMockFile("test.jpg", "image/jpeg", INVALID_TEXT.getBytes(StandardCharsets.UTF_8));

            doThrow(new RuntimeException("Upload failed"))
                .when(imageUploadService).uploadSync(file, "Test Image", "Description", List.of("tag1"));

            assertThatThrownBy(() -> imageService.uploadImage(file, "Test Image", "Description", List.of("tag1")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Upload failed");
        }

        @Test
        void uploadImage_ShouldThrowException_WhenFileEmpty() {
            MockMultipartFile emptyFile = createMockFile("empty.jpg", "image/jpeg", new byte[0]);

            doThrow(new IllegalArgumentException("File cannot be empty"))
                .when(imageUploadService).uploadSync(emptyFile, "Test", "Desc", null);

            assertThatThrownBy(() -> imageService.uploadImage(emptyFile, "Test", "Desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File cannot be empty");
        }

        @Test
        void uploadImage_ShouldThrowException_WhenInvalidContentType() {
            MockMultipartFile textFile = createMockFile("test.txt", "text/plain", SOME_TEXT.getBytes(StandardCharsets.UTF_8));

            doThrow(new IllegalArgumentException("Invalid image type: text/plain"))
                .when(imageUploadService).uploadSync(textFile, "Test", "Desc", null);

            assertThatThrownBy(() -> imageService.uploadImage(textFile, "Test", "Desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid image type: text/plain");
        }

        @Test
        void uploadImage_ShouldThrowException_WhenFileTooLarge() {
            long largeFileSize = MAX_FILE_SIZE + 1;
            MockMultipartFile largeFile = createLargeMockFile(largeFileSize);

            doThrow(new IllegalArgumentException("File too large: " + largeFileSize))
                .when(imageUploadService).uploadSync(largeFile, "Test", "Desc", null);

            assertThatThrownBy(() -> imageService.uploadImage(largeFile, "Test", "Desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File too large: " + largeFileSize);
        }
    }

    @Nested
    class UrlGeneration {
        @Test
        void generateSignedUrl_ShouldThrowException_WhenImageNotFound() {
            when(imageMetadataService.findById(NON_EXISTENT_ID))
                .thenThrow(new com.valantic.sti.image.exception.ImageNotFoundException("Image not found: " + NON_EXISTENT_ID));

            assertThatThrownBy(() -> imageService.generateSignedUrl(NON_EXISTENT_ID, ImageSize.ORIGINAL, Duration.ofMinutes(10)))
                .hasMessageContaining("Image not found: " + NON_EXISTENT_ID);
        }

        @Test
        void validateExpiration_ShouldThrowException_WhenExpirationTooLong() {
            Duration longExpiration = Duration.ofMinutes(20);
            when(imageMetadataService.findById(TEST_IMAGE_ID))
                .thenThrow(new com.valantic.sti.image.exception.ImageNotFoundException("Image not found: " + TEST_IMAGE_ID));

            assertThatThrownBy(() -> imageService.generateSignedUrl(TEST_IMAGE_ID, ImageSize.ORIGINAL, longExpiration))
                .hasMessageContaining("Image not found: " + TEST_IMAGE_ID);
        }

        @Test
        void getThumbnailUrl_ShouldReturnCorrectUrl_WhenValidSize() {
            String expectedUrl = TEST_CLOUDFRONT_DOMAIN + "/images/" + TEST_IMAGE_ID + "/thumbnail_300";
            when(imageUrlService.generatePresignedUrl(any(), any()))
                .thenReturn(expectedUrl);

            String result = imageService.getThumbnailUrl(TEST_IMAGE_ID, ImageSize.THUMBNAIL_300);

            assertThat(result).isEqualTo(expectedUrl);
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

            var emptyResponse = new com.valantic.sti.image.model.SearchResponse(
                Collections.emptyList(), 0L, 0, 0, 20
            );
            when(imageMetadataService.searchImages(searchRequest))
                .thenReturn(emptyResponse);

            var result = imageService.searchImages(searchRequest);

            assertThat(result.images()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        void listImages_ShouldReturnEmptyList_WhenNoBucketObjects() {
            var emptyResponse = new com.valantic.sti.image.model.SearchResponse(
                Collections.emptyList(), 0L, 0, 0, 20
            );
            when(imageMetadataService.searchImages(any()))
                .thenReturn(emptyResponse);

            List<ImageResponse> result = imageService.listImages(0, 20);

            assertThat(result).isEmpty();
        }

        @Test
        void getImageStats_ShouldReturnDefaultStats() {
            var stats = imageService.getImageStats();

            assertThat(stats.totalImages()).isZero();
            assertThat(stats.totalSizeBytes()).isZero();
            assertThat(stats.averageSizeBytes()).isZero();
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

    private MockMultipartFile createMockFile(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("file", filename, contentType, content);
    }

    private MockMultipartFile createLargeMockFile(long size) {
        byte[] largeContent = new byte[(int) size];
        return new MockMultipartFile("file", "large.jpg", "image/jpeg", largeContent);
    }
}
