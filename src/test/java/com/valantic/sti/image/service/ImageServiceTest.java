package com.valantic.sti.image.service;

import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.entity.ImageMetadata;
import com.valantic.sti.image.model.ImageDimensions;
import com.valantic.sti.image.model.ImageResponse;
import com.valantic.sti.image.model.ImageSize;
import com.valantic.sti.image.model.ImageUpdateRequest;
import com.valantic.sti.image.model.SearchRequest;
import com.valantic.sti.image.model.SearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 🧪 Unit Tests für ImageService - fokussiert auf Orchestrierung und Delegation.
 * <p>
 * Nach Refactoring testet ImageService primär:
 * - Delegation an spezialisierte Services
 * - Orchestrierung von Service-Aufrufen
 * - Response-Building und Mapping
 * - Exception-Handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImageService Unit Tests")
class ImageServiceTest {

    @Mock
    private ImageUploadService imageUploadService;
    @Mock
    private ImageMetadataService imageMetadataService;
    @Mock
    private ImageUrlService imageUrlService;
    @Mock
    private S3StorageService s3StorageService;
    @Mock
    private MultipartFile multipartFile;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        ImageProperties imageProperties = new ImageProperties(
            "test-bucket", "test-thumbnails", "test-kms-key", "https://cdn.example.com",
            10485760L, "eu-central-1", 15, new int[]{150, 300, 600},
            Set.of("image/jpeg", "image/png"), 1000, "images"
        );

        imageService = new ImageService(
            imageUploadService, imageMetadataService, imageUrlService,
            s3StorageService, imageProperties
        );
    }

    @Nested
    @DisplayName("Upload Operations")
    class UploadOperations {

        @Test
        @DisplayName("Should delegate sync upload to ImageUploadService")
        void uploadImage_ShouldDelegateToUploadService() {
            // Given
            ImageResponse expectedResponse = createMockImageResponse();
            when(imageUploadService.uploadSync(multipartFile, "title", "desc", List.of("tag")))
                .thenReturn(expectedResponse);

            // When
            ImageResponse result = imageService.uploadImage(multipartFile, "title", "desc", List.of("tag"));

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(imageUploadService).uploadSync(multipartFile, "title", "desc", List.of("tag"));
        }

        @Test
        @DisplayName("Should delegate async upload to ImageUploadService")
        void uploadImageAsync_ShouldDelegateToUploadService() {
            // Given
            ImageResponse expectedResponse = createMockImageResponse();
            when(imageUploadService.uploadAsync(multipartFile, "title", "desc", List.of("tag")))
                .thenReturn(expectedResponse);

            // When
            ImageResponse result = imageService.uploadImageAsync(multipartFile, "title", "desc", List.of("tag"));

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(imageUploadService).uploadAsync(multipartFile, "title", "desc", List.of("tag"));
        }
    }

    @Nested
    @DisplayName("Metadata Operations")
    class MetadataOperations {

        @Test
        @DisplayName("Should retrieve and build image response from metadata")
        void getImageMetadata_ShouldReturnImageResponse() {
            // Given
            String imageId = "test-image-id";
            ImageMetadata metadata = mock(ImageMetadata.class);
            when(metadata.getImageId()).thenReturn(imageId);
            when(metadata.getTitle()).thenReturn("Test Image");
            when(metadata.getDescription()).thenReturn("Description");
            when(metadata.getTags()).thenReturn(Set.of("tag1"));
            when(metadata.getContentType()).thenReturn("image/jpeg");
            when(metadata.getFileSize()).thenReturn(1024L);
            when(metadata.getWidth()).thenReturn(800);
            when(metadata.getHeight()).thenReturn(600);
            when(metadata.getCreatedAt()).thenReturn(LocalDateTime.now());
            when(metadata.getUpdatedAt()).thenReturn(LocalDateTime.now());
            when(metadata.getUploadedBy()).thenReturn("user");
            when(imageMetadataService.findById(imageId)).thenReturn(metadata);
            com.valantic.sti.image.model.ImageUrls mockUrls = mock(com.valantic.sti.image.model.ImageUrls.class);
            when(imageUrlService.buildImageUrls(imageId)).thenReturn(mockUrls);

            // When
            ImageResponse result = imageService.getImageMetadata(imageId);

            // Then
            assertThat(result.id()).isEqualTo(imageId);
            assertThat(result.title()).isEqualTo("Test Image");
            verify(imageMetadataService).findById(imageId);
        }

        @Test
        @DisplayName("Should update metadata and return updated response")
        void updateImageMetadata_ShouldUpdateAndReturnResponse() {
            // Given
            String imageId = "test-image-id";
            ImageMetadata metadata = mock(ImageMetadata.class);
            when(metadata.getImageId()).thenReturn(imageId);
            when(metadata.getTitle()).thenReturn("New Title");
            when(metadata.getDescription()).thenReturn("New Desc");
            when(metadata.getTags()).thenReturn(Set.of("new-tag"));
            when(metadata.getContentType()).thenReturn("image/jpeg");
            when(metadata.getFileSize()).thenReturn(1024L);
            when(metadata.getWidth()).thenReturn(800);
            when(metadata.getHeight()).thenReturn(600);
            when(metadata.getCreatedAt()).thenReturn(LocalDateTime.now());
            when(metadata.getUpdatedAt()).thenReturn(LocalDateTime.now());
            when(metadata.getUploadedBy()).thenReturn("user");
            ImageUpdateRequest request = new ImageUpdateRequest("New Title", "New Desc", List.of("new-tag"));

            when(imageMetadataService.findById(imageId)).thenReturn(metadata);
            when(imageMetadataService.save(metadata)).thenReturn(metadata);
            com.valantic.sti.image.model.ImageUrls mockUrls = mock(com.valantic.sti.image.model.ImageUrls.class);
            when(imageUrlService.buildImageUrls(imageId)).thenReturn(mockUrls);

            // When
            ImageResponse result = imageService.updateImageMetadata(imageId, request);

            // Then
            assertThat(result.title()).isEqualTo("New Title");
            verify(imageMetadataService).save(metadata);
        }
    }

    @Nested
    @DisplayName("Search Operations")
    class SearchOperations {

        @Test
        @DisplayName("Should delegate search to ImageMetadataService")
        void searchImages_ShouldDelegateToMetadataService() {
            // Given
            SearchRequest request = new SearchRequest("query", null, null, 0, 10, "createdAt", "desc");
            SearchResponse expectedResponse = new SearchResponse(List.of(), 0L, 0, 0, 10);
            when(imageMetadataService.searchImages(request)).thenReturn(expectedResponse);

            // When
            SearchResponse result = imageService.searchImages(request);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(imageMetadataService).searchImages(request);
        }

        @Test
        @DisplayName("Should create search request for listImages")
        void listImages_ShouldCreateSearchRequestAndDelegate() {
            // Given
            SearchResponse expectedResponse = new SearchResponse(List.of(), 0L, 0, 0, 20);
            when(imageMetadataService.searchImages(any(SearchRequest.class))).thenReturn(expectedResponse);

            // When
            List<ImageResponse> result = imageService.listImages(0, 20);

            // Then
            assertThat(result).isEmpty();
            verify(imageMetadataService).searchImages(any(SearchRequest.class));
        }
    }

    @Nested
    @DisplayName("URL Generation")
    class UrlGeneration {

        @Test
        @DisplayName("Should generate signed URL for original image")
        void generateSignedUrl_ShouldGenerateUrlForOriginal() {
            // Given
            String imageId = "test-image-id";
            ImageMetadata metadata = mock(ImageMetadata.class);
            when(metadata.getS3Key()).thenReturn("images/test-image-id/original");
            when(imageMetadataService.findById(imageId)).thenReturn(metadata);
            when(imageUrlService.generatePresignedUrl(eq("test-bucket"), eq("images/test-image-id/original"), eq(10)))
                .thenReturn("https://signed-url.com");

            // When
            String result = imageService.generateSignedUrl(imageId, ImageSize.ORIGINAL, Duration.ofMinutes(10));

            // Then
            assertThat(result).isEqualTo("https://signed-url.com");
            verify(imageUrlService).generatePresignedUrl("test-bucket", "images/test-image-id/original", 10);
        }

        @Test
        @DisplayName("Should generate signed URL for thumbnail")
        void generateSignedUrl_ShouldGenerateUrlForThumbnail() {
            // Given
            String imageId = "test-image-id";
            ImageMetadata metadata = mock(ImageMetadata.class);
            when(imageMetadataService.findById(imageId)).thenReturn(metadata);
            when(imageUrlService.generatePresignedUrl(eq("test-thumbnails"), anyString(), eq(5)))
                .thenReturn("https://thumbnail-url.com");

            // When
            String result = imageService.generateSignedUrl(imageId, ImageSize.THUMBNAIL_300, Duration.ofMinutes(5));

            // Then
            assertThat(result).isEqualTo("https://thumbnail-url.com");
            verify(imageUrlService).generatePresignedUrl(eq("test-thumbnails"), eq("thumbnails/test-image-id/300.webp"), eq(5));
        }

        @Test
        @DisplayName("Should throw exception for original size in getThumbnailUrl")
        void getThumbnailUrl_ShouldThrowExceptionForOriginalSize() {
            // When & Then
            assertThatThrownBy(() -> imageService.getThumbnailUrl("test-id", ImageSize.ORIGINAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Use generateSignedUrl for original images");
        }

        @Test
        @DisplayName("Should generate thumbnail URL")
        void getThumbnailUrl_ShouldGenerateThumbnailUrl() {
            // Given
            when(imageUrlService.generatePresignedUrl(eq("test-thumbnails"), eq("thumbnails/test-id/300.webp")))
                .thenReturn("https://thumbnail.com");

            // When
            String result = imageService.getThumbnailUrl("test-id", ImageSize.THUMBNAIL_300);

            // Then
            assertThat(result).isEqualTo("https://thumbnail.com");
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete image from S3 and metadata")
        void deleteImage_ShouldDeleteFromS3AndMetadata() {
            // Given
            String imageId = "test-image-id";
            ImageMetadata metadata = mock(ImageMetadata.class);
            when(metadata.getS3Key()).thenReturn("images/test-image-id/original");
            when(imageMetadataService.findById(imageId)).thenReturn(metadata);

            // When
            imageService.deleteImage(imageId);

            // Then
            verify(s3StorageService).deleteImage("images/test-image-id/original");
            verify(imageMetadataService).deleteById(imageId);
        }
    }

    @Nested
    @DisplayName("Tag Operations")
    class TagOperations {

        @Test
        @DisplayName("Should add tags to existing image")
        void addTags_ShouldAddTagsToImage() {
            // Given
            String imageId = "test-image-id";
            ImageMetadata metadata = mock(ImageMetadata.class);
            when(metadata.getTags()).thenReturn(new java.util.HashSet<>(Set.of("tag1")));
            when(imageMetadataService.findById(imageId)).thenReturn(metadata);

            // When
            imageService.addTags(imageId, List.of("new-tag"));

            // Then
            verify(imageMetadataService).save(metadata);
            verify(metadata).setTags(any(Set.class));
        }

        @Test
        @DisplayName("Should remove tags from existing image")
        void removeTags_ShouldRemoveTagsFromImage() {
            // Given
            String imageId = "test-image-id";
            ImageMetadata metadata = mock(ImageMetadata.class);
            when(metadata.getTags()).thenReturn(new java.util.HashSet<>(Set.of("tag1", "remove-me")));
            when(imageMetadataService.findById(imageId)).thenReturn(metadata);

            // When
            imageService.removeTags(imageId, List.of("remove-me"));

            // Then
            verify(imageMetadataService).save(metadata);
            verify(metadata).setTags(any(Set.class));
        }
    }

    // Helper Methods
    private ImageResponse createMockImageResponse() {
        com.valantic.sti.image.model.ImageUrls mockUrls = mock(com.valantic.sti.image.model.ImageUrls.class);
        return new ImageResponse(
            "test-id", "Test Image", "Description", List.of("tag"),
            "image/jpeg", 1024L, new ImageDimensions(800, 600),
            LocalDateTime.now(), LocalDateTime.now(), "user", mockUrls
        );
    }


}
