package com.valantic.sti.image.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valantic.sti.image.ImageProperties;
import com.valantic.sti.image.model.ImageAnalytics;
import com.valantic.sti.image.model.ImageDimensions;
import com.valantic.sti.image.model.ImageResponse;
import com.valantic.sti.image.model.ImageSize;
import com.valantic.sti.image.model.ImageStats;
import com.valantic.sti.image.model.ImageUpdateRequest;
import com.valantic.sti.image.model.ImageUrls;
import com.valantic.sti.image.model.ImageVersion;
import com.valantic.sti.image.model.SearchRequest;
import com.valantic.sti.image.model.SearchResponse;
import com.valantic.sti.image.service.ImageService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
@ContextConfiguration(classes = {ImageController.class, ImageControllerTest.TestConfig.class})
@Import(TestSecurityConfig.class)
class ImageControllerTest {

    private static final String VALID_UUID = "12345678-1234-1234-1234-123456789012";
    private static final String VERSION_UUID = "87654321-4321-4321-4321-210987654321";
    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final String SIGNED_URL = "https://example.com/signed-url";
    private static final String THUMBNAIL_URL = "http://localhost:9000/images/test-id/thumbnail_300";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImageService imageService;

    @Configuration
    static class TestConfig {
        @Bean
        public ImageService imageService() {
            return mock(ImageService.class);
        }

        @Bean
        public ImageProperties imageProperties() {
            return new ImageProperties(
                "test-bucket",
                "test-thumbnails",
                "test-kms-key",
                "http://localhost:9000",
                10485760L,
                "us-east-1",
                15,
                new int[]{150, 300, 600},
                java.util.Set.of("image/jpeg", "image/png", "image/webp"),
                1000,
                "images"
            );
        }
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class ImageManagement {
        @Test
        void uploadImage_ShouldReturn201_WhenValidFile() throws Exception {
            MockMultipartFile file = createValidJpegFile();
            ImageResponse response = createImageResponse("test-id", "Test Image", "Description");
            when(imageService.uploadImage(any(), eq("Test Image"), eq("Description"), eq(List.of("tag1"))))
                .thenReturn(response);

            mockMvc.perform(multipart("/api/images")
                    .file(file)
                    .param("title", "Test Image")
                    .param("description", "Description")
                    .param("tags", "tag1"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/images/test-id"))
                .andExpect(jsonPath("$.id").value("test-id"))
                .andExpect(jsonPath("$.title").value("Test Image"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
        }

        @Test
        void getImageMetadata_ShouldReturn200_WhenImageExists() throws Exception {
            ImageResponse response = createImageResponse(VALID_UUID, "Test Image", "Description");
            when(imageService.getImageMetadata(VALID_UUID)).thenReturn(response);

            mockMvc.perform(get("/api/images/{imageId}", VALID_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALID_UUID))
                .andExpect(jsonPath("$.title").value("Test Image"))
                .andExpect(jsonPath("$.tags[0]").value("tag1"));
        }

        @Test
        void deleteImage_ShouldReturn204_WhenImageExists() throws Exception {
            mockMvc.perform(delete("/api/images/{imageId}", VALID_UUID))
                .andExpect(status().isNoContent());
        }

        @Test
        void updateImage_ShouldReturn200_WhenValidRequest() throws Exception {
            ImageUpdateRequest request = new ImageUpdateRequest("Updated Title", "Updated Description", List.of("new-tag"));
            ImageResponse response = createImageResponse(VALID_UUID, "Updated Title", "Updated Description");
            when(imageService.updateImageMetadata(VALID_UUID, request)).thenReturn(response);

            mockMvc.perform(put("/api/images/{imageId}", VALID_UUID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"));
        }
    }

    @Nested
    class DownloadAndAccess {
        @Test
        void getDownloadUrl_ShouldReturn200_WhenValidRequest() throws Exception {
            when(imageService.generateSignedUrl(eq(VALID_UUID), eq(ImageSize.ORIGINAL), any(Duration.class)))
                .thenReturn(SIGNED_URL);

            mockMvc.perform(get("/api/images/{imageId}/download", VALID_UUID)
                    .param("size", "ORIGINAL")
                    .param("expirationMinutes", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value(SIGNED_URL))
                .andExpect(jsonPath("$.expiresIn").value("10 minutes"));
        }

        @Test
        void getThumbnailUrl_ShouldReturn200_WhenValidRequest() throws Exception {
            when(imageService.getThumbnailUrl(VALID_UUID, ImageSize.THUMBNAIL_300)).thenReturn(THUMBNAIL_URL);

            mockMvc.perform(get("/api/images/{imageId}/thumbnails/{size}", VALID_UUID, "THUMBNAIL_300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thumbnailUrl").value(THUMBNAIL_URL));
        }
    }

    @Nested
    class SearchAndDiscovery {
        @Test
        void searchImages_ShouldReturn200_WhenValidRequest() throws Exception {
            List<ImageResponse> images = createImageList();
            SearchResponse searchResponse = new SearchResponse(images, 1L, 1, 0, 20);
            when(imageService.searchImages(any(SearchRequest.class))).thenReturn(searchResponse);

            mockMvc.perform(get("/api/images/search")
                    .param("query", "test")
                    .param("page", "0")
                    .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images[0].id").value("id1"))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void listImages_ShouldReturn200_WhenValidRequest() throws Exception {
            List<ImageResponse> images = createImageList();
            when(imageService.listImages(0, 20)).thenReturn(images);

            mockMvc.perform(get("/api/images")
                    .param("page", "0")
                    .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("id1"));
        }
    }

    @Nested
    class Analytics {
        @Test
        void getImageStats_ShouldReturn200() throws Exception {
            ImageStats stats = new ImageStats(
                10L,
                1048576L,
                java.util.Map.of("image/jpeg", 5L),
                java.util.Map.of("nature", 3L),
                104857L
            );
            when(imageService.getImageStats()).thenReturn(stats);

            mockMvc.perform(get("/api/images/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalImages").value(10))
                .andExpect(jsonPath("$.totalSizeBytes").value(1048576));
        }

        @Test
        void getImageAnalytics_ShouldReturn200_WhenValidRequest() throws Exception {
            ImageAnalytics analytics = new ImageAnalytics(VALID_UUID, 100L, 50L, NOW, List.of("US", "DE"));
            when(imageService.getImageAnalytics(VALID_UUID)).thenReturn(analytics);

            mockMvc.perform(get("/api/images/{imageId}/analytics", VALID_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageId").value(VALID_UUID))
                .andExpect(jsonPath("$.downloadCount").value(100))
                .andExpect(jsonPath("$.viewCount").value(50));
        }
    }

    @Nested
    class TagManagement {
        @Test
        void addTags_ShouldReturn200_WhenValidRequest() throws Exception {
            // Arrange
            mockMvc.perform(post("/api/images/{imageId}/tags", VALID_UUID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of("new-tag1", "new-tag2"))))
                .andExpect(status().isOk());
        }

        @Test
        void removeTags_ShouldReturn200_WhenValidRequest() throws Exception {
            mockMvc.perform(delete("/api/images/{imageId}/tags", VALID_UUID)
                    .param("tags", "old-tag1", "old-tag2"))
                .andExpect(status().isOk());
        }
    }

    @Test
    void uploadImage_ShouldReturn400_WhenInvalidUUID() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/images/{imageId}", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getDownloadUrl_ShouldReturn400_WhenInvalidExpirationMinutes() throws Exception {
        // Arrange
        String imageId = "12345678-1234-1234-1234-123456789012";

        // Act & Assert
        mockMvc.perform(get("/api/images/{imageId}/download", imageId)
                .param("expirationMinutes", "100")) // > 15 max
            .andExpect(status().isBadRequest());
    }

    @Test
    void getImageVersions_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        String imageId = "12345678-1234-1234-1234-123456789012";
        List<ImageVersion> versions = List.of(
            new ImageVersion("v1", VALID_UUID, LocalDateTime.now(), 1024L, false, "etag1"),
            new ImageVersion("v2", VALID_UUID, LocalDateTime.now(), 2048L, true, "etag2")
        );

        when(imageService.getImageVersions(imageId)).thenReturn(versions);

        // Act & Assert
        mockMvc.perform(get("/api/images/{imageId}/versions", imageId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].versionId").value("v1"));
    }

    @Test
    void restoreVersion_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        String imageId = "12345678-1234-1234-1234-123456789012";
        String versionId = "87654321-4321-4321-4321-210987654321";

        ImageResponse response = new ImageResponse(
            imageId, "Restored Image", "Description", List.of("tag1"),
            "image/jpeg", 1024L, new ImageDimensions(800, 600),
            LocalDateTime.now(), LocalDateTime.now(), "user",
            new ImageUrls(null, "thumb150", "thumb300", "thumb600")
        );

        when(imageService.restoreVersion(imageId, versionId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/images/{imageId}/versions/{versionId}/restore", imageId, versionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(imageId))
            .andExpect(jsonPath("$.title").value("Restored Image"));
    }

    @Test
    void removeTags_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        String imageId = "12345678-1234-1234-1234-123456789012";

        // Act & Assert
        mockMvc.perform(delete("/api/images/{imageId}/tags", imageId)
                .param("tags", "old-tag1", "old-tag2"))
            .andExpect(status().isOk());
    }

    @Test
    void getImageAnalytics_ShouldReturn200_WhenValidRequest() throws Exception {
        // Arrange
        String imageId = "12345678-1234-1234-1234-123456789012";
        ImageAnalytics analytics = new ImageAnalytics(imageId, 100L, 50L, LocalDateTime.now(), List.of("US", "DE"));

        when(imageService.getImageAnalytics(imageId)).thenReturn(analytics);

        // Act & Assert
        mockMvc.perform(get("/api/images/{imageId}/analytics", imageId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imageId").value(imageId))
            .andExpect(jsonPath("$.downloadCount").value(100))
            .andExpect(jsonPath("$.viewCount").value(50));
    }

    @Test
    void searchImages_ShouldReturn400_WhenInvalidPageSize() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/images/search")
                .param("size", "200")) // > 100 max
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchImages_ShouldReturn400_WhenInvalidSortBy() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/images/search")
                .param("sortBy", "invalid-field"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchImages_ShouldReturn400_WhenInvalidSortDirection() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/images/search")
                .param("sortDirection", "invalid-direction"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getDownloadUrl_ShouldReturn400_WhenExpirationTooLow() throws Exception {
        // Arrange
        String imageId = "12345678-1234-1234-1234-123456789012";

        // Act & Assert
        mockMvc.perform(get("/api/images/{imageId}/download", imageId)
                .param("expirationMinutes", "0")) // < 1 min
            .andExpect(status().isBadRequest());
    }

    // Helper methods
    private MockMultipartFile createValidJpegFile() {
        return new MockMultipartFile("file", "test.jpg", "image/jpeg", JPEG_HEADER);
    }

    private ImageResponse createImageResponse(String id, String title, String description) {
        return new ImageResponse(
            id, title, description, List.of("tag1"),
            "image/jpeg", 1024L, new ImageDimensions(800, 600),
            NOW, NOW, "user",
            new ImageUrls(null, "thumb150", "thumb300", "thumb600")
        );
    }

    private List<ImageResponse> createImageList() {
        return List.of(createImageResponse("id1", "Image 1", "Desc 1"));
    }

    @Nested
    class ValidationTests {
        @Test
        void uploadImage_ShouldReturn400_WhenInvalidUUID() throws Exception {
            mockMvc.perform(get("/api/images/{imageId}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void getDownloadUrl_ShouldReturn400_WhenInvalidExpirationMinutes() throws Exception {
            mockMvc.perform(get("/api/images/{imageId}/download", VALID_UUID)
                    .param("expirationMinutes", "100"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void searchImages_ShouldReturn400_WhenInvalidPageSize() throws Exception {
            mockMvc.perform(get("/api/images/search").param("size", "200"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class VersionManagement {
        @Test
        void getImageVersions_ShouldReturn200_WhenValidRequest() throws Exception {
            List<ImageVersion> versions = List.of(
                new ImageVersion("v1", VALID_UUID, NOW, 1024L, false, "etag1"),
                new ImageVersion("v2", VALID_UUID, NOW, 2048L, true, "etag2")
            );
            when(imageService.getImageVersions(VALID_UUID)).thenReturn(versions);

            mockMvc.perform(get("/api/images/{imageId}/versions", VALID_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].versionId").value("v1"));
        }

        @Test
        void restoreVersion_ShouldReturn200_WhenValidRequest() throws Exception {
            ImageResponse response = createImageResponse(VALID_UUID, "Restored Image", "Description");
            when(imageService.restoreVersion(VALID_UUID, VERSION_UUID)).thenReturn(response);

            mockMvc.perform(post("/api/images/{imageId}/versions/{versionId}/restore", VALID_UUID, VERSION_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALID_UUID))
                .andExpect(jsonPath("$.title").value("Restored Image"));
        }
    }
}
