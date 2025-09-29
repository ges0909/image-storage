package com.valantic.sti.image.service;

import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.model.ImageDimensions;
import com.valantic.sti.image.model.ImageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 🧪 Unit Tests für ImageUploadService - fokussiert auf Upload-Workflow.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImageUploadService Unit Tests")
class ImageUploadServiceTest {

    @Mock
    private S3StorageService s3StorageService;
    @Mock
    private ImageProcessingService imageProcessingService;
    @Mock
    private ImageMetadataService imageMetadataService;
    @Mock
    private ImageUrlService imageUrlService;
    @Mock
    private ImageValidationService imageValidationService;
    @Mock
    private AsyncImageService asyncImageService;
    @Mock
    private MultipartFile multipartFile;

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        imageUploadService = new ImageUploadService(
            s3StorageService, imageProcessingService, imageMetadataService,
            imageUrlService, imageValidationService, asyncImageService
        );
    }

    @Nested
    @DisplayName("Sync Upload")
    class SyncUpload {

        @Test
        @DisplayName("Should complete full sync upload workflow")
        void uploadSync_ShouldCompleteFullWorkflow() throws IOException {
            // Given
            byte[] imageData = "test-image-data".getBytes();
            when(multipartFile.getBytes()).thenReturn(imageData);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getSize()).thenReturn(1024L);

            when(imageProcessingService.getImageDimensions(imageData))
                .thenReturn(new ImageDimensions(800, 600));

            ImageMetadata savedMetadata = createMockImageMetadata();
            when(imageMetadataService.save(any(ImageMetadata.class))).thenReturn(savedMetadata);

            // When
            ImageResponse result = imageUploadService.uploadSync(multipartFile, "title", "desc", List.of("tag"));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("title");

            verify(imageValidationService).validateImageFile(multipartFile);
            verify(imageValidationService).validateInputs("title", "desc", List.of("tag"));
            verify(s3StorageService).uploadImage(anyString(), eq(imageData), eq("image/jpeg"), any());
            verify(imageProcessingService).generateThumbnails(anyString(), eq(imageData), eq("image/jpeg"));
            verify(imageMetadataService).save(any(ImageMetadata.class));
        }

        @Test
        @DisplayName("Should throw exception when validation fails")
        void uploadSync_ShouldThrowExceptionWhenValidationFails() {
            // Given
            doThrow(new IllegalArgumentException("Invalid file"))
                .when(imageValidationService).validateImageFile(multipartFile);

            // When & Then
            assertThatThrownBy(() -> imageUploadService.uploadSync(multipartFile, "title", "desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid file");
        }

        @Test
        @DisplayName("Should throw exception when S3 upload fails")
        void uploadSync_ShouldThrowExceptionWhenS3UploadFails() throws IOException {
            // Given
            byte[] imageData = "test-data".getBytes();
            when(multipartFile.getBytes()).thenReturn(imageData);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(imageProcessingService.getImageDimensions(imageData))
                .thenReturn(new ImageDimensions(800, 600));
            when(imageMetadataService.save(any(ImageMetadata.class)))
                .thenReturn(createMockImageMetadata());

            doThrow(new RuntimeException("S3 upload failed"))
                .when(s3StorageService).uploadImage(anyString(), any(byte[].class), anyString(), any(Map.class));

            // When & Then
            assertThatThrownBy(() -> imageUploadService.uploadSync(multipartFile, "title", "desc", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 upload failed");
        }
    }

    @Nested
    @DisplayName("Async Upload")
    class AsyncUpload {

        @Test
        @DisplayName("Should save metadata immediately and start async processing")
        void uploadAsync_ShouldSaveMetadataAndStartAsyncProcessing() throws IOException {
            // Given
            byte[] imageData = "test-image-data".getBytes();
            when(multipartFile.getBytes()).thenReturn(imageData);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getSize()).thenReturn(1024L);

            when(imageProcessingService.getImageDimensions(imageData))
                .thenReturn(new ImageDimensions(800, 600));

            ImageMetadata savedMetadata = createMockImageMetadata();
            when(imageMetadataService.save(any(ImageMetadata.class))).thenReturn(savedMetadata);

            // When
            ImageResponse result = imageUploadService.uploadAsync(multipartFile, "title", "desc", List.of("tag"));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("title");

            verify(imageValidationService).validateImageFile(multipartFile);
            verify(imageMetadataService).save(any(ImageMetadata.class));
            verify(asyncImageService).uploadImage(anyString(), eq(imageData), eq("image/jpeg"), any(ImageMetadata.class));
        }

        @Test
        @DisplayName("Should throw exception when async preparation fails")
        void uploadAsync_ShouldThrowExceptionWhenPreparationFails() throws IOException {
            // Given
            when(multipartFile.getBytes()).thenThrow(new IOException("File read error"));

            // When & Then
            assertThatThrownBy(() -> imageUploadService.uploadAsync(multipartFile, "title", "desc", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Upload preparation failed");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should validate file and inputs before processing")
        void shouldValidateInputsBeforeProcessing() throws IOException {
            // Given
            when(multipartFile.getBytes()).thenReturn("data".getBytes());
            when(imageProcessingService.getImageDimensions(any()))
                .thenReturn(new ImageDimensions(800, 600));
            when(imageMetadataService.save(any())).thenReturn(createMockImageMetadata());

            // When
            imageUploadService.uploadSync(multipartFile, "title", "desc", List.of("tag"));

            // Then
            verify(imageValidationService).validateImageFile(multipartFile);
            verify(imageValidationService).validateInputs("title", "desc", List.of("tag"));
        }
    }

    // Helper Methods
    private ImageMetadata createMockImageMetadata() {
        return new ImageMetadata(
            "test-id", "title", "desc", Set.of("tag"),
            "image/jpeg", 1024L, 800, 600,
            "images/test-id/original", "user"
        );
    }
}
