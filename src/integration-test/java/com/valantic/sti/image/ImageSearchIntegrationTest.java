package com.valantic.sti.image;

import com.valantic.sti.image.model.ImageResponse;
import com.valantic.sti.image.model.SearchRequest;
import com.valantic.sti.image.service.ImageService;
import com.valantic.sti.image.testutil.AbstractIntegrationTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class ImageSearchIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private ImageService imageService;

    @Test
    void searchImages_ShouldReturnEmptyList_WhenNoImages() {
        var searchRequest = new SearchRequest(
            "nonexistent", null, null, 0, 20, "createdAt", "desc"
        );

        var result = imageService.searchImages(searchRequest);

        assertThat(result.images()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void listImages_ShouldReturnEmptyList_WhenNoImages() {
        // Clear any existing data from other tests
        cleanupAllImages();

        List<ImageResponse> result = imageService.listImages(0, 20);
        assertThat(result).isEmpty();
    }

    @Test
    void uploadAndSearch_ShouldFindUploadedImage() {
        // Upload image
        MockMultipartFile file = createValidJpegFile();
        ImageResponse uploaded = imageService.uploadImage(file, "Searchable Image", "Test", List.of("search-test"));

        // Search for it
        var searchRequest = new SearchRequest(
            "Searchable", null, null, 0, 20, "createdAt", "desc"
        );
        var result = imageService.searchImages(searchRequest);

        assertThat(result.images()).hasSize(1);
        assertThat(result.images().getFirst().id()).isEqualTo(uploaded.id());
    }

    @Test
    void searchByTags_ShouldFindImageWithMatchingTags() {
        // Upload images with different tags
        MockMultipartFile file1 = createValidJpegFile();
        MockMultipartFile file2 = createValidJpegFile();

        ImageResponse uploaded1 = imageService.uploadImage(file1, "Image 1", "Desc", List.of("nature", "landscape"));
        ImageResponse uploaded2 = imageService.uploadImage(file2, "Image 2", "Desc", List.of("portrait", "people"));

        // Verify images were uploaded
        assertThat(uploaded1).isNotNull();
        assertThat(uploaded2).isNotNull();

        // Search by tag
        var searchRequest = new SearchRequest(
            null, List.of("nature"), null, 0, 20, "createdAt", "desc"
        );
        var result = imageService.searchImages(searchRequest);

        assertThat(result.images()).hasSize(1);
        assertThat(result.images().getFirst().title()).isEqualTo("Image 1");
    }

    private void cleanupAllImages() {
        try {
            List<ImageResponse> existingImages = imageService.listImages(0, 100);
            for (ImageResponse image : existingImages) {
                imageService.deleteImage(image.id());
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}
